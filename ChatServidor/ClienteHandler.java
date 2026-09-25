package ChatServidor;

import ChatServidor.Segurança.GerenciadorAES;
import ChatServidor.Segurança.GerenciadorHMAC;
import ChatServidor.Segurança.GerenciadorAssinatura;
import ChatServidor.Segurança.HandshakeServidor;
import ChatServidor.Segurança.SessaoSeguraServidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.List;

public class ClienteHandler implements Runnable {

    private final Socket cliente;

    private final GerenciadorClientes gerenciador;

    private final GerenciadorUsuariosBanco gerenciadorUsuariosBanco;

    private String nomeUsuario;

    private PrintWriter saida;

    private SessaoSeguraServidor sessaoSegura;

    private final GerenciadorAES gerenciadorAES;

    private final GerenciadorHMAC gerenciadorHMAC;

    private final GerenciadorAssinatura gerenciadorAssinatura;

    private final HandshakeServidor handshakeServidor;

    /*
     * Usuário que está aguardando a resposta
     * do desafio de autenticação.
     */
    private String usuarioLoginPendente;

    /*
     * Nonce enviado ao cliente.
     *
     * Ele é descartado depois que a autenticação
     * termina.
     */
    private String nonceLogin;

    public ClienteHandler(
            Socket cliente,
            GerenciadorClientes gerenciador,
            GerenciadorUsuariosBanco gerenciadorUsuariosBanco
    ) {

        this.cliente =
                cliente;

        this.gerenciador =
                gerenciador;

        this.gerenciadorUsuariosBanco =
                gerenciadorUsuariosBanco;

        this.gerenciadorAES =
                new GerenciadorAES();

        this.gerenciadorHMAC =
                new GerenciadorHMAC();

        this.gerenciadorAssinatura =
                new GerenciadorAssinatura();

        this.handshakeServidor =
                new HandshakeServidor();
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

                processarMensagem(
                        mensagem
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "Erro ao atender cliente: "
                            + e.getMessage()
            );

        } finally {

            gerenciador.removerCliente(
                    this
            );

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

        /*
         * O primeiro comando da conexão precisa
         * ser o CLIENT_HELLO.
         */
        if (sessaoSegura == null) {

            if (
                    mensagem.startsWith(
                            "CLIENT_HELLO|"
                    )
            ) {

                processarClientHello(
                        mensagem
                );

            } else {

                saida.println(
                        "ERRO|Handshake seguro não estabelecido"
                );
            }

            return;
        }

        /*
         * Depois do handshake, somente pacotes
         * SECURE são aceitos.
         */
        if (
                !mensagem.startsWith(
                        "SECURE|"
                )
        ) {

            saida.println(
                    "ERRO|Mensagem não protegida rejeitada"
            );

            return;
        }

        String mensagemDescriptografada =
                descriptografarMensagem(
                        mensagem
                );

        if (
                mensagemDescriptografada
                        == null
        ) {

            return;
        }

        processarMensagemInterna(
                mensagemDescriptografada
        );
    }

    private void processarClientHello(
            String mensagem
    ) {

        String[] partes =
                mensagem.split(
                        "\\|",
                        3
                );

        if (partes.length < 3) {

            saida.println(
                    "ERRO|CLIENT_HELLO inválido"
            );

            return;
        }

        String chavePublicaCliente =
                partes[1];

        String saltBase64 =
                partes[2];

        try {

            /*
             * O servidor gera seu par DH.
             */
            var parDH =
                    handshakeServidor.gerarParDH();

            /*
             * O servidor deriva suas chaves usando:
             *
             * segredo DH + salt enviado pelo cliente.
             */
            sessaoSegura =
                    handshakeServidor.finalizarHandshake(
                            parDH,
                            chavePublicaCliente,
                            saltBase64
                    );

            String chavePublicaServidor =
                    handshakeServidor
                            .obterChavePublicaDH(
                                    parDH
                            );

            /*
             * SERVER_HELLO também faz parte do
             * handshake inicial e ainda não é
             * criptografado.
             */
            saida.println(
                    "SERVER_HELLO|"
                            + chavePublicaServidor
            );

            System.out.println(
                    "Handshake seguro estabelecido com cliente: "
                            + cliente.getInetAddress()
                                    .getHostAddress()
            );

        } catch (Exception e) {

            sessaoSegura =
                    null;

            saida.println(
                    "ERRO|Falha no handshake seguro"
            );

            System.out.println(
                    "Erro ao processar CLIENT_HELLO: "
                            + e.getMessage()
            );
        }
    }

