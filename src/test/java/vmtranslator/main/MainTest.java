package vmtranslator.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vmtranslator.parser.VMSyntaxException;

class MainTest {

    @TempDir
    Path dir;

    @Test
    void geraOAsmNaMesmaPastaComOMesmoNome() throws IOException {
        Path vm = dir.resolve("Prog.vm");
        Files.write(vm, List.of("push constant 1", "push constant 2", "add"));

        Path asm = Main.translate(vm);

        assertEquals(dir.resolve("Prog.asm"), asm);
        assertTrue(Files.exists(asm));
    }

    @Test
    void erroInformaALinhaENaoDeixaAsmPelaMetade() throws IOException {
        Path vm = dir.resolve("Prog.vm");
        Files.write(vm, List.of("push constant 1", "pop constant 0"));

        VMSyntaxException e = assertThrows(VMSyntaxException.class, () -> Main.translate(vm));

        assertTrue(e.getMessage().contains("Linha 2"));
        assertFalse(Files.exists(dir.resolve("Prog.asm")));
    }

    @Test
    void entradaInexistenteEhErro() {
        assertThrows(IllegalArgumentException.class, () -> Main.translate(dir.resolve("Nada.vm")));
    }

    @Test
    void diretorioGeraUmAsmComONomeDoDiretorio() throws IOException {
        Path prog = Files.createDirectory(dir.resolve("Prog"));
        Files.write(prog.resolve("A.vm"), List.of("push constant 1"));
        Files.write(prog.resolve("B.vm"), List.of("push static 0"));
        Files.write(prog.resolve("notas.txt"), List.of("não é .vm"));

        Path asm = Main.translate(prog);

        assertEquals(prog.resolve("Prog.asm"), asm);
        List<String> out = Files.readAllLines(asm);
        assertTrue(out.contains("@B.0"));                 // static usa o nome de cada arquivo
        assertFalse(out.contains("@Sys.init"));           // sem Sys.vm, sem bootstrap
    }

    @Test
    void bootstrapSoQuandoODiretorioTemSysVm() throws IOException {
        Path prog = Files.createDirectory(dir.resolve("Prog"));
        Files.write(prog.resolve("Sys.vm"), List.of("function Sys.init 0", "label FIM", "goto FIM"));

        List<String> out = Files.readAllLines(Main.translate(prog));

        assertEquals("// bootstrap: SP = 256; call Sys.init 0", out.get(0));
        assertTrue(out.contains("@Sys.init"));
    }

    @Test
    void arquivoSysVmSozinhoNaoGeraBootstrap() throws IOException {
        Path vm = dir.resolve("Sys.vm");
        Files.write(vm, List.of("function Sys.init 0", "return"));

        List<String> out = Files.readAllLines(Main.translate(vm));

        assertTrue(out.get(0).startsWith("// function"));
    }

    @Test
    void diretorioSemVmEhErro() throws IOException {
        Path vazio = Files.createDirectory(dir.resolve("Vazio"));
        assertThrows(IllegalArgumentException.class, () -> Main.translate(vazio));
    }

    @Test
    void erroNoDiretorioInformaOArquivo() throws IOException {
        Path prog = Files.createDirectory(dir.resolve("Prog"));
        Files.write(prog.resolve("A.vm"), List.of("push constant 1"));
        Files.write(prog.resolve("B.vm"), List.of("call 3x 0"));

        VMSyntaxException e = assertThrows(VMSyntaxException.class, () -> Main.translate(prog));

        assertTrue(e.getMessage().startsWith("B.vm, Linha 1"));
        assertFalse(Files.exists(prog.resolve("Prog.asm")));
    }
}
