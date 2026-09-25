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

        return cadastrarUsuario(
                nome,
                senha,
                null
        );
    }

    public boolean cadastrarUsuario(
            String nome,
            String senha,
            String chavePublica
    ) {

        String senhaHash =
                gerenciadorSenhas.gerarHash(
                        senha
                );

        String sql =
                """
                INSERT INTO usuarios (
                    nome,
                    senha,
                    senha_hash,
                    chave_publica
                )
                VALUES (?, NULL, ?, ?)
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

            statement.setString(
                    3,
                    chavePublica
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

                if (senhaHash != null) {

                    return gerenciadorSenhas.verificarSenha(
                            senha,
                            senhaHash
                    );
                }

                if (senhaAntiga != null) {

                    boolean senhaCorreta =
                            senhaAntiga.equals(senha);

                    if (!senhaCorreta) {
                        return false;
                    }

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

    public String obterChavePublica(
            String nome
    ) {

        String sql =
                """
                SELECT chave_publica
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
                    return null;
                }

                return resultado.getString(
                        "chave_publica"
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao obter chave pública:"
            );

            e.printStackTrace();

            return null;
        }
    }

    public boolean atualizarChavePublica(
            String nome,
            String chavePublica
    ) {

        String sql =
                """
                UPDATE usuarios
                SET chave_publica = ?
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
                    chavePublica
            );

            statement.setString(
                    2,
                    nome
            );

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao atualizar chave pública:"
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
}