    private String descriptografarMensagem(
            String mensagem
    ) {

        String[] partes =
                mensagem.split(
                        "\\|",
                        3
                );

        if (partes.length < 3) {

            System.out.println(
                    "Pacote SECURE inválido."
            );

            return null;
        }

        String ciphertext =
                partes[1];

        String hmacRecebido =
                partes[2];

        /*
         * PRIMEIRO verifica o HMAC.
         *
         * Somente depois será feita a
         * descriptografia.
         */
        boolean hmacValido =
                gerenciadorHMAC.verificarHMAC(
                        ciphertext,
                        hmacRecebido,
                        sessaoSegura
                                .getChaveHMAC()
                                .getEncoded()
                );

        if (!hmacValido) {

            System.out.println(
                    "HMAC inválido. Mensagem descartada."
            );

            return null;
        }

        try {

            String mensagemDescriptografada =
                    gerenciadorAES.descriptografar(
                    ciphertext,
                    sessaoSegura
                            .getChaveAES()
                            .getEncoded()
                    );

            sessaoSegura.registrarMensagem();

            return mensagemDescriptografada;

        } catch (RuntimeException e) {

            System.out.println(
                    "Erro ao descriptografar mensagem: "
                            + e.getMessage()
            );

            return null;
        }
    }

    private void processarMensagemInterna(
            String mensagem
    ) {

        /*
         * REGISTER possui quatro partes:
         *
         * REGISTER | nome | senha | chavePublica
         */
        if (
                mensagem.startsWith(
                        "REGISTER|"
                )
        ) {

            String[] partesCadastro =
                    mensagem.split(
                            "\\|",
                            4
                    );

            processarCadastro(
                    partesCadastro
            );

            return;
        }

        /*
         * LOGIN_SIGNATURE possui duas partes:
         *
         * LOGIN_SIGNATURE | assinatura
         */
        if (
                mensagem.startsWith(
                        "LOGIN_SIGNATURE|"
                )
        ) {

            String[] partesAssinatura =
                    mensagem.split(
                            "\\|",
                            2
                    );

            processarAssinaturaLogin(
                    partesAssinatura
            );

            return;
        }

        if (
                mensagem.startsWith(
                        "LOGIN_NEW_DEVICE|"
                )
        ) {

            String[] partesNovoDispositivo =
                    mensagem.split(
                            "\\|",
                            4
                    );

            processarLoginNovoDispositivo(
                    partesNovoDispositivo
            );

            return;
        }

        if (
                mensagem.startsWith(
                        "PUBLIC_KEY_REQUEST|"
                )
        ) {

            processarPedidoChavePublica(
                    mensagem.split(
                            "\\|",
                            2
                    )
            );

            return;
        }

        String[] partes =
                mensagem.split(
                        "\\|",
                        3
                );

        if (partes.length == 0) {
            return;
        }

        String comando =
                partes[0];

        switch (comando) {

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

            case "READ":
                processarLeitura(partes);
                break;

            case "TYPING":
                processarDigitacao(partes);
                break;

            case "STOP_TYPING":
                processarParadaDigitacao(partes);
                break;

                        case "REKEY_REQUEST":
                                processarPedidoRenovacao(partes);
                                break;

                        case "E2EE_HELLO":
                        case "E2EE_HELLO_RESPONSE":
                        case "E2EE_AUTH_CHALLENGE":
                        case "E2EE_AUTH_RESPONSE":
                                processarControleE2EE(partes);
                                break;

            default:
                enviarMensagem(
                        "ERRO|Comando desconhecido"
                );
        }
    }

