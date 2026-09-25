package ChatServidor.Segurança;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class GerenciadorHMAC {

    private static final String ALGORITMO =
            "HmacSHA256";

    public String gerarHMAC(
            String texto,
            byte[] chave
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            ALGORITMO
                    );

            SecretKeySpec chaveHMAC =
                    new SecretKeySpec(
                            chave,
                            ALGORITMO
                    );

            mac.init(
                    chaveHMAC
            );

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
            byte[] chave
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
}