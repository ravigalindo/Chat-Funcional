package ChatCliente;

import javafx.application.Platform;

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
import java.util.Timer;
import java.util.TimerTask;

public class ChatClienteApp
        extends javafx.application.Application {

    private final Sessao sessao =
            new Sessao();

    private final ClienteTCP clienteTCP =
            new ClienteTCP();

    private final Map<String, List<Mensagem>> historicoConversas =
            new HashMap<>();

    private VBox mensagens;

    private ListView<String> listaContatos;

    private String contatoAtual;

    private Timer timerDigitacao;

    private Label indicadorDigitacao;

    @Override
    public void start(Stage stage) {

        LoginView loginView =
                new LoginView(
                        stage,
                        this,
                        sessao,
                        clienteTCP
                );

        loginView.mostrar();
    }

    public void receberMensagemServidor(
            String mensagem
    ) {

        if (mensagem.startsWith("MESSAGE|")) {

            String[] partes =
                    mensagem.split("\\|", 3);

            if (partes.length < 3) {
                return;
            }

            String remetente =
                    partes[1];

            String conteudo =
                    partes[2];

            Mensagem novaMensagem =
                    new Mensagem(
                            remetente,
                            conteudo,
                            false
                    );

            historicoConversas
                    .computeIfAbsent(
                            remetente,
                            chave ->
                                    new ArrayList<>()
                    )
                    .add(novaMensagem);

            Platform.runLater(() -> {

                if (remetente.equals(contatoAtual)) {

                    adicionarMensagemNaTela(
                            novaMensagem
                    );
                }
            });

            return;
        }

        if (mensagem.startsWith("USERS|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String usuarios =
                    partes[1];

            Platform.runLater(() -> {

                atualizarListaContatos(
                        usuarios
                );
            });

            return;
        }

        /*
         * Recebe o aviso de que o outro usuário
         * começou a digitar.
         */
        if (mensagem.startsWith("TYPING|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1].trim();

            System.out.println(
                    "DIGITANDO recebido de: "
                            + nomeUsuario
            );

            Platform.runLater(() -> {

                if (contatoAtual != null
                        && nomeUsuario.equalsIgnoreCase(
                                contatoAtual
                        )) {

                    indicadorDigitacao.setText(
                            nomeUsuario
                                    + " está digitando..."
                    );
                }
            });

            return;
        }

        /*
         * Recebe o aviso de que o outro usuário
         * parou de digitar.
         */
        if (mensagem.startsWith("STOP_TYPING|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1].trim();

            System.out.println(
                    "PAROU DE DIGITAR recebido de: "
                            + nomeUsuario
            );

            Platform.runLater(() -> {

                if (contatoAtual != null
                        && nomeUsuario.equalsIgnoreCase(
                                contatoAtual
                        )) {

                    indicadorDigitacao.setText(
                            ""
                    );
                }
            });

            return;
        }

        if (mensagem.startsWith("ONLINE|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1];

            Platform.runLater(() -> {

                atualizarStatusContato(
                        nomeUsuario,
                        true
                );
            });

            return;
        }

        if (mensagem.startsWith("OFFLINE|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1];

            Platform.runLater(() -> {

                atualizarStatusContato(
                        nomeUsuario,
                        false
                );
            });
        }
    }

    public void mostrarChat(Stage stage) {

        Usuario usuarioLogado =
                sessao.getUsuarioLogado();

        Label usuarioLogadoLabel =
                new Label(
                        "👤 "
                                + usuarioLogado.getNome()
                );

        Label tituloContatos =
                new Label("CONTATOS");

        listaContatos =
                new ListView<>();

        Label nomeContato =
                new Label(
                        "Selecione um contato"
                );

        /*
         * Indicador de digitação.
         */
        indicadorDigitacao =
                new Label("");

        indicadorDigitacao.setStyle(
                "-fx-text-fill: #666666;"
                        + "-fx-font-style: italic;"
        );

        mensagens =
                new VBox(10);

        mensagens.setPadding(
                new Insets(15)
        );

        ScrollPane scrollMensagens =
                new ScrollPane(
                        mensagens
                );

        scrollMensagens.setFitToWidth(
                true
        );

        TextField campoMensagem =
                new TextField();

        campoMensagem.setPromptText(
                "Digite uma mensagem..."
        );

        /*
         * Detecta quando o usuário começa
         * ou continua digitando.
         */
        campoMensagem.setOnKeyTyped(event -> {

            if (contatoAtual == null) {
                return;
            }

            clienteTCP.enviarDigitacao(
                    contatoAtual
            );

            iniciarContadorParadaDigitacao();
        });

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
                    listaContatos
                            .getSelectionModel()
                            .getSelectedItem();

            if (contatoSelecionado != null) {

                contatoAtual =
                        removerStatus(
                                contatoSelecionado
                        );

                nomeContato.setText(
                        contatoSelecionado
                );

                /*
                 * Limpa o indicador ao trocar
                 * de conversa.
                 */
                indicadorDigitacao.setText(
                        ""
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

        painelContatos.setPrefWidth(
                200
        );

        /*
         * Cabeçalho da conversa.
         * Contém o nome do contato e o
         * indicador de digitação.
         */
        VBox cabecalhoConversa =
                new VBox(
                        3,
                        nomeContato,
                        indicadorDigitacao
                );

        BorderPane painelConversa =
                new BorderPane();

        painelConversa.setTop(
                cabecalhoConversa
        );

        painelConversa.setCenter(
                scrollMensagens
        );

        painelConversa.setBottom(
                campoEnvio
        );

        BorderPane.setMargin(
                cabecalhoConversa,
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

    private void atualizarListaContatos(
            String usuarios
    ) {

        if (listaContatos == null) {
            return;
        }

        listaContatos
                .getItems()
                .clear();

        if (usuarios.isEmpty()) {
            return;
        }

        String meuNome =
                sessao
                        .getUsuarioLogado()
                        .getNome();

        String[] listaUsuarios =
                usuarios.split(",");

        for (String nomeUsuario :
                listaUsuarios) {

            nomeUsuario =
                    nomeUsuario.trim();

            if (nomeUsuario.isEmpty()) {
                continue;
            }

            if (nomeUsuario.equalsIgnoreCase(
                    meuNome
            )) {
                continue;
            }

            listaContatos
                    .getItems()
                    .add(
                            "🟢 "
                                    + nomeUsuario
                    );
        }
    }

    private void atualizarStatusContato(
            String nomeUsuario,
            boolean online
    ) {

        if (listaContatos == null) {
            return;
        }

        for (int i = 0;
             i < listaContatos.getItems().size();
             i++) {

            String contato =
                    listaContatos
                            .getItems()
                            .get(i);

            String nome =
                    removerStatus(contato);

            if (nome.equalsIgnoreCase(
                    nomeUsuario
            )) {

                String novoStatus;

                if (online) {
                    novoStatus = "🟢 ";
                } else {
                    novoStatus = "⚫ ";
                }

                listaContatos
                        .getItems()
                        .set(
                                i,
                                novoStatus + nome
                        );

                break;
            }
        }
    }

    private void iniciarContadorParadaDigitacao() {

        if (timerDigitacao != null) {

            timerDigitacao.cancel();
        }

        timerDigitacao =
                new Timer();

        timerDigitacao.schedule(
                new TimerTask() {

                    @Override
                    public void run() {

                        if (contatoAtual != null) {

                            clienteTCP
                                    .enviarParadaDigitacao(
                                            contatoAtual
                                    );
                        }
                    }

                },
                1000
        );
    }

    private void enviarMensagem(
            TextField campoMensagem
    ) {

        if (contatoAtual == null) {
            return;
        }

        String texto =
                campoMensagem
                        .getText()
                        .trim();

        if (texto.isEmpty()) {
            return;
        }

        if (timerDigitacao != null) {

            timerDigitacao.cancel();
        }

        clienteTCP.enviarParadaDigitacao(
                contatoAtual
        );

        clienteTCP.enviarMensagem(
                contatoAtual,
                texto
        );

        Mensagem mensagem =
                new Mensagem(
                        sessao
                                .getUsuarioLogado()
                                .getNome(),
                        texto,
                        true
                );

        historicoConversas
                .computeIfAbsent(
                        contatoAtual,
                        chave ->
                                new ArrayList<>()
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

        Label remetente =
                new Label(
                        mensagem.getRemetente()
                );

        remetente.setStyle(
                "-fx-font-weight: bold;"
        );

        Label conteudo =
                new Label(
                        mensagem.getConteudo()
                );

        conteudo.setWrapText(
                true
        );

        conteudo.setMaxWidth(
                350
        );

        VBox balaoMensagem =
                new VBox(
                        3,
                        remetente,
                        conteudo
                );

        balaoMensagem.setPadding(
                new Insets(10)
        );

        balaoMensagem.setMaxWidth(
                400
        );

        balaoMensagem.setStyle(
                "-fx-background-color: #E8E8E8;"
                        + "-fx-background-radius: 10;"
        );

        HBox linhaMensagem =
                new HBox(
                        balaoMensagem
                );

        linhaMensagem.setPadding(
                new Insets(
                        3,
                        0,
                        3,
                        0
                )
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

        mensagens
                .getChildren()
                .clear();

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