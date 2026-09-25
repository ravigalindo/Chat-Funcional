package ChatServidor.Segurança;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GerenciadorAssinatura {

    private static final String ALGORITMO =
            "Ed25519";

    public boolean verificar(
            String mensagem,
            String assinaturaBase64,
            PublicKey chavePublica
    ) {

        try {

            Signature assinatura =
                    Signature.getInstance(
                            ALGORITMO
                    );

            assinatura.initVerify(
                    chavePublica
            );

            assinatura.update(
                    mensagem.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] assinaturaBytes =
                    Base64.getDecoder()
                            .decode(
                                    assinaturaBase64
                            );

            return assinatura.verify(
                    assinaturaBytes
            );

        } catch (Exception e) {

            return false;
        }
    }

    public PublicKey base64ParaChavePublica(
            String chaveBase64
    ) {

        try {

            byte[] chaveBytes =
                    Base64.getDecoder()
                            .decode(
                                    chaveBase64
                            );

            X509EncodedKeySpec especificacao =
                    new X509EncodedKeySpec(
                            chaveBytes
                    );

            KeyFactory fabrica =
                    KeyFactory.getInstance(
                            ALGORITMO
                    );

            return fabrica.generatePublic(
                    especificacao
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao reconstruir chave pública Ed25519.",
                    e
            );
        }
    }
}