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

    private final GerenciadorUsuarios gerenciadorUsuarios =
            new GerenciadorUsuarios();

    private final Sessao sessao =
            new Sessao();

    private final Map<String, List<Mensagem>> historicoConversas =
            new HashMap<>();

    private VBox mensagens;

    private String contatoAtual;

    @Override
    public void start(Stage stage) {

        LoginView loginView =
                new LoginView(
                        stage,
                        this,
                        gerenciadorUsuarios,
                        sessao
                );

        loginView.mostrar();
    }

    public void mostrarChat(Stage stage) {

        Usuario usuarioLogado =
                sessao.getUsuarioLogado();

        Label usuarioLogadoLabel =
                new Label(
                        "👤 " + usuarioLogado.getNome()
                );

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
            enviarMensagem(campoMensagem);
        });

        campoMensagem.setOnAction(event -> {
            enviarMensagem(campoMensagem);
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

        HBox topo =
                new HBox(
                        10,
                        usuarioLogadoLabel
                );

        topo.setAlignment(
                Pos.CENTER_RIGHT
        );

        topo.setPadding(
                new Insets(10)
        );

        BorderPane layoutPrincipal =
                new BorderPane();

        layoutPrincipal.setTop(
                topo
        );

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
                        sessao.getUsuarioLogado().getNome(),
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

        // Nome do remetente
        Label remetente =
                new Label(
                        mensagem.getRemetente()
                );

        remetente.setStyle(
                "-fx-font-weight: bold;"
        );

        // Conteúdo da mensagem
        Label conteudo =
                new Label(
                        mensagem.getConteudo()
                );

        conteudo.setWrapText(true);

        conteudo.setMaxWidth(350);

        // Balão da mensagem
        VBox balaoMensagem =
                new VBox(
                        3,
                        remetente,
                        conteudo
                );

        balaoMensagem.setPadding(
                new Insets(10)
        );

        balaoMensagem.setMaxWidth(400);

        balaoMensagem.setStyle(
                "-fx-background-color: #E8E8E8;"
                + "-fx-background-radius: 10;"
        );

        // Container responsável pelo alinhamento
        HBox linhaMensagem =
                new HBox(
                        balaoMensagem
                );

        linhaMensagem.setPadding(
                new Insets(3, 0, 3, 0)
        );

        if (mensagem.isEnviadaPorMim()) {

            linhaMensagem.setAlignment(
                    Pos.CENTER_RIGHT
            );

            balaoMensagem.setStyle(
                    "-fx-background-color: #DCF8C6;"
                    + "-fx-background-radius: 10;"
            );

        } else {

            linhaMensagem.setAlignment(
                    Pos.CENTER_LEFT
            );
        }

        mensagens.getChildren().add(
                linhaMensagem
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

        for (Mensagem mensagem :
                historico) {

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