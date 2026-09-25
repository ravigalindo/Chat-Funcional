package ChatCliente.Seguranca;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class SessaoE2EE {

    private final String contato;

    private final KeyPair parChavesDH;

    private final byte[] segredoCompartilhado;

    private final byte[] salt;

    private final SecretKey chaveAES;

    private final SecretKey chaveHMAC;

    private final PublicKey chavePublicaContato;

    private boolean autenticada;

    public SessaoE2EE(
            String contato,
            KeyPair parChavesDH,
            byte[] segredoCompartilhado,
            byte[] salt,
            GerenciadorHKDF.ChavesDerivadas chaves,
            PublicKey chavePublicaContato
    ) {

        this.contato = contato;
        this.parChavesDH = parChavesDH;
        this.segredoCompartilhado = segredoCompartilhado.clone();
        this.salt = salt.clone();
        this.chaveAES = new SecretKeySpec(
                chaves.getChaveAES(),
                "AES"
        );
        this.chaveHMAC = new SecretKeySpec(
                chaves.getChaveHMAC(),
                "HmacSHA256"
        );
        this.chavePublicaContato = chavePublicaContato;
        this.autenticada = false;
    }

    public String getContato() {
        return contato;
    }

    public KeyPair getParChavesDH() {
        return parChavesDH;
    }

    public byte[] getSegredoCompartilhado() {
        return segredoCompartilhado.clone();
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public SecretKey getChaveAES() {
        return chaveAES;
    }

    public SecretKey getChaveHMAC() {
        return chaveHMAC;
    }

    public PublicKey getChavePublicaContato() {
        return chavePublicaContato;
    }

    public boolean estaAutenticada() {
        return autenticada;
    }

    public void marcarComoAutenticada() {
        autenticada = true;
    }

    public String getSaltBase64() {
        return Base64.getEncoder().encodeToString(salt);
    }

        public String criptografar(
            String texto
        ) {

        GerenciadorAES aes =
            new GerenciadorAES();

        GerenciadorHMAC hmac =
            new GerenciadorHMAC();

        String ciphertext =
            aes.criptografar(
                texto,
                chaveAES
            );

        String mac =
            hmac.gerarHMAC(
                ciphertext,
                chaveHMAC
            );

        return ciphertext + "|" + mac;
        }

        public String descriptografar(
            String pacote
        ) {

        String[] partes =
            pacote.split(
                "\\|",
                2
            );

        if (partes.length < 2) {
            return null;
        }

        GerenciadorHMAC hmac =
            new GerenciadorHMAC();

        if (!hmac.verificarHMAC(
            partes[0],
            partes[1],
            chaveHMAC
        )) {
            return null;
        }

        return new GerenciadorAES()
            .descriptografar(
                partes[0],
                chaveAES
            );
        }
}
