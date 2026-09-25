package ChatCliente.Seguranca;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;

public class HandshakeE2EEPendente {

    private final KeyPair parChavesDH;

    private final byte[] salt;

    public HandshakeE2EEPendente(
            KeyPair parChavesDH,
            byte[] salt
    ) {
        this.parChavesDH = parChavesDH;
        this.salt = salt.clone();
    }

    public KeyPair getParChavesDH() {
        return parChavesDH;
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public String getSaltBase64() {
        return Base64.getEncoder().encodeToString(salt);
    }

    public boolean possuiSalt(byte[] outroSalt) {
        return Arrays.equals(salt, outroSalt);
    }
}
