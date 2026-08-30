package ChatServidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteHandler implements Runnable {

    private final Socket cliente;

    public ClienteHandler(Socket cliente) {
        this.cliente = cliente;
    }

    @Override
    public void run() {

        try (
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream())
                );

                PrintWriter saida = new PrintWriter(
                        cliente.getOutputStream(),
                        true
                )
        ) {

            System.out.println(
                    "Atendendo cliente: "
                            + cliente.getInetAddress().getHostAddress()
            );

            String mensagem = entrada.readLine();

            System.out.println(
                    "Mensagem recebida: " + mensagem
            );

            saida.println(
                    "Servidor recebeu: " + mensagem
            );

        } catch (IOException e) {

            System.out.println(
                    "Erro ao atender cliente: "
                            + e.getMessage()
            );

        } finally {

            try {
                cliente.close();
            } catch (IOException e) {
                System.out.println(
                        "Erro ao fechar conexão: "
                                + e.getMessage()
                );
            }

            System.out.println("Cliente desconectado.");
        }
    }
}