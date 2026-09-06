package ChatServidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServidor {

    private static final int PORTA = 5000;

    public static void main(String[] args) {

        System.out.println(
                "Iniciando servidor..."
        );

        /*
         * Garante que o banco e as tabelas
         * existam antes de aceitar clientes.
         */
        ConexaoSQLite.criarTabelas();

        GerenciadorClientes gerenciadorClientes =
                new GerenciadorClientes();

        GerenciadorUsuariosBanco gerenciadorUsuariosBanco =
                new GerenciadorUsuariosBanco();

        try (
                ServerSocket servidor =
                        new ServerSocket(PORTA)
        ) {

            System.out.println(
                    "Servidor iniciado na porta "
                            + PORTA
            );

            System.out.println(
                    "Aguardando conexões..."
            );

            while (true) {

                Socket cliente =
                        servidor.accept();

                System.out.println(
                        "Novo cliente conectado!"
                );

                ClienteHandler clienteHandler =
                        new ClienteHandler(
                                cliente,
                                gerenciadorClientes,
                                gerenciadorUsuariosBanco
                        );

                Thread thread =
                        new Thread(clienteHandler);

                thread.start();
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro no servidor: "
                            + e.getMessage()
            );
        }
    }
}