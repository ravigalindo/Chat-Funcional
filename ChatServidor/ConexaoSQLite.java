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

        try (
                Connection conexao = conectar();
                Statement statement = conexao.createStatement()
        ) {

            statement.execute(sqlUsuarios);

            System.out.println(
                    "Tabela usuarios criada/verificada com sucesso!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao criar tabela usuarios:"
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