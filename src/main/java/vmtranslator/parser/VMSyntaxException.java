package vmtranslator.parser;

/**
 * Erro de sintaxe num arquivo .vm. A mensagem informa a linha e o texto
 * do comando problemático, para facilitar a correção.
 */
public class VMSyntaxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VMSyntaxException(int lineNumber, String line, String message) {
        super("Linha " + lineNumber + ": " + message + " -> \"" + line + "\"");
    }
}