package ChatCliente.Seguranca;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

public class GerenciadorDH {

    private static final String ALGORITMO =
            "DH";

    private static final int TAMANHO_CHAVE =
            2048;

    /**
     * Gera um par de chaves Diffie-Hellman.
     *
     * O par contém:
     * - chave privada
     * - chave pública
     *
     * As chaves são efêmeras e devem ser usadas
     * para uma sessão específica.
     */
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

    /**
     * Calcula o segredo compartilhado usando:
     *
     * chave privada deste participante
     * +
     * chave pública do outro participante.
     */
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

    /**
     * Converte uma chave pública para Base64.
     *
     * Será útil posteriormente para transmitir
     * a chave pública pela rede.
     */
    public String chavePublicaParaBase64(
            PublicKey chavePublica
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        chavePublica.getEncoded()
                );
    }

    /**
     * Reconstrói uma chave pública DH a partir
     * de seus bytes codificados.
     */
    public PublicKey base64ParaChavePublica(
            String chaveBase64,
            PublicKey chavePublicaLocal
    ) {

        try {

            byte[] bytes =
                    Base64
                            .getDecoder()
                            .decode(
                                    chaveBase64
                            );

            java.security.spec.X509EncodedKeySpec especificacao =
                    new java.security.spec.X509EncodedKeySpec(
                            bytes
                    );

            java.security.KeyFactory fabrica =
                    java.security.KeyFactory.getInstance(
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

    /**
     * Retorna a chave pública DH de um KeyPair.
     */
    public PublicKey getChavePublica(
            KeyPair par
    ) {

        return par.getPublic();
    }

    /**
     * Retorna a chave privada DH de um KeyPair.
     */
    public PrivateKey getChavePrivada(
            KeyPair par
    ) {

        return par.getPrivate();
    }

    /**
     * Converte bytes para Base64.
     */
    public String bytesParaBase64(
            byte[] dados
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        dados
                );
    }
}