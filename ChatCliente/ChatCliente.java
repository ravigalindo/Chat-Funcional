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

        try (
                Socket socket = new Socket(HOST, PORTA);

                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                PrintWriter saida = new PrintWriter(
                        socket.getOutputStream(),
                        true
                );

                BufferedReader teclado = new BufferedReader(
                        new InputStreamReader(System.in)
                )
        ) {

            System.out.println("Conectado ao servidor!");
            System.out.println("Digite uma mensagem.");
            System.out.println("Digite /sair para desconectar.");
            System.out.println();

            String mensagem;

            while (true) {

                System.out.print("Você: ");

                mensagem = teclado.readLine();

                if (mensagem == null || mensagem.equalsIgnoreCase("/sair")) {
                    break;
                }

                if (mensagem.isBlank()) {
                    continue;
                }

                saida.println(mensagem);

                String resposta = entrada.readLine();

                System.out.println(
                        "Servidor: " + resposta
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro na conexão: " + e.getMessage()
            );
        }

        System.out.println("Cliente encerrado.");
    }
}