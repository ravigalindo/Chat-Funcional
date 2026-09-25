package ChatCliente.Seguranca;

import javax.crypto.SecretKey;

import java.security.KeyPair;
import java.time.Instant;

public class SessaoSegura {

    private final KeyPair parChavesDH;

    private final byte[] segredoCompartilhado;

    private final byte[] salt;

    private final SecretKey chaveAES;

    private final SecretKey chaveHMAC;

    private final Instant inicioSessao;

    private int quantidadeMensagens;


    public SessaoSegura(
            KeyPair parChavesDH,
            byte[] segredoCompartilhado,
            byte[] salt,
            SecretKey chaveAES,
            SecretKey chaveHMAC
    ) {

        this.parChavesDH =
                parChavesDH;

        this.segredoCompartilhado =
                segredoCompartilhado;

        this.salt =
                salt;

        this.chaveAES =
                chaveAES;

        this.chaveHMAC =
                chaveHMAC;

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

        Instant agora =
                Instant.now();

        long minutos =
                java.time.Duration
                        .between(
                                inicioSessao,
                                agora
                        )
                        .toMinutes();

        return minutos >= 60;
    }


    public boolean precisaRenovar() {

        return atingiuLimiteMensagens()
                || atingiuLimiteTempo();
    }
}
