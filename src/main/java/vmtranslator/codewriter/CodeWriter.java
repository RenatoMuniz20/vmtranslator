package vmtranslator.codewriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Traduz comandos VM para Assembly Hack e os grava no arquivo .asm.
 */
public class CodeWriter implements AutoCloseable {

    /** Maior valor que cabe numa instrução A (15 bits). */
    private static final int MAX_CONSTANT = 32767;

    /** O segmento temp ocupa RAM[5] a RAM[12]. */
    private static final int TEMP_BASE = 5;
    private static final int TEMP_SIZE = 8;

    private final BufferedWriter out;
    private String fileName = "";

    /** Contador que torna únicos os rótulos gerados pelas comparações. */
    private int labelCounter = 0;

    /** Abre (ou recria) o arquivo .asm de saída. */
    public CodeWriter(Path output) throws IOException {
        this.out = Files.newBufferedWriter(output);
    }

    /** Informa o nome do .vm sendo traduzido (usado pelos símbolos static). */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /** add, sub, neg, eq, gt, lt, and, or, not. */
    public void writeArithmetic(String command) throws IOException {
        comment(command);
        switch (command) {
            case "add" -> binary("D+M");   // x + y
            case "sub" -> binary("M-D");   // x - y  (M = x, D = y)
            case "and" -> binary("D&M");   // x & y
            case "or"  -> binary("D|M");   // x | y
            case "neg" -> unary("-M");     // -y
            case "not" -> unary("!M");     // !y
            case "eq"  -> compare("JEQ");  // x == y
            case "gt"  -> compare("JGT");  // x >  y
            case "lt"  -> compare("JLT");  // x <  y
            default -> throw new IllegalArgumentException(
                    "comando aritmético desconhecido: " + command);
        }
    }

    /** Empilha o valor de segment[index]. */
    public void writePush(String segment, int index) throws IOException {
        comment("push " + segment + " " + index);
        switch (segment) {
            case "constant" -> {
                if (index > MAX_CONSTANT) {
                    throw new IllegalArgumentException(
                            "constante maior que " + MAX_CONSTANT + ": " + index);
                }
                emit("@" + index, "D=A");          // D = index
            }
            case "local", "argument", "this", "that" -> emit(
                    "@" + index, "D=A",                  // D = index
                    "@" + basePointer(segment), "A=D+M", // A = base + index
                    "D=M");                              // D = RAM[base + index]
            case "temp" -> emit("@" + fixedAddress(segment, index), "D=M");
            default -> throw new IllegalArgumentException(
                    "segmento desconhecido em push: " + segment);
        }
        pushD();
    }

    /** Desempilha o topo da pilha para segment[index]. */
    public void writePop(String segment, int index) throws IOException {
        comment("pop " + segment + " " + index);
        switch (segment) {
            case "local", "argument", "this", "that" -> {
                emit("@" + index, "D=A",
                     "@" + basePointer(segment), "D=D+M", // D = base + index
                     "@R13", "M=D");                      // R13 = endereço de destino
                popD();                                   // D = valor desempilhado
                emit("@R13", "A=M", "M=D");               // RAM[R13] = D
            }
            case "temp" -> {
                popD();
                emit("@" + fixedAddress(segment, index), "M=D");
            }
            case "constant" -> throw new IllegalArgumentException(
                    "não é possível fazer pop em constant");
            default -> throw new IllegalArgumentException(
                    "segmento desconhecido em pop: " + segment);
        }
    }

    /** Escreve um laço infinito no fim do programa e fecha o arquivo. */
    @Override
    public void close() throws IOException {
        comment("fim do programa: laço infinito");
        emit("(END_OF_PROGRAM)", "@END_OF_PROGRAM", "0;JMP");
        out.close();
    }

    // ------------------------------------------------------------ blocos de código

    /** Símbolo do ponteiro-base de local, argument, this e that. */
    private static String basePointer(String segment) {
        return switch (segment) {
            case "local" -> "LCL";
            case "argument" -> "ARG";
            case "this" -> "THIS";
            case "that" -> "THAT";
            default -> throw new IllegalArgumentException("segmento sem ponteiro-base: " + segment);
        };
    }

    /** Endereço fixo (conhecido em tempo de tradução) de temp i. */
    private String fixedAddress(String segment, int index) {
        switch (segment) {
            case "temp" -> {
                checkRange(segment, index, TEMP_SIZE);
                return String.valueOf(TEMP_BASE + index);   // RAM[5 + i]
            }
            default -> throw new IllegalArgumentException("segmento sem endereço fixo: " + segment);
        }
    }

    private static void checkRange(String segment, int index, int size) {
        if (index >= size) {
            throw new IllegalArgumentException(
                    "índice fora do intervalo 0.." + (size - 1) + " em " + segment + ": " + index);
        }
    }

    /** *SP = D; SP++ */
    private void pushD() throws IOException {
        emit("@SP", "A=M", "M=D", "@SP", "M=M+1");
    }

    /** SP--; D = *SP */
    private void popD() throws IOException {
        emit("@SP", "AM=M-1", "D=M");
    }

    /**
     * Operação binária: desempilha y, e troca x (o novo topo) por "x op y".
     * Em Assembly: D = y, M = x, e o resultado é gravado no lugar de x.
     */
    private void binary(String operation) throws IOException {
        emit("@SP", "AM=M-1",   // SP--, A aponta para y
             "D=M",             // D = y
             "A=A-1",           // A aponta para x
             "M=" + operation); // x = x op y
    }

    /** Operação unária: troca o topo da pilha por "op topo", sem mexer no SP. */
    private void unary(String operation) throws IOException {
        emit("@SP", "A=M-1",    // A aponta para o topo (y)
             "M=" + operation); // y = op y
    }

    /**
     * Comparação: desempilha y, e troca x por -1 (verdadeiro) ou 0 (falso).
     * Calcula D = x - y e usa o salto condicional (JEQ, JGT ou JLT) sobre D.
     */
    private void compare(String jump) throws IOException {
        String trueLabel = "TRUE_" + jump.substring(1) + "_" + labelCounter++;  // ex.: TRUE_EQ_0
        emit("@SP", "AM=M-1",   // SP--, A aponta para y
             "D=M",             // D = y
             "A=A-1",           // A aponta para x
             "D=M-D",           // D = x - y
             "M=-1",            // supõe verdadeiro: x = -1
             "@" + trueLabel,
             "D;" + jump,       // se a condição vale, mantém -1 e pula
             "@SP", "A=M-1",    // senão, volta a apontar para o topo
             "M=0",             // e grava 0 (falso)
             "(" + trueLabel + ")");
    }

    // ------------------------------------------------------------ escrita

    private void comment(String text) throws IOException {
        out.write("// " + text + "\n");
    }

    private void emit(String... lines) throws IOException {
        for (String line : lines) {
            out.write(line + "\n");
        }
    }
}
