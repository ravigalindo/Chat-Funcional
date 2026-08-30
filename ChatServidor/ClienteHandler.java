package ChatServidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteHandler implements Runnable {

    private final Socket cliente;
    private final GerenciadorClientes gerenciador;

    private String nomeUsuario;

    private PrintWriter saida;

    public ClienteHandler(
            Socket cliente,
            GerenciadorClientes gerenciador
    ) {
        this.cliente = cliente;
        this.gerenciador = gerenciador;
    }

    @Override
    public void run() {

        gerenciador.adicionarCliente(this);

        try (
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(
                                cliente.getInputStream()
                        )
                )
        ) {

            saida = new PrintWriter(
                    cliente.getOutputStream(),
                    true
            );

            System.out.println(
                    "Cliente conectado: "
                            + cliente.getInetAddress()
                                    .getHostAddress()
            );

            String mensagem;

            while ((mensagem = entrada.readLine()) != null) {

                processarMensagem(mensagem);
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro ao atender cliente: "
                            + e.getMessage()
            );

        } finally {

            gerenciador.removerCliente(this);

            try {
                cliente.close();
            } catch (IOException e) {
                System.out.println(
                        "Erro ao fechar conexão."
                );
            }

            System.out.println(
                    "Cliente desconectado: "
                            + nomeUsuario
            );
        }
    }

    private void processarMensagem(String mensagem) {

        String[] partes = mensagem.split("\\|", 3);

        String comando = partes[0];

        switch (comando) {

            case "LOGIN":

                if (partes.length < 2) {
                    saida.println("ERRO|Nome de usuário inválido");
                    return;
                }

                nomeUsuario = partes[1];

                System.out.println(
                        "Usuário identificado como: "
                                + nomeUsuario
                );

                saida.println(
                        "LOGIN_OK|" + nomeUsuario
                );

                break;

            case "MESSAGE":

                if (partes.length < 3) {
                    saida.println(
                            "ERRO|Mensagem inválida"
                    );
                    return;
                }

                String destinatario = partes[1];
                String conteudo = partes[2];

                ClienteHandler clienteDestino =
                        gerenciador.encontrarCliente(
                                destinatario
                        );

                if (clienteDestino == null) {

                    saida.println(
                            "ERRO|Usuário não encontrado"
                    );

                    return;
                }

                clienteDestino.enviarMensagem(
                        "MESSAGE|"
                                + nomeUsuario
                                + "|"
                                + conteudo
                );

                saida.println(
                        "MESSAGE_SENT"
                );

                break;

            default:

                saida.println(
                        "ERRO|Comando desconhecido"
                );
        }
    }

    public void enviarMensagem(String mensagem) {

        if (saida != null) {
            saida.println(mensagem);
        }
    }

    public String getNomeUsuario() {

        return nomeUsuario;
    }
}