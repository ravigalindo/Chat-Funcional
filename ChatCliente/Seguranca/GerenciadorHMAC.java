package ChatCliente.Seguranca;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class GerenciadorHMAC {

    private static final String ALGORITMO =
            "HmacSHA256";

    private static final int TAMANHO_CHAVE =
            256;

    public SecretKey gerarChave() {

        byte[] chaveBytes =
                new byte[TAMANHO_CHAVE / 8];

        SecureRandom random =
                new SecureRandom();

        random.nextBytes(chaveBytes);

        return new SecretKeySpec(
                chaveBytes,
                ALGORITMO
        );
    }

    public String gerarHMAC(
            String texto,
            SecretKey chave
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            ALGORITMO
                    );

            mac.init(chave);

            byte[] resultado =
                    mac.doFinal(
                            texto.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64
                    .getEncoder()
                    .encodeToString(
                            resultado
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao gerar HMAC-SHA-256.",
                    e
            );
        }
    }

    public boolean verificarHMAC(
            String texto,
            String hmacRecebido,
            SecretKey chave
    ) {

        try {

            String hmacCalculado =
                    gerarHMAC(
                            texto,
                            chave
                    );

            byte[] esperado =
                    Base64
                            .getDecoder()
                            .decode(
                                    hmacCalculado
                            );

            byte[] recebido =
                    Base64
                            .getDecoder()
                            .decode(
                                    hmacRecebido
                            );

            return MessageDigest
                    .isEqual(
                            esperado,
                            recebido
                    );

        } catch (Exception e) {

            return false;
        }
    }

    public String chaveParaBase64(
            SecretKey chave
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        chave.getEncoded()
                );
    }

    public SecretKey base64ParaChave(
            String chaveBase64
    ) {

        byte[] chaveBytes =
                Base64
                        .getDecoder()
                        .decode(
                                chaveBase64
                        );

        return new SecretKeySpec(
                chaveBytes,
                ALGORITMO
        );
    }
}