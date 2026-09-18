package ChatServidor.Segurança;

import com.password4j.Password;

public class GerenciadorSenhas {

    public String gerarHash(
            String senha
    ) {

        return Password
                .hash(senha)
                .addRandomSalt()
                .withArgon2()
                .getResult();
    }

    public boolean verificarSenha(
            String senha,
            String hash
    ) {

        return Password
                .check(
                        senha,
                        hash
                )
                .withArgon2();
    }
}