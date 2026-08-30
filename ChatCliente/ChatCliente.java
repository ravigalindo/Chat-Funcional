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

                BufferedReader entrada =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                PrintWriter saida =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        );

                BufferedReader teclado =
                        new BufferedReader(
                                new InputStreamReader(
                                        System.in
                                )
                        )
        ) {

            System.out.println(
                    "Conectado ao servidor!"
            );

            System.out.print(
                    "Digite seu nome: "
            );

            String nome = teclado.readLine();

            saida.println(
                    "LOGIN|" + nome
            );

            String respostaLogin =
                    entrada.readLine();

            System.out.println(
                    "Servidor: "
                            + respostaLogin
            );

            System.out.println();
            System.out.println(
                    "Digite mensagens no formato:"
            );

            System.out.println(
                    "destinatario|mensagem"
            );

            System.out.println(
                    "Digite /sair para desconectar."
            );

            while (true) {

                System.out.print(
                        nome + ": "
                );

                String mensagem =
                        teclado.readLine();

                if (
                        mensagem == null
                                || mensagem.equalsIgnoreCase(
                                "/sair"
                        )
                ) {
                    break;
                }

                if (mensagem.isBlank()) {
                    continue;
                }

                String[] partes =
                        mensagem.split("\\|", 2);

                if (partes.length < 2) {

                    System.out.println(
                            "Formato inválido."
                    );

                    continue;
                }

                String destinatario =
                        partes[0];

                String conteudo =
                        partes[1];

                saida.println(
                        "MESSAGE|"
                                + destinatario
                                + "|"
                                + conteudo
                );

                String resposta =
                        entrada.readLine();

                System.out.println(
                        "Servidor: " + resposta
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro na conexão: "
                            + e.getMessage()
            );
        }

        System.out.println(
                "Cliente encerrado."
        );
    }
}