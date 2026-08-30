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
    private final GerenciadorUsuarios gerenciadorUsuarios;

   public CadastroView(
        Stage stage,
        LoginView loginView,
        GerenciadorUsuarios gerenciadorUsuarios
) {
    this.stage = stage;
    this.loginView = loginView;
    this.gerenciadorUsuarios =
            gerenciadorUsuarios;
}

    public void mostrar() {

        // ==========================================
        // TÍTULO
        // ==========================================

        Label titulo =
                new Label("CRIAR CONTA");

        // ==========================================
        // USUÁRIO
        // ==========================================

        Label labelUsuario =
                new Label("Usuário");

        TextField campoUsuario =
                new TextField();

        campoUsuario.setPromptText(
                "Digite seu usuário"
        );

        // ==========================================
        // SENHA
        // ==========================================

        Label labelSenha =
                new Label("Senha");

        PasswordField campoSenha =
                new PasswordField();

        campoSenha.setPromptText(
                "Digite sua senha"
        );

        // ==========================================
        // CONFIRMAR SENHA
        // ==========================================

        Label labelConfirmarSenha =
                new Label("Confirmar senha");

        PasswordField campoConfirmarSenha =
                new PasswordField();

        campoConfirmarSenha.setPromptText(
                "Digite a senha novamente"
        );

        // ==========================================
        // MENSAGEM DE STATUS
        // ==========================================

        Label mensagemStatus =
                new Label();

        // ==========================================
        // BOTÃO CADASTRAR
        // ==========================================

        Button botaoCadastrar =
                new Button("Cadastrar");

        botaoCadastrar.setOnAction(event -> {

            String usuario =
                    campoUsuario.getText().trim();

            String senha =
                    campoSenha.getText();

            String confirmarSenha =
                    campoConfirmarSenha.getText();

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

            boolean cadastrado =
        gerenciadorUsuarios.cadastrarUsuario(
                usuario,
                senha
        );

if (!cadastrado) {

    mensagemStatus.setText(
            "Esse usuário já existe."
    );

    return;
}
            mensagemStatus.setText(
                    "Cadastro realizado com sucesso!"
            );
        });

        // ==========================================
        // BOTÃO VOLTAR
        // ==========================================

        Button botaoVoltar =
                new Button("Voltar para o login");

        botaoVoltar.setOnAction(event -> {

            loginView.mostrar();
        });

        // ==========================================
        // LAYOUT
        // ==========================================

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

        layout.setPrefWidth(400);

        // ==========================================
        // CENA
        // ==========================================

        Scene scene =
                new Scene(
                        layout,
                        400,
                        550
                );

        // ==========================================
        // JANELA
        // ==========================================

        stage.setTitle(
                "Cadastro - Chat"
        );

        stage.setScene(scene);

        stage.show();
    }
}