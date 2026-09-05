package ChatCliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClienteTCP {

    private static final String HOST = "localhost";
    private static final int PORTA = 5000;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;

    private Thread threadRecebimento;

    private Consumer<String> aoReceberMensagem;

    public boolean conectar() {

        try {

            socket = new Socket(
                    HOST,
                    PORTA
            );

            entrada =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            saida =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            return true;

        } catch (IOException e) {

            System.out.println(
                    "Erro ao conectar ao servidor: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public String fazerLogin(
            String nomeUsuario
    ) {

        if (saida == null) {
            return null;
        }

        saida.println(
                "LOGIN|" + nomeUsuario
        );

        try {

            return entrada.readLine();

        } catch (IOException e) {

            System.out.println(
                    "Erro ao receber resposta do login: "
                            + e.getMessage()
            );

            return null;
        }
    }

    public void iniciarRecebimento(
            Consumer<String> aoReceberMensagem
    ) {

        this.aoReceberMensagem =
                aoReceberMensagem;

        threadRecebimento =
                new Thread(() -> {

                    try {

                        String mensagem;

                        while (
                                (mensagem =
                                        entrada.readLine())
                                        != null
                        ) {

                            if (
                                    this.aoReceberMensagem
                                            != null
                            ) {

                                this.aoReceberMensagem
                                        .accept(mensagem);
                            }
                        }

                    } catch (IOException e) {

                        if (
                                socket != null
                                        && !socket.isClosed()
                        ) {

                            System.out.println(
                                    "Erro ao receber mensagem: "
                                            + e.getMessage()
                            );
                        }
                    }

                });

        threadRecebimento.setDaemon(true);

        threadRecebimento.start();
    }

    public void enviarMensagem(
            String destinatario,
            String conteudo
    ) {

        if (saida == null) {
            return;
        }

        saida.println(
                "MESSAGE|"
                        + destinatario
                        + "|"
                        + conteudo
        );
    }

    public void desconectar() {

        try {

            if (socket != null) {

                socket.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro ao desconectar: "
                            + e.getMessage()
            );
        }
    }
}