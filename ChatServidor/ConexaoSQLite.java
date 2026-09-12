package ChatServidor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoSQLite {

    private static final String CAMINHO_BANCO =
            "banco/chat.db";

    public static Connection conectar()
            throws SQLException {

        File pastaBanco =
                new File("banco");

        if (!pastaBanco.exists()) {
            pastaBanco.mkdirs();
        }

        return DriverManager.getConnection(
                "jdbc:sqlite:" + CAMINHO_BANCO
        );
    }

    public static void criarTabelas() {

        String sqlUsuarios =
                """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL UNIQUE,
                    senha TEXT NOT NULL
                )
                """;

        String sqlMensagens =
                """
                CREATE TABLE IF NOT EXISTS mensagens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    remetente TEXT NOT NULL,
                    destinatario TEXT NOT NULL,
                    conteudo TEXT NOT NULL,
                    data_hora TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    apagada_remetente INTEGER NOT NULL DEFAULT 0,
                    apagada_destinatario INTEGER NOT NULL DEFAULT 0,
                    entregue INTEGER NOT NULL DEFAULT 0
                )
                """;

        try (
                Connection conexao =
                        conectar();

                Statement statement =
                        conexao.createStatement()
        ) {

            statement.execute(sqlUsuarios);

            statement.execute(sqlMensagens);

            /*
             * O banco pode já existir de uma execução anterior.
             *
             * Nesse caso, a tabela mensagens já existe e o
             * CREATE TABLE IF NOT EXISTS não altera sua estrutura.
             *
             * Por isso verificamos se a coluna entregue existe.
             */
            try (
                    var resultado =
                            statement.executeQuery(
                                    "PRAGMA table_info(mensagens)"
                            )
            ) {

                boolean colunaEntregueExiste = false;

                while (resultado.next()) {

                    String nomeColuna =
                            resultado.getString("name");

                    if ("entregue".equalsIgnoreCase(
                            nomeColuna
                    )) {

                        colunaEntregueExiste = true;

                        break;
                    }
                }

                if (!colunaEntregueExiste) {

                    statement.executeUpdate(
                            """
                            ALTER TABLE mensagens
                            ADD COLUMN entregue
                            INTEGER NOT NULL DEFAULT 0
                            """
                    );

                    System.out.println(
                            "Coluna 'entregue' adicionada à tabela mensagens."
                    );
                }
            }

            System.out.println(
                    "Tabelas criadas/verificadas com sucesso!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao criar/verificar tabelas:"
            );

            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {

            Connection conexao =
                    conectar();

            System.out.println(
                    "Conexão com SQLite realizada com sucesso!"
            );

            conexao.close();

            criarTabelas();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao conectar com SQLite:"
            );

            e.printStackTrace();
        }
    }
}