package vmtranslator.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import vmtranslator.codewriter.CodeWriter;
import vmtranslator.parser.Parser;
import vmtranslator.parser.VMSyntaxException;

/**
 * Ponto de entrada: java -jar vmtranslator.jar caminho/Xxx.vm
 */
public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -jar vmtranslator.jar <arquivo.vm | diretório>");
            System.exit(1);
        }
        try {
            Path output = translate(Path.of(args[0]));
            System.out.println("Gerado: " + output);
        } catch (VMSyntaxException | IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erro de leitura/escrita: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Traduz um arquivo .vm e devolve o caminho do .asm gerado. */
    static Path translate(Path input) throws IOException {
        String fileName = input.getFileName().toString();
        if (!fileName.endsWith(".vm") || !Files.isRegularFile(input)) {
            throw new IllegalArgumentException("a entrada precisa ser um arquivo .vm existente: " + input);
        }

        String baseName = fileName.substring(0, fileName.length() - ".vm".length());
        Path output = input.resolveSibling(baseName + ".asm");

        try (CodeWriter writer = new CodeWriter(output)) {
            writer.setFileName(baseName);
            translateFile(new Parser(input), writer);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(output);   // não deixa um .asm pela metade
            throw e;
        }
        return output;
    }

    /** Lê cada comando do Parser e despacha para o método certo do CodeWriter. */
    private static void translateFile(Parser parser, CodeWriter writer) throws IOException {
        while (parser.hasMoreCommands()) {
            parser.advance();
            try {
                switch (parser.commandType()) {
                    case C_ARITHMETIC -> writer.writeArithmetic(parser.arg1());
                    case C_PUSH -> writer.writePush(parser.arg1(), parser.arg2());
                    case C_POP -> writer.writePop(parser.arg1(), parser.arg2());
                }
            } catch (IllegalArgumentException e) {
                // acrescenta a linha do .vm à mensagem de erro do CodeWriter
                throw new VMSyntaxException(parser.currentLine(), parser.currentCommand(), e.getMessage());
            }
        }
    }
}
