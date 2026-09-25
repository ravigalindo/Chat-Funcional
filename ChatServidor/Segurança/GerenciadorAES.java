package ChatServidor.Segurança;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class GerenciadorAES {

    private static final String ALGORITMO =
            "AES";

    private static final String TRANSFORMACAO =
            "AES/GCM/NoPadding";

    private static final int TAMANHO_IV =
            12;

    private static final int TAMANHO_TAG =
            128;

    public String criptografar(
            String texto,
            byte[] chave
    ) {

        try {

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

            SecretKeySpec chaveAES =
                    new SecretKeySpec(
                            chave,
                            ALGORITMO
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    chaveAES,
                    especificacao
            );

            byte[] textoCifrado =
                    cipher.doFinal(
                            texto.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

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

    public String descriptografar(
            String textoCifrado,
            byte[] chave
    ) {

        try {

            byte[] dados =
                    Base64
                            .getDecoder()
                            .decode(
                                    textoCifrado
                            );

            byte[] iv =
                    new byte[TAMANHO_IV];

            System.arraycopy(
                    dados,
                    0,
                    iv,
                    0,
                    TAMANHO_IV
            );

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

            SecretKeySpec chaveAES =
                    new SecretKeySpec(
                            chave,
                            ALGORITMO
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    chaveAES,
                    especificacao
            );

            byte[] textoOriginal =
                    cipher.doFinal(
                            dadosCifrados
                    );

            return new String(
                    textoOriginal,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao descriptografar com AES-256.",
                    e
            );
        }
    }
}