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

public class CadastroView {

    private final Stage stage;
    private final LoginView loginView;
    private final ClienteTCP clienteTCP;

    public CadastroView(
            Stage stage,
            LoginView loginView,
            ClienteTCP clienteTCP
    ) {
        this.stage = stage;
        this.loginView = loginView;
        this.clienteTCP = clienteTCP;
    }

    public void mostrar() {

        Label titulo =
                new Label("CRIAR CONTA");

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

        Label labelConfirmarSenha =
                new Label("Confirmar senha");

        PasswordField campoConfirmarSenha =
                new PasswordField();

        campoConfirmarSenha.setPromptText(
                "Digite a senha novamente"
        );

        Label mensagemStatus =
                new Label();

        Button botaoCadastrar =
                new Button("Cadastrar");

        botaoCadastrar.setOnAction(event -> {

            String usuario =
                    campoUsuario
                            .getText()
                            .trim();

            String senha =
                    campoSenha
                            .getText();

            String confirmarSenha =
                    campoConfirmarSenha
                            .getText();

            if (usuario.isEmpty()) {

                mensagemStatus.setText(
                        "Digite um usuário."
                );

                return;
            }

            if (senha.isEmpty()) {

                mensagemStatus.setText(
                        "Digite uma senha."
                );

                return;
            }

            if (!senha.equals(confirmarSenha)) {

                mensagemStatus.setText(
                        "As senhas não são iguais."
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
                    clienteTCP.cadastrarUsuario(
                            usuario,
                            senha
                    );

            if (
                    resposta != null
                            && resposta.equals(
                                    "REGISTER_OK"
                            )
            ) {

                mensagemStatus.setText(
                        "Cadastro realizado com sucesso!"
                );

                clienteTCP.desconectar();

            } else if (
                    resposta != null
                            && resposta.startsWith(
                                    "REGISTER_ERROR|"
                            )
            ) {

                mensagemStatus.setText(
                        resposta.substring(
                                "REGISTER_ERROR|".length()
                        )
                );

                clienteTCP.desconectar();

            } else {

                mensagemStatus.setText(
                        "Erro ao realizar cadastro."
                );

                clienteTCP.desconectar();
            }
        });

        Button botaoVoltar =
                new Button("Voltar para o login");

        botaoVoltar.setOnAction(event -> {

            loginView.mostrar();
        });

        VBox layout =
                new VBox(
                        10,
                        titulo,
                        labelUsuario,
                        campoUsuario,
                        labelSenha,
                        campoSenha,
                        labelConfirmarSenha,
                        campoConfirmarSenha,
                        botaoCadastrar,
                        mensagemStatus,
                        botaoVoltar
                );

        layout.setPadding(
                new Insets(30)
        );

        layout.setAlignment(
                Pos.CENTER
        );

        layout.setPrefWidth(
                400
        );

        Scene scene =
                new Scene(
                        layout,
                        400,
                        550
                );

        stage.setTitle(
                "Cadastro - Chat"
        );

        stage.setScene(scene);
        stage.show();
    }
}