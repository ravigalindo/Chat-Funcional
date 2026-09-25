package ChatCliente.Seguranca;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class GerenciadorHKDF {

    private static final String ALGORITMO =
            "HmacSHA256";

    private static final int TAMANHO_HASH =
            32;

    private static final int TAMANHO_CHAVE_AES =
            32;

    private static final int TAMANHO_CHAVE_HMAC =
            32;

    private static final int TAMANHO_CHAVES =
            TAMANHO_CHAVE_AES
                    + TAMANHO_CHAVE_HMAC;

    public byte[] gerarSalt() {

        byte[] salt =
                new byte[TAMANHO_HASH];

        SecureRandom random =
                new SecureRandom();

        random.nextBytes(salt);

        return salt;
    }

    public byte[] extrair(
            byte[] segredo,
            byte[] salt
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            ALGORITMO
                    );

            SecretKeySpec chaveSalt =
                    new SecretKeySpec(
                            salt,
                            ALGORITMO
                    );

            mac.init(chaveSalt);

            return mac.doFinal(
                    segredo
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro na etapa Extract do HKDF-SHA-256.",
                    e
            );
        }
    }

    public byte[] expandir(
            byte[] prk,
            byte[] info,
            int tamanho
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            ALGORITMO
                    );

            SecretKeySpec chave =
                    new SecretKeySpec(
                            prk,
                            ALGORITMO
                    );

            mac.init(chave);

            byte[] resultado =
                    new byte[tamanho];

            byte[] blocoAnterior =
                    new byte[0];

            int posicao =
                    0;

            int contador =
                    1;

            while (
                    posicao < tamanho
            ) {

                mac.reset();

                mac.update(
                        blocoAnterior
                );

                if (
                        info != null
                                && info.length > 0
                ) {

                    mac.update(
                            info
                    );
                }

                mac.update(
                        (byte) contador
                );

                blocoAnterior =
                        mac.doFinal();

                int quantidade =
                        Math.min(
                                blocoAnterior.length,
                                tamanho - posicao
                        );

                System.arraycopy(
                        blocoAnterior,
                        0,
                        resultado,
                        posicao,
                        quantidade
                );

                posicao += quantidade;

                contador++;
            }

            return resultado;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro na etapa Expand do HKDF-SHA-256.",
                    e
            );
        }
    }

    public ChavesDerivadas derivarChaves(
            byte[] segredoCompartilhado,
            byte[] salt
    ) {

        byte[] prk =
                extrair(
                        segredoCompartilhado,
                        salt
                );

        byte[] info =
                "Chat-Funcional-Projeto-2"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        byte[] chaves =
                expandir(
                        prk,
                        info,
                        TAMANHO_CHAVES
                );

        byte[] chaveAES =
                Arrays.copyOfRange(
                        chaves,
                        0,
                        TAMANHO_CHAVE_AES
                );

        byte[] chaveHMAC =
                Arrays.copyOfRange(
                        chaves,
                        TAMANHO_CHAVE_AES,
                        TAMANHO_CHAVES
                );

        return new ChavesDerivadas(
                chaveAES,
                chaveHMAC
        );
    }

    public String paraBase64(
            byte[] dados
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        dados
                );
    }

    public byte[] deBase64(
            String dados
    ) {

        return Base64
                .getDecoder()
                .decode(
                        dados
                );
    }

    public static class ChavesDerivadas {

        private final byte[] chaveAES;

        private final byte[] chaveHMAC;

        public ChavesDerivadas(
                byte[] chaveAES,
                byte[] chaveHMAC
        ) {

            this.chaveAES =
                    chaveAES;

            this.chaveHMAC =
                    chaveHMAC;
        }

        public byte[] getChaveAES() {

            return chaveAES;
        }

        public byte[] getChaveHMAC() {

            return chaveHMAC;
        }
    }
}