    private void processarPedidoRenovacao(
            String[] partes
    ) {

        if (partes.length < 3) {
            return;
        }

        try {
            String saltBase64 = partes[1];
            String chavePublicaCliente = partes[2];

            SessaoSeguraServidor sessaoAnterior =
                    sessaoSegura;

            var parDH =
                    handshakeServidor.gerarParDH();

            SessaoSeguraServidor novaSessao =
                    handshakeServidor.finalizarHandshake(
                            parDH,
                            chavePublicaCliente,
                            saltBase64
                    );

            enviarMensagemComSessao(
                    "REKEY_RESPONSE|"
                            + handshakeServidor
                                    .obterChavePublicaDH(
                                            parDH
                                    )
                            + "|"
                            + saltBase64,
                    sessaoAnterior
            );

            sessaoSegura = novaSessao;

            System.out.println(
                    "Sessão segura renovada para o cliente: "
                            + cliente.getInetAddress()
                                    .getHostAddress()
            );

        } catch (RuntimeException e) {
            System.out.println(
                    "Erro ao renovar sessão segura: "
                            + e.getMessage()
            );
        }
    }

    private void processarPedidoChavePublica(
            String[] partes
    ) {

        if (
                partes.length < 2
                        || nomeUsuario == null
        ) {

            enviarMensagem(
                    "E2EE_ERROR|Pedido de chave inválido"
            );

            return;
        }

        String contato =
                partes[1].trim();

        System.out.println(
                "E2EE: "
                        + nomeUsuario
                        + " solicitou a chave pública de "
                        + contato
        );

        String chavePublicaContato =
                gerenciadorUsuariosBanco
                        .obterChavePublica(
                                contato
                        );

        if (
                chavePublicaContato == null
                        || chavePublicaContato.trim().isEmpty()
        ) {

            enviarMensagem(
                    "E2EE_ERROR|Chave pública do contato não encontrada"
            );

            return;
        }

        enviarMensagem(
                "PUBLIC_KEY|"
                        + contato
                        + "|"
                        + chavePublicaContato
        );

        ClienteHandler clienteContato =
                gerenciador.encontrarCliente(
                        contato
                );

        if (clienteContato != null) {

            String chavePublicaSolicitante =
                    gerenciadorUsuariosBanco
                            .obterChavePublica(
                                    nomeUsuario
                            );

            clienteContato.enviarMensagem(
                    "KEY_REQUESTED|"
                            + nomeUsuario
                            + "|"
                            + chavePublicaSolicitante
            );
        }
    }

    private void processarControleE2EE(
            String[] partes
    ) {

        if (
                partes.length < 2
                        || nomeUsuario == null
        ) {

            return;
        }

        String destinatario =
                partes[1].trim();

        ClienteHandler clienteDestino =
                gerenciador.encontrarCliente(
                        destinatario
                );

        System.out.println(
                "E2EE: roteando "
                        + partes[0]
                        + " de "
                        + nomeUsuario
                        + " para "
                        + destinatario
        );

        if (clienteDestino == null) {

            enviarMensagem(
                    "E2EE_ERROR|Contato offline"
            );

            return;
        }

        StringBuilder encaminhada =
                new StringBuilder(
                        "E2EE_ROUTE|"
                                + nomeUsuario
                );

        encaminhada.append("|");

        encaminhada.append(
                partes[0]
        );

        for (int indice = 2; indice < partes.length; indice++) {

            encaminhada.append("|");
            encaminhada.append(partes[indice]);
        }

        clienteDestino.enviarMensagem(
                encaminhada.toString()
        );
    }

