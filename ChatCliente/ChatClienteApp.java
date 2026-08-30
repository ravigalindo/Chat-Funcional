package ChatCliente;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatClienteApp extends Application {

    @Override
    public void start(Stage stage) {

        Label titulo = new Label("CHAT");

        Label tituloContatos =
                new Label("CONTATOS");

        ListView<String> listaContatos =
                new ListView<>();

        listaContatos.getItems().addAll(
                "🟢 João",
                "🟢 Maria",
                "⚫ Carlos"
        );

        Label nomeContato =
                new Label("Selecione um contato");

        listaContatos.setOnMouseClicked(event -> {

            String contatoSelecionado =
                    listaContatos.getSelectionModel()
                            .getSelectedItem();

            if (contatoSelecionado != null) {

                nomeContato.setText(
                        contatoSelecionado
                );
            }
        });

        VBox painelContatos = new VBox(
                10,
                tituloContatos,
                listaContatos
        );

        painelContatos.setPadding(
                new Insets(15)
        );

        painelContatos.setPrefWidth(200);

        VBox mensagens =
                new VBox(10);

        mensagens.setPadding(
                new Insets(15)
        );

        TextField campoMensagem =
                new TextField();

        campoMensagem.setPromptText(
                "Digite uma mensagem..."
        );

        Button botaoEnviar =
                new Button("Enviar");

        HBox campoEnvio = new HBox(
                10,
                campoMensagem,
                botaoEnviar
        );

        campoEnvio.setPadding(
                new Insets(10)
        );

        HBox.setHgrow(
                campoMensagem,
                javafx.scene.layout.Priority.ALWAYS
        );

        BorderPane painelConversa =
                new BorderPane();

        painelConversa.setTop(
                nomeContato
        );

        painelConversa.setCenter(
                mensagens
        );

        painelConversa.setBottom(
                campoEnvio
        );

        BorderPane.setMargin(
                nomeContato,
                new Insets(15)
        );

        BorderPane layoutPrincipal =
                new BorderPane();

        layoutPrincipal.setLeft(
                painelContatos
        );

        layoutPrincipal.setCenter(
                painelConversa
        );

        Scene scene =
                new Scene(
                        layoutPrincipal,
                        800,
                        600
                );

        stage.setTitle(
                "Chat - Segurança da Informação"
        );

        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}