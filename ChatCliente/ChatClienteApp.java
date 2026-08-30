package ChatCliente;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClienteApp extends javafx.application.Application {

    private final Map<String, List<Mensagem>> historicoConversas =
            new HashMap<>();

    private VBox mensagens;

    private String contatoAtual;

    @Override
    public void start(Stage stage) {

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

        mensagens =
                new VBox(10);

        mensagens.setPadding(
                new Insets(15)
        );

        ScrollPane scrollMensagens =
        new ScrollPane(mensagens);

        scrollMensagens.setFitToWidth(true);

        TextField campoMensagem =
                new TextField();

        campoMensagem.setPromptText(
                "Digite uma mensagem..."
        );

        Button botaoEnviar =
                new Button("Enviar");

        botaoEnviar.setOnAction(event -> {

            enviarMensagem(
                    campoMensagem
            );
        });

        campoMensagem.setOnAction(event -> {

            enviarMensagem(
                    campoMensagem
            );
        });

        HBox campoEnvio =
                new HBox(
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

        listaContatos.setOnMouseClicked(event -> {

            String contatoSelecionado =
                    listaContatos.getSelectionModel()
                            .getSelectedItem();

            if (contatoSelecionado != null) {

                contatoAtual =
                        removerStatus(
                                contatoSelecionado
                        );

                nomeContato.setText(
                        contatoSelecionado
                );

                carregarHistorico();
            }
        });

        VBox painelContatos =
                new VBox(
                        10,
                        tituloContatos,
                        listaContatos
                );

        painelContatos.setPadding(
                new Insets(15)
        );

        painelContatos.setPrefWidth(200);

        BorderPane painelConversa =
                new BorderPane();

        painelConversa.setTop(
                nomeContato
        );

        painelConversa.setCenter(
                scrollMensagens
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

    private void enviarMensagem(
            TextField campoMensagem
    ) {

        if (contatoAtual == null) {
            return;
        }

        String texto =
                campoMensagem.getText().trim();

        if (texto.isEmpty()) {
            return;
        }

        Mensagem mensagem =
                new Mensagem(
                        "Você",
                        texto,
                        true
                );

        historicoConversas
                .computeIfAbsent(
                        contatoAtual,
                        chave -> new ArrayList<>()
                )
                .add(mensagem);

        adicionarMensagemNaTela(
                mensagem
        );

        campoMensagem.clear();
    }

    private void adicionarMensagemNaTela(
            Mensagem mensagem
    ) {

        Label textoMensagem =
                new Label(
                        mensagem.getConteudo()
                );

        textoMensagem.setWrapText(true);

        textoMensagem.setPadding(
                new Insets(10)
        );

        textoMensagem.setMaxWidth(400);

        HBox balao =
                new HBox(
                        textoMensagem
                );

        if (mensagem.isEnviadaPorMim()) {

            balao.setAlignment(
                    Pos.CENTER_RIGHT
            );

        } else {

            balao.setAlignment(
                    Pos.CENTER_LEFT
            );
        }

        mensagens.getChildren().add(
                balao
        );
    }

    private void carregarHistorico() {

        mensagens.getChildren().clear();

        List<Mensagem> historico =
                historicoConversas.get(
                        contatoAtual
                );

        if (historico == null) {
            return;
        }

        for (Mensagem mensagem : historico) {

            adicionarMensagemNaTela(
                    mensagem
            );
        }
    }

    private String removerStatus(
            String contato
    ) {

        return contato
                .replace("🟢 ", "")
                .replace("⚫ ", "");
    }

    public static void main(String[] args) {

        launch();
    }
}