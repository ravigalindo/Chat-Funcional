package ChatCliente;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ChaveAssinaturaLocal {

    private static final Path DIRETORIO =
            Path.of("chaves");

    private static final int ITERACOES = 120000;

    public void salvar(
            String usuario,
            String senha,
            KeyPair par
    ) {

        try {
            Files.createDirectories(DIRETORIO);

            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);

            SecretKeySpec chave = derivarChave(senha, salt);
            String conteudo = Base64.getEncoder().encodeToString(
                    par.getPublic().getEncoded()
            ) + "|" + Base64.getEncoder().encodeToString(
                    par.getPrivate().getEncoded()
            );

            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(
                    Cipher.ENCRYPT_MODE,
                    chave,
                    new GCMParameterSpec(128, iv)
            );

            byte[] cifrado = cifra.doFinal(
                    conteudo.getBytes(StandardCharsets.UTF_8)
            );

            String pacote = Base64.getEncoder().encodeToString(salt)
                    + "|"
                    + Base64.getEncoder().encodeToString(iv)
                    + "|"
                    + Base64.getEncoder().encodeToString(cifrado);

            Files.writeString(
                    caminho(usuario),
                    pacote,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não foi possível salvar a chave local.",
                    e
            );
        }
    }

    public KeyPair carregar(
            String usuario,
            String senha
    ) {

        try {
            if (!Files.exists(caminho(usuario))) {
                return null;
            }

            String[] partes = Files.readString(
                    caminho(usuario),
                    StandardCharsets.UTF_8
            ).split("\\|", 3);

            if (partes.length < 3) {
                return null;
            }

            byte[] salt = Base64.getDecoder().decode(partes[0]);
            byte[] iv = Base64.getDecoder().decode(partes[1]);
            byte[] cifrado = Base64.getDecoder().decode(partes[2]);

            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(
                    Cipher.DECRYPT_MODE,
                    derivarChave(senha, salt),
                    new GCMParameterSpec(128, iv)
            );

            String conteudo = new String(
                    cifra.doFinal(cifrado),
                    StandardCharsets.UTF_8
            );

            String[] chaves = conteudo.split("\\|", 2);
            KeyFactory fabrica = KeyFactory.getInstance("Ed25519");

            PublicKey publica = fabrica.generatePublic(
                    new X509EncodedKeySpec(
                            Base64.getDecoder().decode(chaves[0])
                    )
            );

            PrivateKey privada = fabrica.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            Base64.getDecoder().decode(chaves[1])
                    )
            );

            return new KeyPair(publica, privada);

        } catch (Exception e) {
            System.out.println(
                    "Chave local não pôde ser carregada: "
                            + e.getMessage()
            );
            return null;
        }
    }

    private SecretKeySpec derivarChave(
            String senha,
            byte[] salt
    ) throws Exception {

        byte[] bytes = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(
                        new PBEKeySpec(
                                senha.toCharArray(),
                                salt,
                                ITERACOES,
                                256
                        )
                )
                .getEncoded();

        return new SecretKeySpec(bytes, "AES");
    }

    private Path caminho(String usuario) {
        return DIRETORIO.resolve(
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                usuario.getBytes(StandardCharsets.UTF_8)
                        )
                        + ".key"
        );
    }
}