    private void processarLoginNovoDispositivo(
            String[] partes
    ) {

        if (partes.length < 4) {

            enviarMensagem(
                    "LOGIN_ERROR|Dados inválidos"
            );

            return;
        }

        String nome =
                partes[1].trim();

        String senha =
                partes[2];

        String chavePublica =
                partes[3].trim();

        if (
                nome.isEmpty()
                        || senha.isEmpty()
                        || chavePublica.isEmpty()
        ) {

            enviarMensagem(
                    "LOGIN_ERROR|Usuário, senha ou chave pública inválidos"
            );

            return;
        }

        boolean autenticado =
                gerenciadorUsuariosBanco
                        .autenticar(
                                nome,
                                senha
                        );

        if (!autenticado) {

            enviarMensagem(
                    "LOGIN_ERROR|Usuário ou senha incorretos"
            );

            return;
        }

        if (
                gerenciador.encontrarCliente(
                        nome
                ) != null
        ) {

            enviarMensagem(
                    "LOGIN_ERROR|Usuário já está online"
            );

            return;
        }

        boolean chaveAtualizada =
                gerenciadorUsuariosBanco
                        .atualizarChavePublica(
                                nome,
                                chavePublica
                        );

        if (!chaveAtualizada) {

            enviarMensagem(
                    "LOGIN_ERROR|Não foi possível atualizar a chave pública"
            );

            return;
        }

        notificarMudancaChavePublica(
                nome
        );

        limparMensagensOffline(
                nome
        );

        finalizarLogin(
                nome
        );
    }

        private void notificarMudancaChavePublica(
                        String nome
        ) {

                for (ClienteHandler cliente :
                                gerenciador.getClientes()) {

                        if (
                                        cliente != this
                                                        && cliente.getNomeUsuario() != null
                        ) {

                                cliente.enviarMensagem(
                                                "PUBLIC_KEY_CHANGED|"
                                                                + nome
                                );
                        }
                }
        }

