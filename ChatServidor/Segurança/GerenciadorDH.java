package ChatServidor.Segurança;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.KeyAgreement;

public class GerenciadorDH {

    private static final String ALGORITMO =
            "DH";

    private static final int TAMANHO_CHAVE =
            2048;


    public KeyPair gerarParDeChaves() {

        try {

            KeyPairGenerator gerador =
                    KeyPairGenerator.getInstance(
                            ALGORITMO
                    );

            gerador.initialize(
                    TAMANHO_CHAVE,
                    new SecureRandom()
            );

            return gerador.generateKeyPair();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao gerar par de chaves Diffie-Hellman.",
                    e
            );
        }
    }


    public byte[] gerarSegredoCompartilhado(
            PrivateKey chavePrivada,
            PublicKey chavePublicaOutro
    ) {

        try {

            KeyAgreement acordo =
                    KeyAgreement.getInstance(
                            ALGORITMO
                    );

            acordo.init(
                    chavePrivada
            );

            acordo.doPhase(
                    chavePublicaOutro,
                    true
            );

            return acordo.generateSecret();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao gerar segredo compartilhado Diffie-Hellman.",
                    e
            );
        }
    }


    public String chavePublicaParaBase64(
            PublicKey chavePublica
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        chavePublica.getEncoded()
                );
    }


    public PublicKey base64ParaChavePublica(
            String chaveBase64
    ) {

        try {

            byte[] bytes =
                    Base64
                            .getDecoder()
                            .decode(
                                    chaveBase64
                            );

            X509EncodedKeySpec especificacao =
                    new X509EncodedKeySpec(
                            bytes
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
                    "Erro ao reconstruir chave pública Diffie-Hellman.",
                    e
            );
        }
    }
}