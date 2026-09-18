package ChatCliente.Seguranca;

import javax.crypto.SecretKey;

public class TesteAES {

    public static void main(String[] args) {

        GerenciadorAES gerenciador =
                new GerenciadorAES();

        String mensagem =
                "Olá! Esta mensagem será criptografada.";

        System.out.println(
                "Mensagem original:"
        );

        System.out.println(
                mensagem
        );

        SecretKey chave =
                gerenciador.gerarChave();

        System.out.println(
                "\nChave AES-256:"
        );

        System.out.println(
                gerenciador.chaveParaBase64(
                        chave
                )
        );

        String mensagemCriptografada =
                gerenciador.criptografar(
                        mensagem,
                        chave
                );

        System.out.println(
                "\nMensagem criptografada:"
        );

        System.out.println(
                mensagemCriptografada
        );

        String mensagemDescriptografada =
                gerenciador.descriptografar(
                        mensagemCriptografada,
                        chave
                );

        System.out.println(
                "\nMensagem descriptografada:"
        );

        System.out.println(
                mensagemDescriptografada
        );

        System.out.println(
                "\nMensagem original == "
                        + "mensagem descriptografada:"
        );

        System.out.println(
                mensagem.equals(
                        mensagemDescriptografada
                )
        );
    }
}