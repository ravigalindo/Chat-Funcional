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
                    gerenciadorUsuarios
            );

    loginView.mostrar();
    }

    // ==========================================
    // INTERFACE PRINCIPAL DO CHAT
    // ==========================================

    public void mostrarChat(Stage stage) {

        // ==========================================
        // CONTATOS
        // ==========================================

        Label tituloContatos =
                new Label("CONTATOS");

        ListView<String> listaContatos =
                new ListView<>();

        listaContatos.getItems().addAll(
                "🟢 João",
                "🟢 Maria",
                "⚫ Carlos"
        );

        // ==========================================
        // NOME DO CONTATO
        // ==========================================

        Label nomeContato =
                new Label("Selecione um contato");

        // ==========================================
        // ÁREA DE MENSAGENS
        // ==========================================

        mensagens =
                new VBox(10);

        mensagens.setPadding(
                new Insets(15)
        );

        ScrollPane scrollMensagens =
                new ScrollPane(mensagens);

        scrollMensagens.setFitToWidth(true);

        // ==========================================
        // CAMPO DE MENSAGEM
        // ==========================================

        TextField campoMensagem =
                new TextField();

        campoMensagem.setPromptText(
                "Digite uma mensagem..."
        );

        Button botaoEnviar =
                new Button("Enviar");

        // ==========================================
        // BOTÃO ENVIAR
        // ==========================================

        botaoEnviar.setOnAction(event -> {

            enviarMensagem(
                    campoMensagem
            );
        });

        // ==========================================
        // ENTER PARA ENVIAR
        // ==========================================

        campoMensagem.setOnAction(event -> {

            enviarMensagem(
                    campoMensagem
            );
        });

        // ==========================================
        // CAMPO DE ENVIO
        // ==========================================

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

        // ==========================================
        // SELEÇÃO DE CONTATO
        // ==========================================

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

        // ==========================================
        // PAINEL DE CONTATOS
        // ==========================================

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

        // ==========================================
        // PAINEL DA CONVERSA
        // ==========================================

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

        // ==========================================
        // LAYOUT PRINCIPAL
        // ==========================================

        BorderPane layoutPrincipal =
                new BorderPane();

        layoutPrincipal.setLeft(
                painelContatos
        );

        layoutPrincipal.setCenter(
                painelConversa
        );

        // ==========================================
        // CENA
        // ==========================================

        Scene scene =
                new Scene(
                        layoutPrincipal,
                        800,
                        600
                );

        // ==========================================
        // JANELA
        // ==========================================

        stage.setTitle(
                "Chat - Segurança da Informação"
        );

        stage.setScene(scene);

        stage.show();
    }

    // ==========================================
    // ENVIAR MENSAGEM
    // ==========================================

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

    // ==========================================
    // ADICIONAR MENSAGEM NA TELA
    // ==========================================

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

    // ==========================================
    // CARREGAR HISTÓRICO
    // ==========================================

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

    // ==========================================
    // REMOVER STATUS DO NOME
    // ==========================================

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