package ChatCliente.Seguranca;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class GerenciadorSessoesE2EE {

    private final GerenciadorDH gerenciadorDH =
            new GerenciadorDH();

    private final GerenciadorHKDF gerenciadorHKDF =
            new GerenciadorHKDF();

    private final Map<String, SessaoE2EE> sessoes =
            new HashMap<>();

    public HandshakeE2EEPendente iniciarHandshake() {

        KeyPair parDH =
                gerenciadorDH.gerarParDeChaves();

        byte[] salt =
                gerenciadorHKDF.gerarSalt();

        return new HandshakeE2EEPendente(
                parDH,
                salt
        );
    }

    public HandshakeE2EEPendente responderHandshake(
            byte[] salt
    ) {

        return new HandshakeE2EEPendente(
                gerenciadorDH.gerarParDeChaves(),
                salt
        );
    }

    public synchronized SessaoE2EE finalizarSessao(
            String contato,
            KeyPair parDH,
            String chavePublicaDHContatoBase64,
            byte[] salt,
            PublicKey chavePublicaContato
    ) {

        PublicKey chavePublicaDHContato =
                gerenciadorDH.base64ParaChavePublica(
                        chavePublicaDHContatoBase64,
                        parDH.getPublic()
                );

        byte[] segredo =
                gerenciadorDH.gerarSegredoCompartilhado(
                        parDH.getPrivate(),
                        chavePublicaDHContato
                );

        GerenciadorHKDF.ChavesDerivadas chaves =
                gerenciadorHKDF.derivarChaves(
                        segredo,
                        salt
                );

        SessaoE2EE sessao =
                new SessaoE2EE(
                        contato,
                        parDH,
                        segredo,
                        salt,
                        chaves,
                        chavePublicaContato
                );

        sessoes.put(contato, sessao);
        return sessao;
    }

    public synchronized void guardar(
            String contato,
            SessaoE2EE sessao
    ) {
        sessoes.put(contato, sessao);
    }

    public synchronized SessaoE2EE obter(
            String contato
    ) {
        return sessoes.get(contato);
    }

    public synchronized void remover(
            String contato
    ) {
        sessoes.remove(contato);
    }

    public synchronized void limpar() {
        sessoes.clear();
    }

        public synchronized Collection<SessaoE2EE> todas() {
                return java.util.List.copyOf(
                                sessoes.values()
                );
        }
}
