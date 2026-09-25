package ChatCliente;

import ChatCliente.Seguranca.GerenciadorAES;
import ChatCliente.Seguranca.GerenciadorHMAC;
import ChatCliente.Seguranca.GerenciadorAssinatura;
import ChatCliente.Seguranca.HandshakeCliente;
import ChatCliente.Seguranca.SessaoSegura;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Base64;
import java.util.function.Consumer;

public class ClienteTCP {

    private static final String HOST =
            "localhost";

    private static final int PORTA =
            5000;

    private Socket socket;

    private BufferedReader entrada;

    private PrintWriter saida;

    private Thread threadRecebimento;

    private Consumer<String> aoReceberMensagem;

    private SessaoSegura sessaoSegura;

    private final GerenciadorAES gerenciadorAES;

    private final GerenciadorHMAC gerenciadorHMAC;

    private final GerenciadorAssinatura gerenciadorAssinatura;

    private final HandshakeCliente handshakeCliente;

    private KeyPair parChavesAssinatura;

    public ClienteTCP() {

        gerenciadorAES =
                new GerenciadorAES();

        gerenciadorHMAC =
                new GerenciadorHMAC();

        gerenciadorAssinatura =
                new GerenciadorAssinatura();

        handshakeCliente =
                new HandshakeCliente();
    }

