package vmtranslator.parser;

import static vmtranslator.parser.CommandType.C_ARITHMETIC;
import static vmtranslator.parser.CommandType.C_CALL;
import static vmtranslator.parser.CommandType.C_FUNCTION;
import static vmtranslator.parser.CommandType.C_GOTO;
import static vmtranslator.parser.CommandType.C_IF;
import static vmtranslator.parser.CommandType.C_LABEL;
import static vmtranslator.parser.CommandType.C_POP;
import static vmtranslator.parser.CommandType.C_PUSH;
import static vmtranslator.parser.CommandType.C_RETURN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lê um arquivo .vm, remove comentários e linhas em branco
 * e entrega um comando por vez, já decomposto em partes.
 */
public class Parser {

    private static final Set<String> ARITHMETIC_COMMANDS =
            Set.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not");

    /**
     * Nome válido para rótulos e funções: letras, dígitos, '_', '.', '$' e ':',
     * sem começar por dígito (mesma regra dos símbolos do Assembly Hack).
     */
    private static final String SYMBOL = "[A-Za-z_.$:][A-Za-z0-9_.$:]*";

    /** Um comando já limpo: suas palavras, o texto completo e a linha de origem. */
    private record Command(String[] tokens, String text, int lineNumber) { }

    private final List<Command> commands = new ArrayList<>();
    private int index = -1;

    /** Abre e lê o arquivo .vm. */
    public Parser(Path file) throws IOException {
        this(Files.readAllLines(file));
    }

    /** Recebe as linhas diretamente (útil nos testes unitários). */
    public Parser(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String text = clean(lines.get(i));
            if (!text.isEmpty()) {
                Command command = new Command(text.split("\\s+"), text, i + 1);
                validate(command);
                commands.add(command);
            }
        }
    }

    public boolean hasMoreCommands() {
        return index + 1 < commands.size();
    }

    public void advance() {
        if (!hasMoreCommands()) {
            throw new IllegalStateException("Não há mais comandos para ler");
        }
        index++;
    }

    public CommandType commandType() {
        return typeOf(current());
    }

    /**
     * Primeiro argumento: o próprio comando (aritmético), o segmento (push/pop),
     * o rótulo (label/goto/if-goto) ou o nome da função (function/call).
     * Não se aplica a return.
     */
    public String arg1() {
        Command command = current();
        CommandType type = typeOf(command);
        if (type == C_RETURN) {
            throw new IllegalStateException("arg1() não se aplica a " + type);
        }
        if (type == C_ARITHMETIC) {
            return command.tokens()[0];
        }
        return command.tokens()[1];
    }

    /** Segundo argumento inteiro: índice (push/pop), nVars (function) ou nArgs (call). */
    public int arg2() {
        Command command = current();
        CommandType type = typeOf(command);
        if (!hasNumericArg2(type)) {
            throw new IllegalStateException("arg2() não se aplica a " + type);
        }
        return Integer.parseInt(command.tokens()[2]);
    }

    /** Texto do comando atual, usado como comentário no .asm. */
    public String currentCommand() {
        return current().text();
    }

    /** Linha do comando atual no arquivo .vm, usada nas mensagens de erro. */
    public int currentLine() {
        return current().lineNumber();
    }

    // ---------------------------------------------------------------- auxiliares

    private Command current() {
        if (index < 0) {
            throw new IllegalStateException("Chame advance() antes de ler o comando");
        }
        return commands.get(index);
    }

    /** Remove o comentário (inclusive no fim da linha) e os espaços das pontas. */
    private static String clean(String line) {
        int comment = line.indexOf("//");
        if (comment >= 0) {
            line = line.substring(0, comment);
        }
        return line.trim();
    }

    private static CommandType typeOf(Command command) {
        String keyword = command.tokens()[0];
        if (ARITHMETIC_COMMANDS.contains(keyword)) {
            return C_ARITHMETIC;
        }
        return switch (keyword) {
            case "push" -> C_PUSH;
            case "pop" -> C_POP;
            case "label" -> C_LABEL;
            case "goto" -> C_GOTO;
            case "if-goto" -> C_IF;
            case "function" -> C_FUNCTION;
            case "call" -> C_CALL;
            case "return" -> C_RETURN;
            default -> throw new VMSyntaxException(
                    command.lineNumber(), command.text(), "comando desconhecido");
        };
    }

    /** Comandos cujo arg2 é um inteiro. */
    private static boolean hasNumericArg2(CommandType type) {
        return type == C_PUSH || type == C_POP || type == C_FUNCTION || type == C_CALL;
    }

    /** Confere a quantidade de argumentos, o formato dos números e dos nomes. */
    private static void validate(Command command) {
        CommandType type = typeOf(command);
        int expectedTokens = switch (type) {
            case C_ARITHMETIC, C_RETURN -> 1;
            case C_LABEL, C_GOTO, C_IF -> 2;
            case C_PUSH, C_POP, C_FUNCTION, C_CALL -> 3;
        };
        if (command.tokens().length != expectedTokens) {
            throw new VMSyntaxException(command.lineNumber(), command.text(),
                    "esperado(s) " + (expectedTokens - 1) + " argumento(s)");
        }
        if (hasNumericArg2(type) && !command.tokens()[2].matches("\\d+")) {
            throw new VMSyntaxException(command.lineNumber(), command.text(),
                    "o segundo argumento deve ser um inteiro não negativo");
        }
        boolean hasName = type == C_LABEL || type == C_GOTO || type == C_IF
                || type == C_FUNCTION || type == C_CALL;
        if (hasName && !command.tokens()[1].matches(SYMBOL)) {
            throw new VMSyntaxException(command.lineNumber(), command.text(),
                    "nome inválido: " + command.tokens()[1]);
        }
    }
}
