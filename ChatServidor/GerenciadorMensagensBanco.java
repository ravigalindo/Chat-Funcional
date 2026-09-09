package ChatServidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GerenciadorMensagensBanco {

    public void listarHistorico(
            String usuario1,
            String usuario2
    ) {

        String sql =
                """
                SELECT
                    remetente,
                    destinatario,
                    conteudo,
                    data_hora
                FROM mensagens
                WHERE
                    (remetente = ? AND destinatario = ?)
                    OR
                    (remetente = ? AND destinatario = ?)
                ORDER BY data_hora ASC, id ASC
                """;

        try (
                Connection conexao =
                        ConexaoSQLite.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    usuario1
            );

            statement.setString(
                    2,
                    usuario2
            );

            statement.setString(
                    3,
                    usuario2
            );

            statement.setString(
                    4,
                    usuario1
            );

            try (
                    ResultSet resultado =
                            statement.executeQuery()
            ) {

                System.out.println(
                        "===== HISTÓRICO ====="
                );

                boolean encontrou =
                        false;

                while (
                        resultado.next()
                ) {

                    encontrou = true;

                    String remetente =
                            resultado.getString(
                                    "remetente"
                            );

                    String destinatario =
                            resultado.getString(
                                    "destinatario"
                            );

                    String conteudo =
                            resultado.getString(
                                    "conteudo"
                            );

                    String dataHora =
                            resultado.getString(
                                    "data_hora"
                            );

                    System.out.println(
                            dataHora
                                    + " | "
                                    + remetente
                                    + " -> "
                                    + destinatario
                                    + " | "
                                    + conteudo
                    );
                }

                if (!encontrou) {

                    System.out.println(
                            "Nenhuma mensagem encontrada."
                    );
                }

                System.out.println(
                        "====================="
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao buscar histórico:"
            );

            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        GerenciadorMensagensBanco gerenciador =
                new GerenciadorMensagensBanco();

        gerenciador.listarHistorico(
                "Testador1",
                "Zabuza"
        );
    }
}