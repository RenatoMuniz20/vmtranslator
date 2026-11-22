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
}