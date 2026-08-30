package ChatCliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatCliente {

    private static final String HOST = "localhost";
    private static final int PORTA = 5000;

    public static void main(String[] args) {

        System.out.println("Iniciando cliente...");

        try (Socket socket = new Socket(HOST, PORTA)) {

            System.out.println("Conectado ao servidor!");

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter saida = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            saida.println("Olá servidor!");

            System.out.println("Mensagem enviada!");

            String resposta = entrada.readLine();

            System.out.println("Resposta do servidor: " + resposta);

        } catch (IOException e) {
            System.out.println("Erro ao conectar ao servidor: "
                    + e.getMessage());
        }

        System.out.println("Cliente encerrado.");
    }
}