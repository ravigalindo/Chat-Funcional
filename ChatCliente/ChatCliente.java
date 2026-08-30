package ChatCliente;

import java.io.IOException;
import java.net.Socket;

public class ChatCliente {

    private static final String HOST = "localhost";
    private static final int PORTA = 5000;

    public static void main(String[] args) {

        System.out.println("Iniciando cliente...");

        try (Socket socket = new Socket(HOST, PORTA)) {

            System.out.println("Conectado ao servidor!");

        } catch (IOException e) {
            System.out.println("Erro ao conectar ao servidor: "
                    + e.getMessage());
        }

        System.out.println("Cliente encerrado.");
    }
}