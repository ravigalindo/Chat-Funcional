package ChatCliente;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {

    private final Stage stage;
    private final ChatClienteApp aplicativo;
    private final GerenciadorUsuarios gerenciadorUsuarios;
    private final Sessao sessao;
    private final ClienteTCP clienteTCP;

    public LoginView(
            Stage stage,
            ChatClienteApp aplicativo,
            GerenciadorUsuarios gerenciadorUsuarios,
            Sessao sessao,
            ClienteTCP clienteTCP
    ) {
        this.stage = stage;
        this.aplicativo = aplicativo;
        this.gerenciadorUsuarios = gerenciadorUsuarios;
        this.sessao = sessao;
        this.clienteTCP = clienteTCP;
    }

    public void mostrar() {

        Label titulo =
                new Label("CHAT DE SEGURANÇA");

        Label labelUsuario =
                new Label("Usuário");

        TextField campoUsuario =
                new TextField();

        campoUsuario.setPromptText(
                "Digite seu usuário"
        );

        Label labelSenha =
                new Label("Senha");

        PasswordField campoSenha =
                new PasswordField();

        campoSenha.setPromptText(
                "Digite sua senha"
        );

        Label mensagemStatus =
                new Label();

        Button botaoEntrar =
                new Button("Entrar");

        botaoEntrar.setOnAction(event -> {

            String usuario =
                    campoUsuario.getText().trim();

            String senha =
                    campoSenha.getText();

            Usuario usuarioAutenticado =
                    gerenciadorUsuarios
                            .obterUsuarioAutenticado(
                                    usuario,
                                    senha
                            );

            if (usuarioAutenticado == null) {

                mensagemStatus.setText(
                        "Usuário ou senha incorretos."
                );

                return;
            }

            boolean conectado =
                    clienteTCP.conectar();

            if (!conectado) {

                mensagemStatus.setText(
                        "Não foi possível conectar ao servidor."
                );

                return;
            }

            String resposta =
                    clienteTCP.fazerLogin(
                            usuarioAutenticado.getNome()
                    );

            if (resposta != null &&
                    resposta.startsWith("LOGIN_OK|")) {

                sessao.iniciarSessao(
                        usuarioAutenticado
                );

                clienteTCP.iniciarRecebimento(
                        mensagem -> {

                            aplicativo.receberMensagemServidor(
                                    mensagem
                            );
                        }
                );

                aplicativo.mostrarChat(
                        stage
                );

            } else {

                mensagemStatus.setText(
                        "Servidor recusou o login."
                );

                clienteTCP.desconectar();
            }
        });

        Button botaoCadastro =
                new Button("Criar uma conta");

        botaoCadastro.setOnAction(event -> {

            CadastroView cadastroView =
                    new CadastroView(
                            stage,
                            this,
                            gerenciadorUsuarios
                    );

            cadastroView.mostrar();
        });

        VBox layout =
                new VBox(
                        10,
                        titulo,
                        labelUsuario,
                        campoUsuario,
                        labelSenha,
                        campoSenha,
                        botaoEntrar,
                        mensagemStatus,
                        botaoCadastro
                );

        layout.setPadding(
                new Insets(30)
        );

        layout.setAlignment(
                Pos.CENTER
        );

        layout.setPrefWidth(400);

        Scene scene =
                new Scene(
                        layout,
                        400,
                        500
                );

        stage.setTitle(
                "Login - Chat"
        );

        stage.setScene(scene);
        stage.show();
    }
}