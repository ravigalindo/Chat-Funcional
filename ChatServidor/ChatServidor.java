package ChatServidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServidor {

    private static final int PORTA = 5000;

    public static void main(String[] args) {

        System.out.println("Iniciando servidor...");

        try (ServerSocket servidor = new ServerSocket(PORTA)) {

            System.out.println("Servidor iniciado na porta " + PORTA);
            System.out.println("Aguardando conexão...");

            Socket cliente = servidor.accept();

            System.out.println("Cliente conectado!");
            System.out.println("IP do cliente: "
                    + cliente.getInetAddress().getHostAddress());

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream())
            );

            PrintWriter saida = new PrintWriter(
                    cliente.getOutputStream(),
                    true
            );

            String mensagem = entrada.readLine();

            System.out.println("Mensagem recebida: " + mensagem);

            saida.println("Olá cliente! O servidor recebeu sua mensagem.");

            cliente.close();

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }

        System.out.println("Servidor encerrado.");
    }
}