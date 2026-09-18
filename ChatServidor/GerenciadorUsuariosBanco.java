package ChatServidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ChatServidor.Segurança.GerenciadorSenhas;

public class GerenciadorUsuariosBanco {

    private final GerenciadorSenhas gerenciadorSenhas =
            new GerenciadorSenhas();

    public boolean cadastrarUsuario(
            String nome,
            String senha
    ) {

        /*
         * A senha nunca é armazenada diretamente.
         *
         * O Password4j gera o hash Argon2
         * utilizando um salt aleatório.
         */
        String senhaHash =
                gerenciadorSenhas.gerarHash(
                        senha
                );

        String sql =
                """
                INSERT INTO usuarios (
                    nome,
                    senha,
                    senha_hash
                )
                VALUES (?, NULL, ?)
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    nome
            );

            statement.setString(
                    2,
                    senhaHash
            );

            statement.executeUpdate();

            return true;

        } catch (SQLException e) {

            if (
                    e.getMessage() != null
                            && e.getMessage()
                                    .contains(
                                            "UNIQUE constraint failed"
                                    )
            ) {

                return false;
            }

            System.out.println(
                    "Erro ao cadastrar usuário:"
            );

            e.printStackTrace();

            return false;
        }
    }

    public boolean autenticar(
        String nome,
        String senha
) {

    String sql =
            """
            SELECT id, senha, senha_hash
            FROM usuarios
            WHERE nome = ?
            """;

    try (
            Connection conexao =
                    ConexaoSQLite.conectar();

            PreparedStatement statement =
                    conexao.prepareStatement(sql)
    ) {

        statement.setString(
                1,
                nome
        );

        try (
                ResultSet resultado =
                        statement.executeQuery()
        ) {

            if (!resultado.next()) {
                return false;
            }

            int id =
                    resultado.getInt("id");

            String senhaAntiga =
                    resultado.getString("senha");

            String senhaHash =
                    resultado.getString("senha_hash");

            /*
             * Usuário que já possui hash Argon2.
             */
            if (senhaHash != null) {

                return gerenciadorSenhas.verificarSenha(
                        senha,
                        senhaHash
                );
            }

            /*
             * Usuário antigo que ainda não foi migrado.
             */
            if (senhaAntiga != null) {

                boolean senhaCorreta =
                        senhaAntiga.equals(senha);

                if (!senhaCorreta) {
                    return false;
                }

                /*
                 * A senha antiga está correta.
                 *
                 * Agora geramos o hash Argon2 e
                 * removemos a senha em texto.
                 */
                String novoHash =
                        gerenciadorSenhas.gerarHash(
                                senha
                        );

                String sqlAtualizar =
                        """
                        UPDATE usuarios
                        SET senha = NULL,
                            senha_hash = ?
                        WHERE id = ?
                        """;

                try (
                        PreparedStatement atualizar =
                                conexao.prepareStatement(
                                        sqlAtualizar
                                )
                ) {

                    atualizar.setString(
                            1,
                            novoHash
                    );

                    atualizar.setInt(
                            2,
                            id
                    );

                    atualizar.executeUpdate();
                }

                System.out.println(
                        "Usuário migrado para Argon2: "
                                + nome
                );

                return true;
            }

            return false;
        }

    } catch (SQLException e) {

        System.out.println(
                "Erro ao autenticar usuário:"
        );

        e.printStackTrace();

        return false;
    }
}

    public boolean usuarioExiste(
            String nome
    ) {

        String sql =
                "SELECT id FROM usuarios WHERE nome = ?";

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    nome
            );

            try (
                    ResultSet resultado =
                            statement.executeQuery()
            ) {

                return resultado.next();
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao verificar usuário:"
            );

            e.printStackTrace();

            return false;
        }
    }

    /*
     * Retorna todos os usuários cadastrados
     * no banco de dados.
     */
    public List<String> listarUsuarios() {

        List<String> usuarios =
                new ArrayList<>();

        String sql =
                """
                SELECT nome
                FROM usuarios
                ORDER BY nome ASC
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql);

                ResultSet resultado =
                        statement.executeQuery()
        ) {

            while (resultado.next()) {

                usuarios.add(
                        resultado.getString("nome")
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao listar usuários:"
            );

            e.printStackTrace();
        }

        return usuarios;
    }

    public static void main(String[] args) {

        GerenciadorUsuariosBanco gerenciador =
                new GerenciadorUsuariosBanco();

        String nomeTeste =
                "teste_argon2";

        String senhaTeste =
                "123456";

        boolean cadastrado =
                gerenciador.cadastrarUsuario(
                        nomeTeste,
                        senhaTeste
                );

        System.out.println(
                "Cadastro realizado: "
                        + cadastrado
        );

        boolean autenticado =
                gerenciador.autenticar(
                        nomeTeste,
                        senhaTeste
                );

        System.out.println(
                "Autenticação correta: "
                        + autenticado
        );

        boolean senhaErrada =
                gerenciador.autenticar(
                        nomeTeste,
                        "senhaerrada"
                );

        System.out.println(
                "Autenticação com senha errada: "
                        + senhaErrada
        );

        System.out.println(
                "\nUsuários cadastrados:"
        );

        List<String> usuarios =
                gerenciador.listarUsuarios();

        for (String usuario :
                usuarios) {

            System.out.println(
                    "- " + usuario
            );
        }
    }
}