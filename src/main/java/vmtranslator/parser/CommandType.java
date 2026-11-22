package vmtranslator.parser;

/**
 * Tipos de comando da linguagem VM.
 * Parte 1: aritméticos/lógicos e acesso à memória.
 * Parte 2: controle de fluxo e funções.
 */
public enum CommandType {
    C_ARITHMETIC,
    C_PUSH,
    C_POP,
    C_LABEL,
    C_GOTO,
    C_IF,
    C_FUNCTION,
    C_CALL,
    C_RETURN
}
