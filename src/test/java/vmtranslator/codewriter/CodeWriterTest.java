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
}