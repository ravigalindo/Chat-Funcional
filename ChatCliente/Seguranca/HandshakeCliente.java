package ChatCliente.Seguranca;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;
import java.security.PublicKey;

public class HandshakeCliente {

    private final GerenciadorDH gerenciadorDH;

    private final GerenciadorHKDF gerenciadorHKDF;


    public HandshakeCliente() {

        gerenciadorDH =
                new GerenciadorDH();

        gerenciadorHKDF =
                new GerenciadorHKDF();
    }


    public KeyPair gerarParDH() {

        return gerenciadorDH
                .gerarParDeChaves();
    }


    public byte[] gerarSalt() {

        return gerenciadorHKDF
                .gerarSalt();
    }


    public String obterChavePublicaDH(
            KeyPair parDH
    ) {

        return gerenciadorDH
                .chavePublicaParaBase64(
                        parDH.getPublic()
                );
    }


    public SessaoSegura finalizarHandshake(
            KeyPair parDH,
            String chavePublicaServidorBase64,
            byte[] salt
    ) {

        PublicKey chavePublicaServidor =
                gerenciadorDH
                        .base64ParaChavePublica(
                                chavePublicaServidorBase64,
                                parDH.getPublic()
                        );

        byte[] segredoCompartilhado =
                gerenciadorDH
                        .gerarSegredoCompartilhado(
                                parDH.getPrivate(),
                                chavePublicaServidor
                        );

        GerenciadorHKDF.ChavesDerivadas chaves =
                gerenciadorHKDF
                        .derivarChaves(
                                segredoCompartilhado,
                                salt
                        );

        SecretKey chaveAES =
                new SecretKeySpec(
                        chaves.getChaveAES(),
                        "AES"
                );

        SecretKey chaveHMAC =
                new SecretKeySpec(
                        chaves.getChaveHMAC(),
                        "HmacSHA256"
                );

        return new SessaoSegura(
                parDH,
                segredoCompartilhado,
                salt,
                chaveAES,
                chaveHMAC
        );
    }
}