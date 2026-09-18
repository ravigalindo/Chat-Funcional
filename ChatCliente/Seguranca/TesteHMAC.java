package ChatCliente.Seguranca;

import javax.crypto.SecretKey;

public class TesteHMAC {

    public static void main(String[] args) {

        GerenciadorHMAC gerenciador =
                new GerenciadorHMAC();

        String mensagem =
                "Esta mensagem precisa ser protegida.";

        System.out.println(
                "Mensagem:"
        );

        System.out.println(
                mensagem
        );

        SecretKey chave =
                gerenciador.gerarChave();

        System.out.println(
                "\nChave HMAC:"
        );

        System.out.println(
                gerenciador.chaveParaBase64(
                        chave
                )
        );

        String hmac =
                gerenciador.gerarHMAC(
                        mensagem,
                        chave
                );

        System.out.println(
                "\nHMAC-SHA-256:"
        );

        System.out.println(
                hmac
        );

        boolean mensagemValida =
                gerenciador.verificarHMAC(
                        mensagem,
                        hmac,
                        chave
                );

        System.out.println(
                "\nMensagem original:"
        );

        System.out.println(
                "HMAC válido: "
                        + mensagemValida
        );

        /*
         * Simula uma alteração na mensagem.
         */
        String mensagemAlterada =
                "Esta mensagem foi alterada.";

        boolean mensagemAlteradaValida =
                gerenciador.verificarHMAC(
                        mensagemAlterada,
                        hmac,
                        chave
                );

        System.out.println(
                "\nMensagem alterada:"
        );

        System.out.println(
                "HMAC válido: "
                        + mensagemAlteradaValida
        );
    }
}