package ChatServidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GerenciadorUsuariosBanco {

    public boolean cadastrarUsuario(
            String nome,
            String senha
    ) {

        String sql =
                "INSERT INTO usuarios (nome, senha) VALUES (?, ?)";

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(1, nome);
            statement.setString(2, senha);

            statement.executeUpdate();

            return true;

        } catch (SQLException e) {

            if (
                    e.getMessage() != null
                            && e.getMessage()
                                    .contains("UNIQUE constraint failed")
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
                "SELECT id FROM usuarios WHERE nome = ? AND senha = ?";

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(1, nome);
            statement.setString(2, senha);

            try (
                    ResultSet resultado =
                            statement.executeQuery()
            ) {

                return resultado.next();
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

            statement.setString(1, nome);

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

    public static void main(String[] args) {

    GerenciadorUsuariosBanco gerenciador =
            new GerenciadorUsuariosBanco();

    boolean cadastrado =
            gerenciador.cadastrarUsuario(
                    "teste",
                    "123456"
            );

    System.out.println(
            "Cadastro realizado: "
                    + cadastrado
    );

    boolean autenticado =
            gerenciador.autenticar(
                    "teste",
                    "123456"
            );

    System.out.println(
            "Autenticação correta: "
                    + autenticado
    );

    boolean senhaErrada =
            gerenciador.autenticar(
                    "teste",
                    "senhaerrada"
            );

    System.out.println(
            "Autenticação com senha errada: "
                    + senhaErrada
    );
    }
}