package ChatServidor.Segurança;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

public class SessaoSeguraServidor {

    private final KeyPair parChavesDH;

    private final byte[] segredoCompartilhado;

    private final byte[] salt;

    private final SecretKey chaveAES;

    private final SecretKey chaveHMAC;

    private final Instant inicioSessao;

    private int quantidadeMensagens;


    public SessaoSeguraServidor(
            KeyPair parChavesDH,
            byte[] segredoCompartilhado,
            byte[] salt,
            byte[] chaveAES,
            byte[] chaveHMAC
    ) {

        this.parChavesDH =
                parChavesDH;

        this.segredoCompartilhado =
                segredoCompartilhado;

        this.salt =
                salt;

        this.chaveAES =
                new SecretKeySpec(
                        chaveAES,
                        "AES"
                );

        this.chaveHMAC =
                new SecretKeySpec(
                        chaveHMAC,
                        "HmacSHA256"
                );

        this.inicioSessao =
                Instant.now();

        this.quantidadeMensagens =
                0;
    }


    public KeyPair getParChavesDH() {

        return parChavesDH;
    }


    public byte[] getSegredoCompartilhado() {

        return segredoCompartilhado;
    }


    public byte[] getSalt() {

        return salt;
    }


    public SecretKey getChaveAES() {

        return chaveAES;
    }


    public SecretKey getChaveHMAC() {

        return chaveHMAC;
    }


    public Instant getInicioSessao() {

        return inicioSessao;
    }


    public int getQuantidadeMensagens() {

        return quantidadeMensagens;
    }


    public void registrarMensagem() {

        quantidadeMensagens++;
    }


    public boolean atingiuLimiteMensagens() {

        return quantidadeMensagens >= 100;
    }


    public boolean atingiuLimiteTempo() {

        long minutos =
                Duration
                        .between(
                                inicioSessao,
                                Instant.now()
                        )
                        .toMinutes();

        return minutos >= 60;
    }


    public boolean precisaRenovar() {

        return atingiuLimiteMensagens()
                || atingiuLimiteTempo();
    }
}