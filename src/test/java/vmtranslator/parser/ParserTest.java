package vmtranslator.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParserTest {

    @Test
    void ignoraComentariosELinhasEmBranco() {
        Parser p = new Parser(List.of(
                "// comentário de linha inteira",
                "",
                "   ",
                "push constant 7 // comentário no fim",
                "add"));

        assertTrue(p.hasMoreCommands());
        p.advance();
        assertEquals("push constant 7", p.currentCommand());
        assertEquals(4, p.currentLine());

        p.advance();
        assertEquals("add", p.currentCommand());
        assertFalse(p.hasMoreCommands());
    }

    @Test
    void reconheceTodosOsComandosAritmeticos() {
        for (String cmd : List.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not")) {
            Parser p = new Parser(List.of(cmd));
            p.advance();
            assertEquals(CommandType.C_ARITHMETIC, p.commandType());
            assertEquals(cmd, p.arg1());
        }
    }

    @Test
    void decompoePushEPop() {
        Parser p = new Parser(List.of("push local 2", "pop   that\t5"));

        p.advance();
        assertEquals(CommandType.C_PUSH, p.commandType());
        assertEquals("local", p.arg1());
        assertEquals(2, p.arg2());

        p.advance();
        assertEquals(CommandType.C_POP, p.commandType());
        assertEquals("that", p.arg1());
        assertEquals(5, p.arg2());
    }

    @Test
    void comandoDesconhecidoInformaALinha() {
        VMSyntaxException e = assertThrows(VMSyntaxException.class,
                () -> new Parser(List.of("push constant 1", "mul")));
        assertTrue(e.getMessage().contains("Linha 2"));
    }

    @Test
    void pushSemIndiceEhErro() {
        assertThrows(VMSyntaxException.class, () -> new Parser(List.of("push constant")));
    }

    @Test
    void indiceNaoNumericoEhErro() {
        assertThrows(VMSyntaxException.class, () -> new Parser(List.of("pop local x")));
    }

    @Test
    void aritmeticoComArgumentoEhErro() {
        assertThrows(VMSyntaxException.class, () -> new Parser(List.of("add 1")));
    }

    @Test
    void leArquivoRealSimpleAdd() throws IOException {
        Parser p = new Parser(Path.of("projects/7/StackArithmetic/SimpleAdd/SimpleAdd.vm"));
        int count = 0;
        while (p.hasMoreCommands()) {
            p.advance();
            count++;
        }
        assertEquals(3, count); // push constant 7, push constant 8, add
    }
}