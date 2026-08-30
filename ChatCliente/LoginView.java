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

    public LoginView(
        Stage stage,
        ChatClienteApp aplicativo,
        GerenciadorUsuarios gerenciadorUsuarios
) {
    this.stage = stage;
    this.aplicativo = aplicativo;
    this.gerenciadorUsuarios =
            gerenciadorUsuarios;
}

    public void mostrar() {

        // ==========================================
        // TÍTULO
        // ==========================================

        Label titulo =
                new Label("CHAT DE SEGURANÇA");

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

        Label mensagemStatus =
                new Label();

        campoSenha.setPromptText(
                "Digite sua senha"
        );

        // ==========================================
        // BOTÃO ENTRAR
        // ==========================================

        Button botaoEntrar =
                new Button("Entrar");

        botaoEntrar.setOnAction(event -> {

            String usuario =
            campoUsuario.getText().trim();

    String senha =
            campoSenha.getText();

    boolean autenticado =
            gerenciadorUsuarios.autenticar(
                    usuario,
                    senha
            );

    if (autenticado) {

        aplicativo.mostrarChat(stage);

    } else {

        mensagemStatus.setText(
                "Usuário ou senha incorretos."
        );
    }

        });

        // ==========================================
        // BOTÃO CADASTRO
        // ==========================================

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

        // ==========================================
        // CENA
        // ==========================================

        Scene scene =
                new Scene(
                        layout,
                        400,
                        500
                );

        // ==========================================
        // JANELA
        // ==========================================

        stage.setTitle(
                "Login - Chat"
        );

        stage.setScene(scene);

        stage.show();
    }
}