    public boolean conectar() {

        try {

            socket =
                    new Socket(
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

            if (!estabelecerHandshake()) {

                System.out.println(
                        "Falha ao estabelecer handshake seguro."
                );

                desconectar();

                return false;
            }

            System.out.println(
                    "Handshake seguro estabelecido."
            );

            return true;

        } catch (IOException e) {

            System.out.println(
                    "Erro ao conectar ao servidor: "
                            + e.getMessage()
            );

            desconectar();

            return false;
        }
    }

    private boolean estabelecerHandshake() {

        try {

            KeyPair parDH =
                    handshakeCliente.gerarParDH();

            String chavePublicaCliente =
                    handshakeCliente.obterChavePublicaDH(
                            parDH
                    );

            byte[] salt =
                    handshakeCliente.gerarSalt();

            String saltBase64 =
                    Base64
                            .getEncoder()
                            .encodeToString(
                                    salt
                            );

            /*
             * O primeiro contato ainda não possui
             * uma sessão segura.
             *
             * Portanto, o CLIENT_HELLO é enviado
             * sem criptografia.
             */
            saida.println(
                    "CLIENT_HELLO|"
                            + chavePublicaCliente
                            + "|"
                            + saltBase64
            );

            String resposta =
                    entrada.readLine();

            if (resposta == null) {

                System.out.println(
                        "Servidor encerrou a conexão durante o handshake."
                );

                return false;
            }

            String[] partes =
                    resposta.split(
                            "\\|",
                            2
                    );

            if (
                    partes.length < 2
                            || !"SERVER_HELLO".equals(
                                    partes[0]
                            )
            ) {

                System.out.println(
                        "Resposta de handshake inválida: "
                                + resposta
                );

                return false;
            }

            String chavePublicaServidor =
                    partes[1];

            sessaoSegura =
                    handshakeCliente.finalizarHandshake(
                            parDH,
                            chavePublicaServidor,
                            salt
                    );

            return sessaoSegura != null;

        } catch (Exception e) {

            System.out.println(
                    "Erro durante o handshake: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public String cadastrarUsuario(
            String nomeUsuario,
            String senha
    ) {

        if (!sessaoSeguraValida()) {
            return null;
        }

        /*
         * Gera o par de chaves assimétricas
         * do usuário.
         *
         * A chave privada permanece somente
         * no cliente.
         */
        if (parChavesAssinatura == null) {

            parChavesAssinatura =
                    gerenciadorAssinatura
                            .gerarParDeChaves();
        }

        String chavePublicaBase64 =
                gerenciadorAssinatura
                        .chavePublicaParaBase64(
                                parChavesAssinatura.getPublic()
                        );

        String comando =
                "REGISTER|"
                        + nomeUsuario
                        + "|"
                        + senha
                        + "|"
                        + chavePublicaBase64;

        enviarMensagemSegura(
                comando
        );

        return lerRespostaSegura(
                "cadastro"
        );
    }

    public String fazerLogin(
            String nomeUsuario,
            String senha
    ) {

        if (!sessaoSeguraValida()) {
            return null;
        }

        /*
         * A senha não é enviada no login de um
         * dispositivo já cadastrado.
         *
         * O servidor enviará um desafio (nonce)
         * e o cliente deverá assiná-lo com sua
         * chave privada Ed25519.
         *
         * O parâmetro senha permanece no método
         * apenas para manter a compatibilidade
         * com a chamada atual da interface.
         */
        if (parChavesAssinatura == null) {

            System.out.println(
                    "Chave privada Ed25519 não disponível. "
                            + "Será necessário autenticar como novo dispositivo."
            );

            return "LOGIN_ERROR|Chave privada não disponível.";
        }

        enviarMensagemSegura(
                "LOGIN|"
                        + nomeUsuario
        );

        String desafio =
                lerRespostaSegura(
                        "login"
                );

        if (desafio == null) {

            return null;
        }

        if (
                desafio.startsWith(
                        "LOGIN_CHALLENGE|"
                )
        ) {

            String[] partes =
                    desafio.split(
                            "\\|",
                            2
                    );

            if (partes.length < 2) {

                System.out.println(
                        "Desafio de login inválido."
                );

                return null;
            }

            String nonce =
                    partes[1];

            String assinatura =
                    gerenciadorAssinatura.assinar(
                            nonce,
                            parChavesAssinatura.getPrivate()
                    );

            enviarMensagemSegura(
                    "LOGIN_SIGNATURE|"
                            + assinatura
            );

            return lerRespostaSegura(
                    "login"
            );
        }

        /*
         * Caso o servidor retorne diretamente um
         * erro de login, repassa a resposta.
         */
        return desafio;
    }

    public String fazerLoginNovoDispositivo(
        String nomeUsuario,
        String senha
) {
    if (!sessaoSeguraValida()) {
        return null;
    }

    /*
     * Um novo dispositivo precisa gerar
     * um novo par de chaves Ed25519.
     */
    KeyPair novoParChaves =
            gerenciadorAssinatura
                    .gerarParDeChaves();

    String chavePublicaBase64 =
            gerenciadorAssinatura
                    .chavePublicaParaBase64(
                            novoParChaves.getPublic()
                    );

    /*
     * A senha é enviada somente dentro
     * do canal já protegido por AES + HMAC.
     */
    String comando =
            "LOGIN_NEW_DEVICE|"
                    + nomeUsuario
                    + "|"
                    + senha
                    + "|"
                    + chavePublicaBase64;

    enviarMensagemSegura(comando);

    String resposta =
            lerRespostaSegura(
                    "login de novo dispositivo"
            );

    /*
     * Só guarda a nova chave privada se
     * o servidor confirmar a autenticação.
     */
    if (
            resposta != null
                    && resposta.startsWith(
                            "LOGIN_OK|"
                    )
    ) {
        parChavesAssinatura =
                novoParChaves;
    }

    return resposta;
}

    private void enviarMensagemSegura(
            String mensagem
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        String ciphertext =
                gerenciadorAES.criptografar(
                        mensagem,
                        sessaoSegura.getChaveAES()
                );

        String hmac =
                gerenciadorHMAC.gerarHMAC(
                        ciphertext,
                        sessaoSegura.getChaveHMAC()
                );

        String pacote =
                "SECURE|"
                        + ciphertext
                        + "|"
                        + hmac;

        saida.println(
                pacote
        );

        sessaoSegura.registrarMensagem();
    }

    private String lerRespostaSegura(
            String operacao
    ) {

        try {

            String resposta =
                    entrada.readLine();

            if (resposta == null) {

                return null;
            }

            return processarMensagemSegura(
                    resposta
            );

        } catch (IOException e) {

            System.out.println(
                    "Erro ao receber resposta do "
                            + operacao
                            + ": "
                            + e.getMessage()
            );

            return null;
        }
    }

    private String processarMensagemSegura(
            String mensagem
    ) {

        if (
                mensagem == null
                        || !mensagem.startsWith(
                                "SECURE|"
                        )
        ) {

            System.out.println(
                    "Mensagem segura inválida recebida."
            );

            return null;
        }

        String[] partes =
                mensagem.split(
                        "\\|",
                        3
                );

        if (partes.length < 3) {

            System.out.println(
                    "Pacote seguro inválido."
            );

            return null;
        }

        String ciphertext =
                partes[1];

        String hmacRecebido =
                partes[2];

        boolean hmacValido =
                gerenciadorHMAC.verificarHMAC(
                        ciphertext,
                        hmacRecebido,
                        sessaoSegura.getChaveHMAC()
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
                            sessaoSegura.getChaveAES()
                    );

            return mensagemDescriptografada;

        } catch (RuntimeException e) {

            System.out.println(
                    "Erro ao descriptografar mensagem segura: "
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

                            String mensagemProcessada =
                                    processarMensagemSegura(
                                            mensagem
                                    );

                            if (
                                    mensagemProcessada
                                            == null
                            ) {

                                continue;
                            }

                            if (
                                    this.aoReceberMensagem
                                            != null
                            ) {

                                this.aoReceberMensagem
                                        .accept(
                                                mensagemProcessada
                                        );
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

        threadRecebimento.setDaemon(
                true
        );

        threadRecebimento.start();
    }

    public void enviarMensagem(
            String destinatario,
            String conteudo
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "MESSAGE|"
                        + destinatario
                        + "|"
                        + conteudo
        );
    }

    public void solicitarHistorico(
            String usuario
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "HISTORY|"
                        + usuario
        );
    }

    public void marcarComoLidas(
            String remetente
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "READ|"
                        + remetente
        );
    }

    public void apagarMensagem(
            int id
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "DELETE_MESSAGE|"
                        + id
        );
    }

    public void enviarDigitacao(
            String destinatario
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "TYPING|"
                        + destinatario
        );
    }

    public void enviarParadaDigitacao(
            String destinatario
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "STOP_TYPING|"
                        + destinatario
        );
    }

    private boolean sessaoSeguraValida() {

        return saida != null
                && sessaoSegura != null;
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