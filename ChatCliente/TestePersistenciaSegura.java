package ChatCliente;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestePersistenciaSegura {

    public static void main(String[] args) throws Exception {

        String usuario = "__teste_persistencia__";
        String senha = "senha-de-teste";

        HistoricoLocalCriptografado historico =
                new HistoricoLocalCriptografado(
                        usuario,
                        senha
                );

        HashMap<String, List<Mensagem>> mensagens =
                new HashMap<>();

        mensagens.put(
                "Contato",
                new ArrayList<>(
                        List.of(
                                new Mensagem(
                                        1,
                                        "Contato",
                                        "mensagem de teste",
                                        false
                                )
                        )
                )
        );

        historico.salvar(mensagens);

        if (!"mensagem de teste".equals(
                historico.carregar()
                        .get("Contato")
                        .get(0)
                        .getConteudo()
        )) {
            throw new IllegalStateException(
                    "Falha no histórico cifrado"
            );
        }

        KeyPairGenerator gerador =
                KeyPairGenerator.getInstance("Ed25519");
        KeyPair original = gerador.generateKeyPair();

        ChaveAssinaturaLocal chaveLocal =
                new ChaveAssinaturaLocal();
        chaveLocal.salvar(
                usuario,
                senha,
                original
        );

        KeyPair recuperada = chaveLocal.carregar(
                usuario,
                senha
        );

        if (recuperada == null
                || !Arrays.equals(
                        original.getPrivate().getEncoded(),
                        recuperada.getPrivate().getEncoded()
                )) {
            throw new IllegalStateException(
                    "Falha na chave Ed25519 local"
            );
        }

        System.out.println("PERSISTENCIA_SEGURA_OK");
    }
}