    private void limparMensagensOffline(
            String nome
    ) {

        String sql =
                """
                DELETE FROM mensagens
                WHERE destinatario = ?
                    AND entregue = 0
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    nome
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao limpar mensagens offline:"
            );

            e.printStackTrace();
        }
    }

    private void processarCadastro(
            String[] partes
    ) {

        if (partes.length < 4) {

            enviarMensagem(
                    "REGISTER_ERROR|Dados inválidos"
            );

            return;
        }

        String nome =
                partes[1].trim();

        String senha =
                partes[2];

        String chavePublica =
                partes[3].trim();

        if (
                nome.isEmpty()
                        || senha.isEmpty()
                        || chavePublica.isEmpty()
        ) {

            enviarMensagem(
                    "REGISTER_ERROR|Usuário, senha ou chave pública inválidos"
            );

            return;
        }

        boolean cadastrado =
                gerenciadorUsuariosBanco
                        .cadastrarUsuario(
                                nome,
                                senha,
                                chavePublica
                        );

        if (cadastrado) {

            System.out.println(
                    "Novo usuário cadastrado: "
                            + nome
                            + " | Chave pública armazenada."
            );

            enviarMensagem(
                    "REGISTER_OK"
            );

        } else {

            enviarMensagem(
                    "REGISTER_ERROR|Usuário já existe"
            );
        }
    }

    private void processarLogin(
            String[] partes
    ) {

        /*
         * Novo fluxo:
         *
         * LOGIN | nome
         *
         * O servidor não recebe a senha.
         * Primeiro envia um nonce.
         */
        if (partes.length == 2) {

            String nome =
                    partes[1].trim();

            if (nome.isEmpty()) {

                enviarMensagem(
                        "LOGIN_ERROR|Usuário inválido"
                );

                return;
            }

            iniciarDesafioLogin(
                    nome
            );

            return;
        }

        /*
         * Fluxo antigo mantido temporariamente
         * durante a transição.
         *
         * Será removido depois que o cliente
         * estiver utilizando exclusivamente
         * o desafio Ed25519.
         */
        if (partes.length < 3) {

            enviarMensagem(
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

            enviarMensagem(
                    "LOGIN_ERROR|Usuário ou senha incorretos"
            );

            return;
        }

        finalizarLogin(
                nome
        );
    }

    private void iniciarDesafioLogin(
            String nome
    ) {

        /*
         * O usuário precisa existir e possuir
         * uma chave pública cadastrada.
         */
        String chavePublica =
                gerenciadorUsuariosBanco
                        .obterChavePublica(
                                nome
                        );

        if (
                chavePublica == null
                        || chavePublica.trim().isEmpty()
        ) {

            enviarMensagem(
                    "LOGIN_ERROR|Usuário não possui chave pública cadastrada"
            );

            return;
        }

        /*
         * Gera nonce criptograficamente aleatório.
         */
        byte[] nonceBytes =
                new byte[32];

        SecureRandom random =
                new SecureRandom();

        random.nextBytes(
                nonceBytes
        );

        nonceLogin =
                Base64
                        .getEncoder()
                        .encodeToString(
                                nonceBytes
                        );

        usuarioLoginPendente =
                nome;

        System.out.println(
                "Desafio de login enviado para: "
                        + nome
        );

        enviarMensagem(
                "LOGIN_CHALLENGE|"
                        + nonceLogin
        );
    }

    private void processarAssinaturaLogin(
            String[] partes
    ) {

        if (partes.length < 2) {

            enviarMensagem(
                    "LOGIN_ERROR|Assinatura não enviada"
            );

            limparDesafioLogin();

            return;
        }

        if (
                usuarioLoginPendente == null
                        || nonceLogin == null
        ) {

            enviarMensagem(
                    "LOGIN_ERROR|Nenhum desafio de login pendente"
            );

            return;
        }

        String assinatura =
                partes[1].trim();

        if (assinatura.isEmpty()) {

            enviarMensagem(
                    "LOGIN_ERROR|Assinatura inválida"
            );

            limparDesafioLogin();

            return;
        }

        String chavePublicaBase64 =
                gerenciadorUsuariosBanco
                        .obterChavePublica(
                                usuarioLoginPendente
                        );

        if (
                chavePublicaBase64 == null
                        || chavePublicaBase64.trim().isEmpty()
        ) {

            enviarMensagem(
                    "LOGIN_ERROR|Chave pública não encontrada"
            );

            limparDesafioLogin();

            return;
        }

        try {

            var chavePublica =
                    gerenciadorAssinatura
                            .base64ParaChavePublica(
                                    chavePublicaBase64
                            );

            boolean assinaturaValida =
                    gerenciadorAssinatura.verificar(
                            nonceLogin,
                            assinatura,
                            chavePublica
                    );

            if (!assinaturaValida) {

                System.out.println(
                        "Assinatura de login inválida: "
                                + usuarioLoginPendente
                );

                enviarMensagem(
                        "LOGIN_ERROR|Autenticação por assinatura recusada"
                );

                limparDesafioLogin();

                return;
            }

            String usuarioAutenticado =
                    usuarioLoginPendente;

            /*
             * O desafio não pode ser reutilizado.
             */
            limparDesafioLogin();

            System.out.println(
                    "Assinatura de login válida: "
                            + usuarioAutenticado
            );

            finalizarLogin(
                    usuarioAutenticado
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "Erro ao verificar assinatura de login: "
                            + e.getMessage()
            );

            enviarMensagem(
                    "LOGIN_ERROR|Não foi possível verificar a assinatura"
            );

            limparDesafioLogin();
        }
    }

    private void limparDesafioLogin() {

        usuarioLoginPendente =
                null;

        nonceLogin =
                null;
    }

    private void finalizarLogin(
            String nome
    ) {

        ClienteHandler usuarioConectado =
                gerenciador.encontrarCliente(
                        nome
                );

        if (usuarioConectado != null) {

            enviarMensagem(
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

        enviarMensagem(
                "LOGIN_OK|"
                        + nomeUsuario
        );

        List<String> usuariosCadastrados =
                gerenciadorUsuariosBanco
                        .listarUsuarios();

        String listaUsuarios =
                String.join(
                        ",",
                        usuariosCadastrados
                );

        gerenciador.enviarParaTodos(
                "USERS|"
                        + listaUsuarios
        );

        String usuariosOnline =
                gerenciador.obterUsuariosOnline();

        enviarMensagem(
                "ONLINE_USERS|"
                        + usuariosOnline
        );

        gerenciador.enviarParaTodos(
                "ONLINE|"
                        + nomeUsuario
        );

        entregarMensagensPendentes();
    }

    private void entregarMensagensPendentes() {

        if (nomeUsuario == null) {
            return;
        }

        String sql =
                """
                SELECT
                    id,
                    remetente,
                    conteudo
                FROM mensagens
                WHERE
                    destinatario = ?
                    AND entregue = 0
                    AND apagada_destinatario = 0
                ORDER BY data_hora ASC, id ASC
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(
                                sql
                        )
        ) {

            statement.setString(
                    1,
                    nomeUsuario
            );

            try (
                    ResultSet resultado =
                            statement.executeQuery()
            ) {

                while (resultado.next()) {

                    int id =
                            resultado.getInt(
                                    "id"
                            );

                    String remetente =
                            resultado.getString(
                                    "remetente"
                            );

                    String conteudo =
                            resultado.getString(
                                    "conteudo"
                            );

                    String mensagem =
                            "MESSAGE|"
                                    + id
                                    + "|"
                                    + remetente
                                    + "|"
                                    + conteudo;

                    enviarMensagem(
                            mensagem
                    );

                    marcarMensagemComoEntregue(
                            id
                    );

                    System.out.println(
                            "Mensagem offline entregue: "
                                    + "ID="
                                    + id
                                    + " | "
                                    + remetente
                                    + " -> "
                                    + nomeUsuario
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao entregar mensagens pendentes:"
            );

            e.printStackTrace();
        }
    }

    private void marcarMensagemComoEntregue(
            int id
    ) {

        String sql =
                """
                UPDATE mensagens
                SET entregue = 1
                WHERE
                    id = ?
                    AND destinatario = ?
                    AND entregue = 0
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    id
            );

            statement.setString(
                    2,
                    nomeUsuario
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao marcar mensagem como entregue:"
            );

            e.printStackTrace();
        }
    }

    private void processarMensagemChat(
            String[] partes
    ) {

        if (partes.length < 3) {

            enviarMensagem(
                    "ERRO|Mensagem inválida"
            );

            return;
        }

        String destinatario =
                partes[1];

        String conteudo =
                partes[2];

        if (nomeUsuario == null) {

            enviarMensagem(
                    "ERRO|Usuário não autenticado"
            );

            return;
        }

        if (
                destinatario == null
                        || destinatario.trim().isEmpty()
        ) {

            enviarMensagem(
                    "ERRO|Destinatário inválido"
            );

            return;
        }

        if (
                conteudo == null
                        || conteudo.trim().isEmpty()
        ) {

            enviarMensagem(
                    "ERRO|Mensagem vazia"
            );

            return;
        }

        int idMensagem =
                salvarMensagem(
                        nomeUsuario,
                        destinatario,
                        conteudo
                );

        if (idMensagem <= 0) {

            enviarMensagem(
                    "ERRO|Não foi possível salvar a mensagem"
            );

            return;
        }

        ClienteHandler clienteDestino =
                gerenciador.encontrarCliente(
                        destinatario
                );

        String mensagemTempoReal =
                "MESSAGE|"
                        + idMensagem
                        + "|"
                        + nomeUsuario
                        + "|"
                        + conteudo;

        if (clienteDestino != null) {

            clienteDestino.enviarMensagem(
                    mensagemTempoReal
            );

            marcarMensagemComoEntreguePara(
                    idMensagem,
                    destinatario
            );
        }

        enviarMensagem(
                mensagemTempoReal
        );
    }

    private void marcarMensagemComoEntreguePara(
            int id,
            String destinatario
    ) {

        String sql =
                """
                UPDATE mensagens
                SET entregue = 1
                WHERE
                    id = ?
                    AND destinatario = ?
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    id
            );

            statement.setString(
                    2,
                    destinatario
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao atualizar entrega da mensagem:"
            );

            e.printStackTrace();
        }
    }

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

            enviarMensagem(
                    "ERRO|Histórico inválido"
            );

            return;
        }

        if (nomeUsuario == null) {

            enviarMensagem(
                    "ERRO|Usuário não autenticado"
            );

            return;
        }

        String outroUsuario =
                partes[1].trim();

        if (outroUsuario.isEmpty()) {

            enviarMensagem(
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
                        conexao.prepareStatement(
                                sql
                        )
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

                while (resultado.next()) {

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

                    enviarMensagem(
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

                enviarMensagem(
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

            enviarMensagem(
                    "ERRO|Não foi possível carregar o histórico"
            );
        }
    }

    private void processarExclusaoMensagem(
            String[] partes
    ) {

        if (partes.length < 2) {

            enviarMensagem(
                    "DELETE_ERROR|ID inválido"
            );

            return;
        }

        if (nomeUsuario == null) {

            enviarMensagem(
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

            enviarMensagem(
                    "DELETE_ERROR|ID inválido"
            );

            return;
        }

        if (id <= 0) {

            enviarMensagem(
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
                        conexao.prepareStatement(
                                sql
                        )
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

                enviarMensagem(
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

                enviarMensagem(
                        "DELETE_ERROR|Mensagem não encontrada ou sem permissão"
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao apagar mensagem:"
            );

            e.printStackTrace();

            enviarMensagem(
                    "DELETE_ERROR|Não foi possível apagar a mensagem"
            );
        }
    }

    private void processarLeitura(
            String[] partes
    ) {

        if (partes.length < 2) {
            return;
        }

        if (nomeUsuario == null) {
            return;
        }

        String remetente =
                partes[1].trim();

        if (remetente.isEmpty()) {
            return;
        }

        marcarMensagensComoLidas(
                remetente
        );
    }

    private void marcarMensagensComoLidas(
            String remetente
    ) {

        String sqlSelecionar =
                """
                SELECT id
                FROM mensagens
                WHERE
                    remetente = ?
                    AND destinatario = ?
                    AND lida = 0
                    AND apagada_destinatario = 0
                ORDER BY id ASC
                """;

        String sqlAtualizar =
                """
                UPDATE mensagens
                SET lida = 1
                WHERE
                    id = ?
                    AND remetente = ?
                    AND destinatario = ?
                    AND lida = 0
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement selecionar =
                        conexao.prepareStatement(
                                sqlSelecionar
                        );

                PreparedStatement atualizar =
                        conexao.prepareStatement(
                                sqlAtualizar
                        )
        ) {

            selecionar.setString(
                    1,
                    remetente
            );

            selecionar.setString(
                    2,
                    nomeUsuario
            );

            try (
                    ResultSet resultado =
                            selecionar.executeQuery()
            ) {

                while (resultado.next()) {

                    int id =
                            resultado.getInt(
                                    "id"
                            );

                    atualizar.setInt(
                            1,
                            id
                    );

                    atualizar.setString(
                            2,
                            remetente
                    );

                    atualizar.setString(
                            3,
                            nomeUsuario
                    );

                    int alteradas =
                            atualizar.executeUpdate();

                    if (alteradas > 0) {

                        ClienteHandler clienteRemetente =
                                gerenciador.encontrarCliente(
                                        remetente
                                );

                        if (clienteRemetente != null) {

                            clienteRemetente.enviarMensagem(
                                    "READ|"
                                            + id
                            );
                        }

                        System.out.println(
                                "Mensagem marcada como lida: "
                                        + "ID="
                                        + id
                                        + " | "
                                        + remetente
                                        + " -> "
                                        + nomeUsuario
                        );
                    }
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao marcar mensagens como lidas:"
            );

            e.printStackTrace();
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

        if (
                saida == null
                        || sessaoSegura == null
        ) {

            return;
        }

        String ciphertext =
                gerenciadorAES.criptografar(
                        mensagem,
                        sessaoSegura
                                .getChaveAES()
                                .getEncoded()
                );

        String hmac =
                gerenciadorHMAC.gerarHMAC(
                        ciphertext,
                        sessaoSegura
                                .getChaveHMAC()
                                .getEncoded()
                );

        saida.println(
                "SECURE|"
                        + ciphertext
                        + "|"
                        + hmac
        );

        sessaoSegura.registrarMensagem();
    }

    private void enviarMensagemComSessao(
            String mensagem,
            SessaoSeguraServidor sessao
    ) {

        if (
                saida == null
                        || sessao == null
        ) {
            return;
        }

        String ciphertext =
                gerenciadorAES.criptografar(
                        mensagem,
                        sessao.getChaveAES().getEncoded()
                );

        String hmac =
                gerenciadorHMAC.gerarHMAC(
                        ciphertext,
                        sessao.getChaveHMAC().getEncoded()
                );

        saida.println(
                "SECURE|"
                        + ciphertext
                        + "|"
                        + hmac
        );

        sessao.registrarMensagem();
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }
}