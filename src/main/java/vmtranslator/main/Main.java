package vmtranslator.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import vmtranslator.codewriter.CodeWriter;
import vmtranslator.parser.Parser;
import vmtranslator.parser.VMSyntaxException;

/**
 * Ponto de entrada:
 *   java -jar vmtranslator.jar caminho/Xxx.vm   → caminho/Xxx.asm
 *   java -jar vmtranslator.jar caminho/Xxx      → caminho/Xxx/Xxx.asm (todos os .vm do diretório)
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

    /** Traduz um arquivo .vm ou um diretório e devolve o caminho do .asm gerado. */
    static Path translate(Path input) throws IOException {
        List<Path> vmFiles;
        Path output;
        if (Files.isDirectory(input)) {
            Path dir = input.toAbsolutePath().normalize();
            vmFiles = listVmFiles(dir);
            if (vmFiles.isEmpty()) {
                throw new IllegalArgumentException("o diretório não contém arquivos .vm: " + input);
            }
            output = dir.resolve(dir.getFileName() + ".asm");        // Xxx/Xxx.asm
        } else if (Files.isRegularFile(input) && isVmFile(input)) {
            vmFiles = List.of(input);
            output = input.resolveSibling(baseName(input) + ".asm"); // Xxx.vm → Xxx.asm
        } else {
            throw new IllegalArgumentException(
                    "a entrada precisa ser um arquivo .vm ou um diretório existente: " + input);
        }

        try (CodeWriter writer = new CodeWriter(output)) {
            for (Path vmFile : vmFiles) {
                writer.setFileName(baseName(vmFile));                // prefixo dos símbolos static
                try {
                    translateFile(new Parser(vmFile), writer);
                } catch (VMSyntaxException e) {
                    throw new VMSyntaxException(vmFile.getFileName().toString(), e);
                }
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(output);   // não deixa um .asm pela metade
            throw e;
        }
        return output;
    }

    /** Arquivos .vm do diretório, em ordem alfabética (resultado previsível). */
    private static List<Path> listVmFiles(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile).filter(Main::isVmFile).sorted().toList();
        }
    }

    private static boolean isVmFile(Path file) {
        return file.getFileName().toString().endsWith(".vm");
    }

    /** Nome do arquivo sem a extensão .vm. */
    private static String baseName(Path vmFile) {
        String name = vmFile.getFileName().toString();
        return name.substring(0, name.length() - ".vm".length());
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
                    case C_LABEL -> writer.writeLabel(parser.arg1());
                    case C_GOTO -> writer.writeGoto(parser.arg1());
                    case C_IF -> writer.writeIf(parser.arg1());
                    case C_FUNCTION -> writer.writeFunction(parser.arg1(), parser.arg2());
                    case C_CALL -> writer.writeCall(parser.arg1(), parser.arg2());
                    case C_RETURN -> writer.writeReturn();
                    default -> throw new IllegalArgumentException(
                            "tipo de comando não suportado: " + parser.commandType());
                }
            } catch (IllegalArgumentException e) {
                // acrescenta a linha do .vm à mensagem de erro do CodeWriter
                throw new VMSyntaxException(parser.currentLine(), parser.currentCommand(), e.getMessage());
            }
        }
    }
}
