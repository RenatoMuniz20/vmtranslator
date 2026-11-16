package vmtranslator.main;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -jar vmtranslator.jar <arquivo.vm | diretório>");
            System.exit(1);
        }
        System.out.println("Entrada recebida: " + args[0]);
    }
}
