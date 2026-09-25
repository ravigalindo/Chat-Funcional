package ChatCliente.Seguranca;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GerenciadorAssinatura {

    private static final String ALGORITMO =
            "Ed25519";

    public KeyPair gerarParDeChaves() {

        try {

            KeyPairGenerator gerador =
                    KeyPairGenerator.getInstance(
                            ALGORITMO
                    );

            return gerador.generateKeyPair();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao gerar par de chaves Ed25519.",
                    e
            );
        }
    }

    public String assinar(
            String mensagem,
            PrivateKey chavePrivada
    ) {

        try {

            Signature assinatura =
                    Signature.getInstance(
                            ALGORITMO
                    );

            assinatura.initSign(
                    chavePrivada
            );

            assinatura.update(
                    mensagem.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] resultado =
                    assinatura.sign();

            return Base64
                    .getEncoder()
                    .encodeToString(
                            resultado
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao assinar com Ed25519.",
                    e
            );
        }
    }

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
                    Base64
                            .getDecoder()
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

            byte[] chaveBytes =
                    Base64
                            .getDecoder()
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