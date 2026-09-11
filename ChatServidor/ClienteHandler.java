package ChatServidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ClienteHandler implements Runnable {

    private final Socket cliente;

    private final GerenciadorClientes gerenciador;

    private final GerenciadorUsuariosBanco gerenciadorUsuariosBanco;

    private String nomeUsuario;

    private PrintWriter saida;

    public ClienteHandler(
            Socket cliente,
            GerenciadorClientes gerenciador,
            GerenciadorUsuariosBanco gerenciadorUsuariosBanco
    ) {

        this.cliente = cliente;

        this.gerenciador = gerenciador;

        this.gerenciadorUsuariosBanco =
                gerenciadorUsuariosBanco;
    }

    @Override
    public void run() {

        try (
                BufferedReader entrada =
                        new BufferedReader(
                                new InputStreamReader(
                                        cliente.getInputStream()
                                )
                        )
        ) {

            saida =
                    new PrintWriter(
                            cliente.getOutputStream(),
                            true
                    );

            System.out.println(
                    "Cliente conectado: "
                            + cliente.getInetAddress()
                                    .getHostAddress()
            );

            String mensagem;

            while (
                    (mensagem =
                            entrada.readLine()) != null
            ) {

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

        if (partes.length == 0) {
            return;
        }

        String comando =
                partes[0];

        switch (comando) {

            case "REGISTER":

                processarCadastro(partes);

                break;

            case "LOGIN":

                processarLogin(partes);

                break;

            case "MESSAGE":

                processarMensagemChat(partes);

                break;

            case "HISTORY":

                processarHistorico(partes);

                break;

            case "DELETE_MESSAGE":

                processarExclusaoMensagem(partes);

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

    private void processarCadastro(
            String[] partes
    ) {

        if (partes.length < 3) {

            saida.println(
                    "REGISTER_ERROR|Dados inválidos"
            );

            return;
        }

        String nome =
                partes[1].trim();

        String senha =
                partes[2];

        if (nome.isEmpty()
                || senha.isEmpty()) {

            saida.println(
                    "REGISTER_ERROR|Usuário ou senha inválidos"
            );

            return;
        }

        boolean cadastrado =
                gerenciadorUsuariosBanco
                        .cadastrarUsuario(
                                nome,
                                senha
                        );

        if (cadastrado) {

            System.out.println(
                    "Novo usuário cadastrado: "
                            + nome
            );

            saida.println(
                    "REGISTER_OK"
            );

        } else {

            saida.println(
                    "REGISTER_ERROR|Usuário já existe"
            );
        }
    }

    private void processarLogin(
            String[] partes
    ) {

        if (partes.length < 3) {

            saida.println(
                    "LOGIN_ERROR|Dados inválidos"
            );

            return;
        }

        String nome =
                partes[1].trim();

        String senha =
                partes[2];

        boolean autenticado =
                gerenciadorUsuariosBanco
                        .autenticar(
                                nome,
                                senha
                        );

        if (!autenticado) {

            System.out.println(
                    "Tentativa de login recusada: "
                            + nome
            );

            saida.println(
                    "LOGIN_ERROR|Usuário ou senha incorretos"
            );

            return;
        }

        ClienteHandler usuarioConectado =
                gerenciador.encontrarCliente(
                        nome
                );

        if (usuarioConectado != null) {

            saida.println(
                    "LOGIN_ERROR|Usuário já está online"
            );

            return;
        }

        nomeUsuario =
                nome;

        System.out.println(
                "Usuário autenticado: "
                        + nomeUsuario
        );

        gerenciador.adicionarCliente(
                this
        );

        saida.println(
                "LOGIN_OK|"
                        + nomeUsuario
        );

        String usuariosOnline =
                gerenciador.obterUsuariosOnline();

        gerenciador.enviarParaTodos(
                "USERS|"
                        + usuariosOnline
        );

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

        if (nomeUsuario == null) {

            saida.println(
                    "ERRO|Usuário não autenticado"
            );

            return;
        }

        if (destinatario == null
                || destinatario.trim().isEmpty()) {

            saida.println(
                    "ERRO|Destinatário inválido"
            );

            return;
        }

        if (conteudo == null
                || conteudo.trim().isEmpty()) {

            saida.println(
                    "ERRO|Mensagem vazia"
            );

            return;
        }

        /*
         * Salva a mensagem no banco e recupera
         * o ID gerado pelo SQLite.
         */
        int idMensagem =
                salvarMensagem(
                        nomeUsuario,
                        destinatario,
                        conteudo
                );

        if (idMensagem <= 0) {

            saida.println(
                    "ERRO|Não foi possível salvar a mensagem"
            );

            return;
        }

        /*
         * Novo formato das mensagens em tempo real:
         *
         * MESSAGE|id|remetente|conteudo
         */
        String mensagemTempoReal =
                "MESSAGE|"
                        + idMensagem
                        + "|"
                        + nomeUsuario
                        + "|"
                        + conteudo;

        /*
         * Envia a mensagem para o destinatário,
         * caso ele esteja online.
         */
        ClienteHandler clienteDestino =
                gerenciador.encontrarCliente(
                        destinatario
                );

        if (clienteDestino != null) {

            clienteDestino.enviarMensagem(
                    mensagemTempoReal
            );
        }

        /*
         * Envia também para o próprio remetente.
         *
         * Isso permite que o cliente receba
         * o ID real gerado pelo banco.
         */
        saida.println(
                mensagemTempoReal
        );
    }

    /*
     * Salva a mensagem no banco e retorna
     * o ID gerado pelo SQLite.
     */
    private int salvarMensagem(
            String remetente,
            String destinatario,
            String conteudo
    ) {

        String sql =
                """
                INSERT INTO mensagens
                (remetente, destinatario, conteudo)
                VALUES (?, ?, ?)
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    remetente
            );

            statement.setString(
                    2,
                    destinatario
            );

            statement.setString(
                    3,
                    conteudo
            );

            statement.executeUpdate();

            try (
                    ResultSet resultado =
                            statement.getGeneratedKeys()
            ) {

                if (resultado.next()) {

                    int id =
                            resultado.getInt(1);

                    System.out.println(
                            "Mensagem salva no banco: "
                                    + remetente
                                    + " -> "
                                    + destinatario
                                    + " | ID="
                                    + id
                    );

                    return id;
                }
            }

            System.out.println(
                    "Mensagem salva, mas o ID não foi recuperado."
            );

            return -1;

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao salvar mensagem:"
            );

            e.printStackTrace();

            return -1;
        }
    }

    private void processarHistorico(
            String[] partes
    ) {

        if (partes.length < 2) {

            saida.println(
                    "ERRO|Histórico inválido"
            );

            return;
        }

        if (nomeUsuario == null) {

            saida.println(
                    "ERRO|Usuário não autenticado"
            );

            return;
        }

        String outroUsuario =
                partes[1].trim();

        if (outroUsuario.isEmpty()) {

            saida.println(
                    "ERRO|Usuário inválido"
            );

            return;
        }

        String sql =
                """
                SELECT
                    id,
                    remetente,
                    destinatario,
                    conteudo,
                    data_hora
                FROM mensagens
                WHERE
                    (
                        remetente = ?
                        AND destinatario = ?
                        AND apagada_remetente = 0
                    )
                    OR
                    (
                        remetente = ?
                        AND destinatario = ?
                        AND apagada_destinatario = 0
                    )
                ORDER BY data_hora ASC, id ASC
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    nomeUsuario
            );

            statement.setString(
                    2,
                    outroUsuario
            );

            statement.setString(
                    3,
                    outroUsuario
            );

            statement.setString(
                    4,
                    nomeUsuario
            );

            try (
                    ResultSet resultado =
                            statement.executeQuery()
            ) {

                while (
                        resultado.next()
                ) {

                    int id =
                            resultado.getInt(
                                    "id"
                            );

                    String remetente =
                            resultado.getString(
                                    "remetente"
                            );

                    String destinatario =
                            resultado.getString(
                                    "destinatario"
                            );

                    String conteudo =
                            resultado.getString(
                                    "conteudo"
                            );

                    String dataHora =
                            resultado.getString(
                                    "data_hora"
                            );

                    saida.println(
                            "HISTORY_MESSAGE|"
                                    + id
                                    + "|"
                                    + remetente
                                    + "|"
                                    + destinatario
                                    + "|"
                                    + conteudo
                                    + "|"
                                    + dataHora
                    );
                }

                saida.println(
                        "HISTORY_END"
                );

                System.out.println(
                        "Histórico enviado para: "
                                + nomeUsuario
                                + " / "
                                + outroUsuario
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao buscar histórico:"
            );

            e.printStackTrace();

            saida.println(
                    "ERRO|Não foi possível carregar o histórico"
            );
        }
    }

    private void processarExclusaoMensagem(
            String[] partes
    ) {

        if (partes.length < 2) {

            saida.println(
                    "DELETE_ERROR|ID inválido"
            );

            return;
        }

        if (nomeUsuario == null) {

            saida.println(
                    "DELETE_ERROR|Usuário não autenticado"
            );

            return;
        }

        int id;

        try {

            id =
                    Integer.parseInt(
                            partes[1].trim()
                    );

        } catch (NumberFormatException e) {

            saida.println(
                    "DELETE_ERROR|ID inválido"
            );

            return;
        }

        if (id <= 0) {

            saida.println(
                    "DELETE_ERROR|ID inválido"
            );

            return;
        }

        String sql =
                """
                UPDATE mensagens
                SET
                    apagada_remetente =
                        CASE
                            WHEN remetente = ?
                            THEN 1
                            ELSE apagada_remetente
                        END,
                    apagada_destinatario =
                        CASE
                            WHEN destinatario = ?
                            THEN 1
                            ELSE apagada_destinatario
                        END
                WHERE
                    id = ?
                    AND (
                        remetente = ?
                        OR destinatario = ?
                    )
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    nomeUsuario
            );

            statement.setString(
                    2,
                    nomeUsuario
            );

            statement.setInt(
                    3,
                    id
            );

            statement.setString(
                    4,
                    nomeUsuario
            );

            statement.setString(
                    5,
                    nomeUsuario
            );

            int linhasAlteradas =
                    statement.executeUpdate();

            if (linhasAlteradas > 0) {

                System.out.println(
                        "Mensagem "
                                + id
                                + " apagada para: "
                                + nomeUsuario
                );

                saida.println(
                        "DELETE_OK|"
                                + id
                );

            } else {

                System.out.println(
                        "Tentativa de apagar mensagem "
                                + id
                                + " sem permissão: "
                                + nomeUsuario
                );

                saida.println(
                        "DELETE_ERROR|Mensagem não encontrada ou sem permissão"
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao apagar mensagem:"
            );

            e.printStackTrace();

            saida.println(
                    "DELETE_ERROR|Não foi possível apagar a mensagem"
            );
        }
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