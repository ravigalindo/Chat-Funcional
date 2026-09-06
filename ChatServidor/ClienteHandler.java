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

    private void processarMensagem(
            String mensagem
    ) {

        String[] partes =
                mensagem.split("\\|", 3);

        String comando =
                partes[0];

        switch (comando) {

            case "LOGIN":

                processarLogin(partes);

                break;

            case "MESSAGE":

                processarMensagemChat(partes);

                break;

            case "TYPING":

                processarDigitacao(partes);

                break;

            case "STOP_TYPING":

                processarParadaDigitacao(partes);

                break;

            default:

                saida.println(
                        "ERRO|Comando desconhecido"
                );
        }
    }

    private void processarLogin(
            String[] partes
    ) {

        if (partes.length < 2) {

            saida.println(
                    "ERRO|Nome de usuário inválido"
            );

            return;
        }

        nomeUsuario =
                partes[1];

        System.out.println(
                "Usuário identificado como: "
                        + nomeUsuario
        );

        gerenciador.adicionarCliente(
                this
        );

        /*
         * Primeiro confirma o login.
         */
        saida.println(
                "LOGIN_OK|"
                        + nomeUsuario
        );

        /*
         * Depois envia a lista atualizada
         * de usuários para TODOS os clientes.
         *
         * Isso faz com que tanto quem acabou
         * de entrar quanto quem já estava
         * conectado recebam a nova lista.
         */
        String usuariosOnline =
                gerenciador.obterUsuariosOnline();

        gerenciador.enviarParaTodos(
                "USERS|"
                        + usuariosOnline
        );

        /*
         * Avisa que este usuário entrou.
         */
        gerenciador.enviarParaTodos(
                "ONLINE|"
                        + nomeUsuario
        );
    }

    private void processarMensagemChat(
            String[] partes
    ) {

        if (partes.length < 3) {

            saida.println(
                    "ERRO|Mensagem inválida"
            );

            return;
        }

        String destinatario =
                partes[1];

        String conteudo =
                partes[2];

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
    }

    private void processarDigitacao(
            String[] partes
    ) {

        if (partes.length < 2) {
            return;
        }

        String destinatario =
                partes[1];

        ClienteHandler clienteDestino =
                gerenciador.encontrarCliente(
                        destinatario
                );

        if (clienteDestino == null) {
            return;
        }

        clienteDestino.enviarMensagem(
                "TYPING|"
                        + nomeUsuario
        );
    }

    private void processarParadaDigitacao(
            String[] partes
    ) {

        if (partes.length < 2) {
            return;
        }

        String destinatario =
                partes[1];

        ClienteHandler clienteDestino =
                gerenciador.encontrarCliente(
                        destinatario
                );

        if (clienteDestino == null) {
            return;
        }

        clienteDestino.enviarMensagem(
                "STOP_TYPING|"
                        + nomeUsuario
        );
    }

    public void enviarMensagem(
            String mensagem
    ) {

        if (saida != null) {

            saida.println(
                    mensagem
            );
        }
    }

    public String getNomeUsuario() {

        return nomeUsuario;
    }
}