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
                    senha TEXT,
                    senha_hash TEXT,
                    chave_publica TEXT
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
                    entregue INTEGER NOT NULL DEFAULT 0,
                    lida INTEGER NOT NULL DEFAULT 0
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

            migrarTabelaUsuarios(statement);
            migrarTabelaMensagens(statement);

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

    private static void migrarTabelaUsuarios(
            Statement statement
    ) throws SQLException {

        boolean colunaSenhaExiste = false;
        boolean colunaSenhaHashExiste = false;
        boolean colunaChavePublicaExiste = false;

        try (
                var resultado =
                        statement.executeQuery(
                                "PRAGMA table_info(usuarios)"
                        )
        ) {

            while (resultado.next()) {

                String nomeColuna =
                        resultado.getString("name");

                if ("senha".equalsIgnoreCase(
                        nomeColuna
                )) {

                    colunaSenhaExiste = true;
                }

                if ("senha_hash".equalsIgnoreCase(
                        nomeColuna
                )) {

                    colunaSenhaHashExiste = true;
                }

                if ("chave_publica".equalsIgnoreCase(
                        nomeColuna
                )) {

                    colunaChavePublicaExiste = true;
                }
            }
        }

        if (!colunaSenhaHashExiste) {

            statement.executeUpdate(
                    """
                    ALTER TABLE usuarios
                    ADD COLUMN senha_hash TEXT
                    """
            );

            System.out.println(
                    "Coluna 'senha_hash' adicionada à tabela usuarios."
            );
        }

        if (!colunaChavePublicaExiste) {

            statement.executeUpdate(
                    """
                    ALTER TABLE usuarios
                    ADD COLUMN chave_publica TEXT
                    """
            );

            System.out.println(
                    "Coluna 'chave_publica' adicionada à tabela usuarios."
            );
        }

        if (
                colunaSenhaExiste
                        && colunaSenhaEhObrigatoria(
                                statement
                        )
        ) {

            System.out.println(
                    "Migrando estrutura da tabela usuarios..."
            );

            statement.executeUpdate(
                    """
                    CREATE TABLE usuarios_nova (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nome TEXT NOT NULL UNIQUE,
                        senha TEXT,
                        senha_hash TEXT,
                        chave_publica TEXT
                    )
                    """
            );

            statement.executeUpdate(
                    """
                    INSERT INTO usuarios_nova (
                        id,
                        nome,
                        senha,
                        senha_hash,
                        chave_publica
                    )
                    SELECT
                        id,
                        nome,
                        senha,
                        senha_hash,
                        chave_publica
                    FROM usuarios
                    """
            );

            statement.executeUpdate(
                    "DROP TABLE usuarios"
            );

            statement.executeUpdate(
                    """
                    ALTER TABLE usuarios_nova
                    RENAME TO usuarios
                    """
            );

            System.out.println(
                    "Tabela usuarios migrada com sucesso."
            );
        }
    }

    private static boolean colunaSenhaEhObrigatoria(
            Statement statement
    ) throws SQLException {

        try (
                var resultado =
                        statement.executeQuery(
                                "PRAGMA table_info(usuarios)"
                        )
        ) {

            while (resultado.next()) {

                String nomeColuna =
                        resultado.getString("name");

                if ("senha".equalsIgnoreCase(
                        nomeColuna
                )) {

                    int notNull =
                            resultado.getInt("notnull");

                    return notNull == 1;
                }
            }
        }

        return false;
    }

    private static void migrarTabelaMensagens(
            Statement statement
    ) throws SQLException {

        boolean colunaEntregueExiste = false;
        boolean colunaLidaExiste = false;

        try (
                var resultado =
                        statement.executeQuery(
                                "PRAGMA table_info(mensagens)"
                        )
        ) {

            while (resultado.next()) {

                String nomeColuna =
                        resultado.getString("name");

                if ("entregue".equalsIgnoreCase(
                        nomeColuna
                )) {

                    colunaEntregueExiste = true;
                }

                if ("lida".equalsIgnoreCase(
                        nomeColuna
                )) {

                    colunaLidaExiste = true;
                }
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

        if (!colunaLidaExiste) {

            statement.executeUpdate(
                    """
                    ALTER TABLE mensagens
                    ADD COLUMN lida
                    INTEGER NOT NULL DEFAULT 0
                    """
            );

            System.out.println(
                    "Coluna 'lida' adicionada à tabela mensagens."
            );
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