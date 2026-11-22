package vmtranslator.codewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeWriterTest {

    @TempDir
    Path dir;

    /** Lê o .asm gerado como lista de linhas. */
    private List<String> lines(Path asm) throws IOException {
        return Files.readAllLines(asm);
    }

    @Test
    void escreveOComandoVmComoComentario() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writePush("constant", 7);
            cw.writeArithmetic("add");
        }
        List<String> out = lines(asm);
        assertTrue(out.contains("// push constant 7"));
        assertTrue(out.contains("// add"));
    }

    @Test
    void comparacoesGeramRotulosUnicos() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writeArithmetic("eq");
            cw.writeArithmetic("eq");
            cw.writeArithmetic("lt");
        }
        List<String> labels = lines(asm).stream().filter(l -> l.startsWith("(TRUE_")).toList();
        assertEquals(List.of("(TRUE_EQ_0)", "(TRUE_EQ_1)", "(TRUE_LT_2)"), labels);
    }

    @Test
    void staticUsaONomeDoArquivo() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.setFileName("Foo");
            cw.writePush("static", 3);
            cw.writePop("static", 5);
        }
        List<String> out = lines(asm);
        assertTrue(out.contains("@Foo.3"));
        assertTrue(out.contains("@Foo.5"));
    }

    @Test
    void pointerETempUsamEnderecosFixos() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writePush("pointer", 0);
            cw.writePush("pointer", 1);
            cw.writePush("temp", 6);
        }
        List<String> out = lines(asm);
        assertTrue(out.contains("@THIS"));
        assertTrue(out.contains("@THAT"));
        assertTrue(out.contains("@11"));   // temp 6 = RAM[5 + 6]
    }

    @Test
    void indicesForaDoIntervaloSaoErro() throws IOException {
        try (CodeWriter cw = new CodeWriter(dir.resolve("Teste.asm"))) {
            assertThrows(IllegalArgumentException.class, () -> cw.writePush("temp", 8));
            assertThrows(IllegalArgumentException.class, () -> cw.writePop("pointer", 2));
            assertThrows(IllegalArgumentException.class, () -> cw.writePush("constant", 32768));
        }
    }

    @Test
    void segmentosEComandosInvalidosSaoErro() throws IOException {
        try (CodeWriter cw = new CodeWriter(dir.resolve("Teste.asm"))) {
            assertThrows(IllegalArgumentException.class, () -> cw.writePop("constant", 0));
            assertThrows(IllegalArgumentException.class, () -> cw.writePush("locals", 0));
            assertThrows(IllegalArgumentException.class, () -> cw.writeArithmetic("mul"));
        }
    }

    @Test
    void rotulosUsamOEscopoDaFuncao() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.setFileName("Foo");
            cw.writeLabel("INICIO");               // fora de função: prefixo = arquivo
            cw.writeFunction("Foo.bar", 0);
            cw.writeLabel("LOOP");
            cw.writeGoto("LOOP");
            cw.writeIf("LOOP");
        }
        List<String> out = lines(asm);
        assertTrue(out.contains("(Foo$INICIO)"));
        assertTrue(out.contains("(Foo.bar$LOOP)"));
        assertEquals(2, out.stream().filter(l -> l.equals("@Foo.bar$LOOP")).count());
    }

    @Test
    void functionZeraAsVariaveisLocais() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writeFunction("Foo.semLocais", 0);
            cw.writeFunction("Foo.tresLocais", 3);
        }
        List<String> out = lines(asm);
        assertTrue(out.contains("(Foo.semLocais)"));
        assertEquals(3, out.stream().filter(l -> l.equals("M=0")).count());   // uma por local
    }

    @Test
    void cadaCallTemSeuRotuloDeRetorno() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.setFileName("Main");
            cw.writeFunction("Main.main", 0);
            cw.writeCall("Main.fibonacci", 1);
            cw.writeCall("Main.fibonacci", 1);
        }
        List<String> labels = lines(asm).stream().filter(l -> l.contains("$ret.") && l.startsWith("(")).toList();
        assertEquals(List.of("(Main.main$ret.0)", "(Main.main$ret.1)"), labels);
    }

    @Test
    void callReposicionaArgDeAcordoComNArgs() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writeCall("Foo.bar", 2);
        }
        assertTrue(lines(asm).contains("@7"));   // ARG = SP - 5 - 2
    }

    @Test
    void bootstrapIniciaSpEChamaSysInit() throws IOException {
        Path asm = dir.resolve("Teste.asm");
        try (CodeWriter cw = new CodeWriter(asm)) {
            cw.writeInit();
        }
        List<String> out = lines(asm);
        assertEquals("@256", out.get(1));
        assertTrue(out.contains("@Sys.init"));
        assertTrue(out.contains("(Bootstrap$ret.0)"));
    }
}
