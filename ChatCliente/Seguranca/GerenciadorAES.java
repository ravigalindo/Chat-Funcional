package ChatCliente.Seguranca;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.SecureRandom;
import java.util.Base64;

public class GerenciadorAES {

    private static final String ALGORITMO =
            "AES";

    private static final String TRANSFORMACAO =
            "AES/GCM/NoPadding";

    private static final int TAMANHO_CHAVE =
            256;

    private static final int TAMANHO_IV =
            12;

    private static final int TAMANHO_TAG =
            128;

    /*
     * Gera uma chave AES-256 aleatória.
     */
    public SecretKey gerarChave() {

        try {

            KeyGenerator gerador =
                    KeyGenerator.getInstance(
                            ALGORITMO
                    );

            gerador.init(
                    TAMANHO_CHAVE
            );

            return gerador.generateKey();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao gerar chave AES-256.",
                    e
            );
        }
    }

    /*
     * Criptografa um texto usando AES-256
     * no modo GCM.
     *
     * O resultado contém:
     *
     * IV + texto cifrado + tag de autenticação.
     */
    public String criptografar(
            String texto,
            SecretKey chave
    ) {

        try {

            /*
             * Gera um IV novo e aleatório
             * para esta mensagem.
             */
            byte[] iv =
                    new byte[TAMANHO_IV];

            SecureRandom random =
                    new SecureRandom();

            random.nextBytes(iv);

            GCMParameterSpec especificacao =
                    new GCMParameterSpec(
                            TAMANHO_TAG,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            TRANSFORMACAO
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    chave,
                    especificacao
            );

            byte[] textoCifrado =
                    cipher.doFinal(
                            texto.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );

            /*
             * Junta IV + texto cifrado.
             */
            byte[] resultado =
                    new byte[
                            iv.length
                                    + textoCifrado.length
                    ];

            System.arraycopy(
                    iv,
                    0,
                    resultado,
                    0,
                    iv.length
            );

            System.arraycopy(
                    textoCifrado,
                    0,
                    resultado,
                    iv.length,
                    textoCifrado.length
            );

            return Base64
                    .getEncoder()
                    .encodeToString(
                            resultado
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao criptografar com AES-256.",
                    e
            );
        }
    }

    /*
     * Descriptografa um texto criptografado
     * anteriormente pelo método criptografar().
     */
    public String descriptografar(
            String textoCifrado,
            SecretKey chave
    ) {

        try {

            byte[] dados =
                    Base64
                            .getDecoder()
                            .decode(
                                    textoCifrado
                            );

            /*
             * Os primeiros 12 bytes são o IV.
             */
            byte[] iv =
                    new byte[TAMANHO_IV];

            System.arraycopy(
                    dados,
                    0,
                    iv,
                    0,
                    TAMANHO_IV
            );

            /*
             * O restante contém o texto cifrado
             * juntamente com a tag GCM.
             */
            byte[] dadosCifrados =
                    new byte[
                            dados.length
                                    - TAMANHO_IV
                    ];

            System.arraycopy(
                    dados,
                    TAMANHO_IV,
                    dadosCifrados,
                    0,
                    dadosCifrados.length
            );

            GCMParameterSpec especificacao =
                    new GCMParameterSpec(
                            TAMANHO_TAG,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            TRANSFORMACAO
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    chave,
                    especificacao
            );

            byte[] textoOriginal =
                    cipher.doFinal(
                            dadosCifrados
                    );

            return new String(
                    textoOriginal,
                    java.nio.charset.StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao descriptografar com AES-256.",
                    e
            );
        }
    }

    /*
     * Converte uma SecretKey para Base64.
     *
     * Útil posteriormente para transportar
     * uma chave quando necessário.
     */
    public String chaveParaBase64(
            SecretKey chave
    ) {

        return Base64
                .getEncoder()
                .encodeToString(
                        chave.getEncoded()
                );
    }

    /*
     * Reconstrói uma SecretKey AES a partir
     * de uma representação Base64.
     */
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