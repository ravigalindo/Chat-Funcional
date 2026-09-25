package ChatCliente;

import ChatCliente.Seguranca.GerenciadorAES;
import ChatCliente.Seguranca.GerenciadorHMAC;
import ChatCliente.Seguranca.GerenciadorAssinatura;
import ChatCliente.Seguranca.HandshakeCliente;
import ChatCliente.Seguranca.HandshakeE2EEPendente;
import ChatCliente.Seguranca.GerenciadorSessoesE2EE;
import ChatCliente.Seguranca.SessaoSegura;
import ChatCliente.Seguranca.SessaoE2EE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        private final ChaveAssinaturaLocal chaveAssinaturaLocal;

        private final GerenciadorSessoesE2EE gerenciadorSessoesE2EE;

        private final Map<String, HandshakeE2EEPendente> handshakesE2EE;

        private final Map<String, PublicKey> chavesPublicasContatos;

        private final Map<String, String> desafiosE2EE;

        private final Map<String, List<String>> mensagensPendentesE2EE;

        private KeyPair parDHRenovacao;

        private byte[] saltRenovacao;

        private boolean renovacaoEmAndamento;

        private final List<String> mensagensSegurasPendentes;

    public ClienteTCP() {

        gerenciadorAES =
                new GerenciadorAES();

        gerenciadorHMAC =
                new GerenciadorHMAC();

        gerenciadorAssinatura =
                new GerenciadorAssinatura();

        handshakeCliente =
                new HandshakeCliente();

        chaveAssinaturaLocal =
                new ChaveAssinaturaLocal();

        gerenciadorSessoesE2EE =
                new GerenciadorSessoesE2EE();

        handshakesE2EE =
                new HashMap<>();

        chavesPublicasContatos =
                new HashMap<>();

        desafiosE2EE =
                new HashMap<>();

        mensagensPendentesE2EE =
                new HashMap<>();

        mensagensSegurasPendentes =
                new ArrayList<>();
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

        String resposta = lerRespostaSegura(
                "cadastro"
        );

        if ("REGISTER_OK".equals(resposta)) {
            chaveAssinaturaLocal.salvar(
                    nomeUsuario,
                    senha,
                    parChavesAssinatura
            );
        }

        return resposta;
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

            parChavesAssinatura =
                    chaveAssinaturaLocal.carregar(
                            nomeUsuario,
                            senha
                    );
        }

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

        chaveAssinaturaLocal.salvar(
                nomeUsuario,
                senha,
                novoParChaves
        );
    }

    return resposta;
}

    private void enviarMensagemSegura(
            String mensagem
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

                boolean comandoRenovacao =
                                mensagem.startsWith("REKEY_REQUEST|")
                                                || mensagem.startsWith("REKEY_RESPONSE|");

                if (!comandoRenovacao && renovacaoEmAndamento) {
                        mensagensSegurasPendentes.add(mensagem);
                        return;
                }

                if (!comandoRenovacao
                                && sessaoSegura.precisaRenovar()) {

                        mensagensSegurasPendentes.add(mensagem);
                        iniciarRenovacaoSessao();
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

    private void iniciarRenovacaoSessao() {

        if (renovacaoEmAndamento) {
            return;
        }

        parDHRenovacao =
                handshakeCliente.gerarParDH();

        saltRenovacao =
                handshakeCliente.gerarSalt();

        renovacaoEmAndamento = true;

        enviarMensagemSegura(
                "REKEY_REQUEST|"
                        + Base64.getEncoder().encodeToString(
                                saltRenovacao
                        )
                        + "|"
                        + handshakeCliente.obterChavePublicaDH(
                                parDHRenovacao
                        )
        );

        System.out.println(
                "Sessão segura: renovação solicitada."
        );
    }

    private boolean processarMensagemRenovacao(
            String mensagem
    ) {

        if (!mensagem.startsWith("REKEY_RESPONSE|")) {
            return false;
        }

        String[] partes =
                mensagem.split("\\|", 3);

        if (partes.length < 3
                || parDHRenovacao == null
                || saltRenovacao == null) {
            return true;
        }

        try {
            sessaoSegura =
                    handshakeCliente.finalizarHandshake(
                            parDHRenovacao,
                            partes[1],
                            saltRenovacao
                    );

            parDHRenovacao = null;
            saltRenovacao = null;
            renovacaoEmAndamento = false;

            List<String> pendentes =
                    new ArrayList<>(
                            mensagensSegurasPendentes
                    );

            mensagensSegurasPendentes.clear();

            System.out.println(
                    "Sessão segura: renovação concluída."
            );

            for (String pendente : pendentes) {
                enviarMensagemSegura(pendente);
            }

        } catch (RuntimeException e) {
            System.out.println(
                    "Sessão segura: falha na renovação: "
                            + e.getMessage()
            );
        }

        return true;
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

            String mensagemProcessada =
                    processarMensagemSegura(
                            resposta
                    );

            if (mensagemProcessada == null) {
                return null;
            }

            if (processarMensagemRenovacao(
                    mensagemProcessada
            )) {
                return lerRespostaSegura(
                        operacao
                );
            }

            if (processarMensagemE2EE(
                    mensagemProcessada
            )) {
                return lerRespostaSegura(
                        operacao
                );
            }

            return mensagemProcessada;

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

            sessaoSegura.registrarMensagem();

            return mensagemDescriptografada;

        } catch (RuntimeException e) {

            System.out.println(
                    "Erro ao descriptografar mensagem segura: "
                            + e.getMessage()
            );

            return null;
        }
    }

    private boolean processarMensagemE2EE(
            String mensagem
    ) {

        if (mensagem.startsWith("PUBLIC_KEY|")) {

            String[] partes =
                    mensagem.split("\\|", 3);

            if (partes.length == 3) {

                guardarChavePublica(
                        partes[1],
                        partes[2]
                );

                if (mensagensPendentesE2EE.containsKey(
                        partes[1]
                )) {

                    iniciarHandshakeE2EE(
                            partes[1]
                    );
                }
            }

            return true;
        }

        if (mensagem.startsWith("KEY_REQUESTED|")) {

            String[] partes =
                    mensagem.split("\\|", 3);

            if (partes.length == 3) {

                guardarChavePublica(
                        partes[1],
                        partes[2]
                );
            }

            return true;
        }

        if (mensagem.startsWith("PUBLIC_KEY_CHANGED|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length == 2) {

                chavesPublicasContatos.remove(
                        partes[1]
                );

                gerenciadorSessoesE2EE.remover(
                        partes[1]
                );

                handshakesE2EE.remove(
                        partes[1]
                );
            }

            return true;
        }

        if (!mensagem.startsWith("E2EE_ROUTE|")) {
            return false;
        }

        String[] partes =
                mensagem.split("\\|", 5);

        if (partes.length < 4) {
            return true;
        }

        String contato = partes[1];
        String comando = partes[2];

        try {

            if ("E2EE_HELLO".equals(comando)) {

                if (partes.length < 5) {
                    return true;
                }

                byte[] salt =
                        Base64.getDecoder().decode(
                                partes[3]
                        );

                HandshakeE2EEPendente pendente =
                        gerenciadorSessoesE2EE
                                .responderHandshake(
                                        salt
                                );

                handshakesE2EE.put(
                        contato,
                        pendente
                );

                finalizarSessaoE2EE(
                        contato,
                        pendente,
                        partes[4]
                );

                enviarMensagemSegura(
                        "E2EE_HELLO_RESPONSE|"
                                + contato
                                + "|"
                                + pendente.getSaltBase64()
                                + "|"
                                + handshakeCliente
                                        .obterChavePublicaDH(
                                                pendente.getParChavesDH()
                                        )
                );

                enviarDesafioE2EE(
                        contato
                );

                return true;
            }

            if ("E2EE_HELLO_RESPONSE".equals(comando)) {

                if (partes.length < 5) {
                    return true;
                }

                HandshakeE2EEPendente pendente =
                        handshakesE2EE.get(
                                contato
                        );

                if (pendente == null) {
                    return true;
                }

                finalizarSessaoE2EE(
                        contato,
                        pendente,
                        partes[4]
                );

                enviarDesafioE2EE(
                        contato
                );

                return true;
            }

            if ("E2EE_AUTH_CHALLENGE".equals(comando)) {

                if (partes.length < 4) {
                    return true;
                }

                responderDesafioE2EE(
                        contato,
                        partes[3]
                );

                return true;
            }

            if ("E2EE_AUTH_RESPONSE".equals(comando)) {

                if (partes.length < 5) {
                    return true;
                }

                verificarRespostaE2EE(
                        contato,
                        partes[3],
                        partes[4]
                );

                return true;
            }

        } catch (RuntimeException e) {

            System.out.println(
                    "Falha no controle E2EE: "
                            + e.getMessage()
            );
        }

        return true;
    }

    private void guardarChavePublica(
            String contato,
            String chaveBase64
    ) {

        chavesPublicasContatos.put(
                contato,
                gerenciadorAssinatura
                        .base64ParaChavePublica(
                                chaveBase64
                        )
        );
    }

    private void finalizarSessaoE2EE(
            String contato,
            HandshakeE2EEPendente pendente,
            String chaveDHContato
    ) {

        PublicKey chavePublicaContato =
                chavesPublicasContatos.get(
                        contato
                );

        if (chavePublicaContato == null) {
            return;
        }

        SessaoE2EE sessao =
                gerenciadorSessoesE2EE
                        .finalizarSessao(
                                contato,
                                pendente.getParChavesDH(),
                                chaveDHContato,
                                pendente.getSalt(),
                                chavePublicaContato
                        );

        gerenciadorSessoesE2EE.guardar(
                contato,
                sessao
        );
    }

    private void enviarDesafioE2EE(
            String contato
    ) {

        byte[] bytes =
                new byte[32];

        new SecureRandom().nextBytes(bytes);

        String nonce =
                Base64.getEncoder().encodeToString(bytes);

        desafiosE2EE.put(
                contato,
                nonce
        );

        enviarMensagemSegura(
                "E2EE_AUTH_CHALLENGE|"
                        + contato
                        + "|"
                        + nonce
        );
    }

    private void responderDesafioE2EE(
            String contato,
            String nonce
    ) {

        if (parChavesAssinatura == null) {
            return;
        }

        String assinatura =
                gerenciadorAssinatura.assinar(
                        nonce,
                        parChavesAssinatura.getPrivate()
                );

        enviarMensagemSegura(
                "E2EE_AUTH_RESPONSE|"
                        + contato
                        + "|"
                        + nonce
                        + "|"
                        + assinatura
        );
    }

    private void verificarRespostaE2EE(
            String contato,
            String nonce,
            String assinatura
    ) {

        String nonceEsperado =
                desafiosE2EE.get(
                        contato
                );

        PublicKey chavePublica =
                chavesPublicasContatos.get(
                        contato
                );

        SessaoE2EE sessao =
                gerenciadorSessoesE2EE.obter(
                        contato
                );

        if (
                nonceEsperado != null
                        && nonceEsperado.equals(nonce)
                        && chavePublica != null
                        && sessao != null
                        && gerenciadorAssinatura.verificar(
                                nonce,
                                assinatura,
                                chavePublica
                        )
        ) {

            sessao.marcarComoAutenticada();
            desafiosE2EE.remove(contato);

            System.out.println(
                    "E2EE: contato autenticado: "
                            + contato
            );

            enviarMensagensPendentesE2EE(
                    contato
            );
        }
    }

    private void enviarMensagensPendentesE2EE(
            String contato
    ) {

        List<String> pendentes =
                mensagensPendentesE2EE.remove(
                        contato
                );

        if (pendentes == null) {
            return;
        }

        for (String conteudo : pendentes) {
            enviarMensagem(
                    contato,
                    conteudo
            );
        }
    }

    private String descriptografarMensagemE2EE(
            String mensagem
    ) {

        if (mensagem.startsWith("HISTORY_MESSAGE|")) {
            return descriptografarHistoricoE2EE(
                    mensagem
            );
        }

        if (!mensagem.startsWith("MESSAGE|")) {
            return null;
        }

        String[] partes =
                mensagem.split("\\|", 4);

        if (partes.length < 4
                || !partes[3].startsWith("E2EE|")) {
            return null;
        }

        String pacote =
                partes[3].substring("E2EE|".length());

        SessaoE2EE sessao =
                gerenciadorSessoesE2EE.obter(
                        partes[2]
                );

        if (sessao == null) {

            for (SessaoE2EE candidata :
                    gerenciadorSessoesE2EE.todas()) {

                String texto =
                        candidata.descriptografar(
                                pacote
                        );

                if (texto != null) {
                    return partes[0]
                            + "|"
                            + partes[1]
                            + "|"
                            + partes[2]
                            + "|"
                            + texto;
                }
            }

            return null;
        }

        String texto =
                sessao.descriptografar(
                        pacote
                );

        if (texto == null) {
            return null;
        }

        return partes[0]
                + "|"
                + partes[1]
                + "|"
                + partes[2]
                + "|"
                + texto;
    }

    private String descriptografarHistoricoE2EE(
            String mensagem
    ) {

        String[] partes =
                mensagem.split("\\|", 6);

        if (partes.length < 6
                || !partes[4].startsWith("E2EE|")) {
            return null;
        }

        String texto =
                descriptografarComSessoes(
                        partes[2],
                        partes[4].substring(
                                "E2EE|".length()
                        )
                );

        if (texto == null) {
            return null;
        }

        return partes[0]
                + "|"
                + partes[1]
                + "|"
                + partes[2]
                + "|"
                + partes[3]
                + "|"
                + texto
                + "|"
                + partes[5];
    }

    private String descriptografarComSessoes(
            String remetente,
            String pacote
    ) {

        SessaoE2EE sessao =
                gerenciadorSessoesE2EE.obter(
                        remetente
                );

        if (sessao != null) {
            return sessao.descriptografar(
                    pacote
            );
        }

        for (SessaoE2EE candidata :
                gerenciadorSessoesE2EE.todas()) {

            String texto =
                    candidata.descriptografar(
                            pacote
                    );

            if (texto != null) {
                return texto;
            }
        }

        return null;
    }

    private boolean possuiPayloadE2EE(
            String mensagem
    ) {

        if (mensagem.startsWith("MESSAGE|")) {

            String[] partes =
                    mensagem.split("\\|", 4);

            return partes.length >= 4
                    && partes[3].startsWith("E2EE|");
        }

        if (mensagem.startsWith("HISTORY_MESSAGE|")) {

            String[] partes =
                    mensagem.split("\\|", 6);

            return partes.length >= 6
                    && partes[4].startsWith("E2EE|");
        }

        return false;
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

                                                        if (processarMensagemRenovacao(
                                                                        mensagemProcessada
                                                        )) {

                                                                continue;
                                                        }

                            if (processarMensagemE2EE(
                                    mensagemProcessada
                            )) {

                                continue;
                            }

                            String mensagemE2EE =
                                    descriptografarMensagemE2EE(
                                            mensagemProcessada
                                    );

                            if (
                                    possuiPayloadE2EE(
                                            mensagemProcessada
                                    )
                                    && mensagemE2EE == null
                            ) {

                                continue;
                            }

                            if (mensagemE2EE != null) {
                                mensagemProcessada = mensagemE2EE;
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

        SessaoE2EE sessaoE2EE =
                gerenciadorSessoesE2EE.obter(
                        destinatario
                );

        if (
                sessaoE2EE == null
                        || !sessaoE2EE.estaAutenticada()
        ) {

            mensagensPendentesE2EE
                    .computeIfAbsent(
                            destinatario,
                            chave -> new ArrayList<>()
                    )
                    .add(
                            conteudo
                    );

            System.out.println(
                    "E2EE: mensagem aguardando handshake com "
                            + destinatario
            );

            iniciarHandshakeE2EE(
                    destinatario
            );

            return;
        }

        String mensagemCifrada =
                sessaoE2EE.criptografar(
                        conteudo
                );

        enviarMensagemSegura(
                "MESSAGE|"
                        + destinatario
                        + "|E2EE|"
                        + mensagemCifrada
        );

        System.out.println(
                "E2EE: mensagem cifrada enviada para "
                        + destinatario
        );
    }

    public void solicitarChavePublica(
            String contato
    ) {

        if (!sessaoSeguraValida()) {
            return;
        }

        enviarMensagemSegura(
                "PUBLIC_KEY_REQUEST|"
                        + contato
        );
    }

    public void iniciarHandshakeE2EE(
            String contato
    ) {

        PublicKey chavePublica =
                chavesPublicasContatos.get(
                        contato
                );

        if (chavePublica == null) {

                        System.out.println(
                                        "E2EE: solicitando chave pública de "
                                                        + contato
                        );

            solicitarChavePublica(
                    contato
            );

            return;
        }

        HandshakeE2EEPendente pendente =
                gerenciadorSessoesE2EE
                        .iniciarHandshake();

        handshakesE2EE.put(
                contato,
                pendente
        );

        System.out.println(
                "E2EE: iniciando handshake com "
                        + contato
        );

        enviarMensagemSegura(
                "E2EE_HELLO|"
                        + contato
                        + "|"
                        + pendente.getSaltBase64()
                        + "|"
                        + handshakeCliente.obterChavePublicaDH(
                                pendente.getParChavesDH()
                        )
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

        System.out.println(
                "Presença: digitando para "
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

        System.out.println(
                "Presença: parou de digitar para "
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

                gerenciadorSessoesE2EE.limpar();
                handshakesE2EE.clear();
                desafiosE2EE.clear();
                mensagensPendentesE2EE.clear();
                mensagensSegurasPendentes.clear();
                parDHRenovacao = null;
                saltRenovacao = null;
                renovacaoEmAndamento = false;
    }
}