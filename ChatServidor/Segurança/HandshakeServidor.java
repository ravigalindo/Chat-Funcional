package ChatServidor.Segurança;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class HandshakeServidor {

    private final GerenciadorDH gerenciadorDH;

    private final GerenciadorHKDF gerenciadorHKDF;


    public HandshakeServidor() {

        gerenciadorDH =
                new GerenciadorDH();

        gerenciadorHKDF =
                new GerenciadorHKDF();
    }


    public KeyPair gerarParDH() {

        return gerenciadorDH
                .gerarParDeChaves();
    }


    public String obterChavePublicaDH(
            KeyPair parDH
    ) {

        return gerenciadorDH
                .chavePublicaParaBase64(
                        parDH.getPublic()
                );
    }


    public SessaoSeguraServidor finalizarHandshake(
            KeyPair parDH,
            String chavePublicaClienteBase64,
            String saltBase64
    ) {

        PublicKey chavePublicaCliente =
                gerenciadorDH
                        .base64ParaChavePublica(
                                chavePublicaClienteBase64
                        );

        byte[] salt =
                Base64
                        .getDecoder()
                        .decode(
                                saltBase64
                        );

        byte[] segredoCompartilhado =
                gerenciadorDH
                        .gerarSegredoCompartilhado(
                                parDH.getPrivate(),
                                chavePublicaCliente
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

        return new SessaoSeguraServidor(
                parDH,
                segredoCompartilhado,
                salt,
                chaveAES.getEncoded(),
                chaveHMAC.getEncoded()
        );
    }
}