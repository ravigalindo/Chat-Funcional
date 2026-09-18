package ChatCliente;

import javafx.application.Platform;

import javafx.geometry.Insets;

import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.Button;

import javafx.scene.control.ContextMenu;

import javafx.scene.control.Label;

import javafx.scene.control.ListView;

import javafx.scene.control.MenuItem;

import javafx.scene.control.ScrollPane;

import javafx.scene.control.TextField;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;

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

    /*
     * Guarda a quantidade de mensagens não
     * lidas de cada contato.
     *
     * A chave é o nome do contato.
     */
    private final Map<String, Integer> mensagensNaoLidas =
            new HashMap<>();

    /*
     * Guarda os usuários que estão cadastrados
     * no sistema.
     */
    private final List<String> usuariosCadastrados =
            new ArrayList<>();

    /*
     * Guarda os usuários que estão online
     * neste momento.
     */
    private final Set<String> usuariosOnline =
            new HashSet<>();

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

        /*
         * Novo formato:
         *
         * MESSAGE|id|remetente|conteudo
         */
        if (mensagem.startsWith("MESSAGE|")) {

            String[] partes =
                    mensagem.split("\\|", 4);

            if (partes.length < 4) {
                return;
            }

            int id;

            try {

                id =
                        Integer.parseInt(
                                partes[1]
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID de mensagem inválido."
                );

                return;
            }

            String remetente =
                    partes[2];

            String conteudo =
                    partes[3];

            String meuNome =
                    sessao
                            .getUsuarioLogado()
                            .getNome();

            boolean enviadaPorMim =
                    remetente.equalsIgnoreCase(
                            meuNome
                    );

            String contatoMensagem;

            if (enviadaPorMim) {

                contatoMensagem =
                        contatoAtual;

            } else {

                contatoMensagem =
                        remetente;
            }

            if (contatoMensagem == null) {
                return;
            }

            Mensagem novaMensagem =
                    new Mensagem(
                            id,
                            remetente,
                            conteudo,
                            enviadaPorMim
                    );

            historicoConversas
                    .computeIfAbsent(
                            contatoMensagem,
                            chave ->
                                    new ArrayList<>()
                    )
                    .add(
                            novaMensagem
                    );

            /*
             * Se a mensagem foi recebida de
             * outro usuário e a conversa NÃO
             * está aberta, aumenta o contador
             * de mensagens não lidas.
             */
            if (!enviadaPorMim
                    && (
                    contatoAtual == null
                            || !contatoMensagem.equalsIgnoreCase(
                            contatoAtual
                    )
            )) {

                int quantidadeAtual =
                        mensagensNaoLidas.getOrDefault(
                                remetente,
                                0
                        );

                int novaQuantidade =
                        quantidadeAtual + 1;

                mensagensNaoLidas.put(
                        remetente,
                        novaQuantidade
                );

                System.out.println(
                        "Mensagem não lida de "
                                + remetente
                                + ": "
                                + novaQuantidade
                );

                /*
                 * Atualiza visualmente o contador
                 * na lista de contatos.
                 */
                Platform.runLater(() -> {

                    atualizarContadorNaoLidasContato(
                            remetente
                    );
                });
            }

            Platform.runLater(() -> {

                if (contatoMensagem.equalsIgnoreCase(
                        contatoAtual
                )) {

                    adicionarMensagemNaTela(
                            novaMensagem
                    );

                    /*
                     * Se a mensagem foi recebida de
                     * outro usuário e a conversa já
                     * está aberta, marca a mensagem
                     * automaticamente como lida.
                     */
                    if (!enviadaPorMim) {

                        clienteTCP.marcarComoLidas(
                                remetente
                        );
                    }
                }
            });

            System.out.println(
                    "Mensagem recebida: "
                            + "ID=" + id
                            + " | "
                            + remetente
                            + " -> "
                            + conteudo
            );

            return;
        }

        /*
         * Recebe a confirmação de que uma
         * mensagem enviada por mim foi lida
         * pelo destinatário.
         *
         * Formato:
         *
         * READ|id
         */
        if (mensagem.startsWith("READ|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            int id;

            try {

                id =
                        Integer.parseInt(
                                partes[1]
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID inválido no aviso de leitura."
                );

                return;
            }

            System.out.println(
                    "Mensagem lida: ID="
                            + id
            );

            Platform.runLater(() -> {

                atualizarIndicadorMensagemLida(
                        id
                );
            });

            return;
        }

        if (mensagem.startsWith("HISTORY_MESSAGE|")) {

            String[] partes =
                    mensagem.split("\\|", 6);

            if (partes.length < 6) {
                return;
            }

            int id;

            try {

                id =
                        Integer.parseInt(
                                partes[1]
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID de mensagem do histórico inválido."
                );

                return;
            }

            String remetente =
                    partes[2];

            String destinatario =
                    partes[3];

            String conteudo =
                    partes[4];

            String dataHora =
                    partes[5];

            String meuNome =
                    sessao
                            .getUsuarioLogado()
                            .getNome();

            boolean enviadaPorMim =
                    remetente.equalsIgnoreCase(
                            meuNome
                    );

            String contato;

            if (remetente.equalsIgnoreCase(
                    meuNome
            )) {

                contato =
                        destinatario;

            } else {

                contato =
                        remetente;
            }

            Mensagem mensagemHistorico =
                    new Mensagem(
                            id,
                            remetente,
                            conteudo,
                            enviadaPorMim
                    );

            historicoConversas
                    .computeIfAbsent(
                            contato,
                            chave ->
                                    new ArrayList<>()
                    )
                    .add(
                            mensagemHistorico
                    );

            Platform.runLater(() -> {

                if (contatoAtual != null
                        && contato.equalsIgnoreCase(
                                contatoAtual
                        )) {

                    adicionarMensagemNaTela(
                            mensagemHistorico
                    );
                }
            });

            System.out.println(
                    "Histórico recebido: "
                            + "ID=" + id
                            + " | "
                            + dataHora
                            + " | "
                            + remetente
                            + " -> "
                            + destinatario
                            + " | "
                            + conteudo
            );

            return;
        }

        if (mensagem.equals("HISTORY_END")) {

            System.out.println(
                    "Fim do histórico."
            );

            return;
        }

        /*
         * Confirmação de que o servidor
         * processou a exclusão da mensagem.
         */
        if (mensagem.startsWith("DELETE_OK|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            int id;

            try {

                id =
                        Integer.parseInt(
                                partes[1]
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "ID inválido na confirmação de exclusão."
                );

                return;
            }

            System.out.println(
                    "Mensagem "
                            + id
                            + " apagada com sucesso."
            );

            /*
             * Remove a mensagem da memória
             * do cliente.
             */
            removerMensagemDaMemoria(
                    id
            );

            /*
             * Remove a mensagem da interface.
             */
            Platform.runLater(() -> {

                removerMensagemDaTela(
                        id
                );
            });

            return;
        }

        /*
         * Erro ao tentar apagar uma mensagem.
         */
        if (mensagem.startsWith("DELETE_ERROR|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String erro =
                    partes[1];

            System.out.println(
                    "Erro ao apagar mensagem: "
                            + erro
            );

            return;
        }

        /*
         * Recebe todos os usuários cadastrados
         * no banco.
         */
        if (mensagem.startsWith("USERS|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String usuarios =
                    partes[1];

            Platform.runLater(() -> {

                atualizarListaUsuarios(
                        usuarios
                );
            });

            return;
        }

        /*
         * Recebe a lista de usuários que já
         * estavam online quando este cliente
         * entrou.
         */
        if (mensagem.startsWith("ONLINE_USERS|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String usuarios =
                    partes[1];

            Platform.runLater(() -> {

                atualizarUsuariosOnline(
                        usuarios
                );

                atualizarListaContatosVisual();
            });

            return;
        }

        /*
         * Recebe o aviso de que um usuário
         * acabou de ficar online.
         */
        if (mensagem.startsWith("ONLINE|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1].trim();

            Platform.runLater(() -> {

                adicionarUsuarioOnline(
                        nomeUsuario
                );

                atualizarStatusContato(
                        nomeUsuario,
                        true
                );
            });

            return;
        }

        /*
         * Recebe o aviso de que um usuário
         * ficou offline.
         */
        if (mensagem.startsWith("OFFLINE|")) {

            String[] partes =
                    mensagem.split("\\|", 2);

            if (partes.length < 2) {
                return;
            }

            String nomeUsuario =
                    partes[1].trim();

            Platform.runLater(() -> {

                removerUsuarioOnline(
                        nomeUsuario
                );

                atualizarStatusContato(
                        nomeUsuario,
                        false
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

                /*
                 * Ao abrir uma conversa,
                 * todas as mensagens não lidas
                 * daquele contato são consideradas
                 * visualizadas.
                 */
                zerarMensagensNaoLidas(
                        contatoAtual
                );

                /*
                 * Atualiza visualmente o contato
                 * para remover o contador.
                 */
                atualizarContadorNaoLidasContato(
                        contatoAtual
                );

                nomeContato.setText(
                        contatoAtual
                );

                indicadorDigitacao.setText(
                        ""
                );

                carregarHistorico();

                clienteTCP.marcarComoLidas(
                        contatoAtual
                );
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

        /*
         * Caso a lista de usuários tenha sido
         * recebida antes da interface terminar
         * de ser criada, atualizamos a tela agora.
         */
        atualizarListaContatosVisual();
    }

    /*
     * Atualiza a lista de usuários cadastrados.
     */
    private void atualizarListaUsuarios(
            String usuarios
    ) {

        usuariosCadastrados.clear();

        if (!usuarios.isEmpty()) {

            String[] listaUsuarios =
                    usuarios.split(",");

            for (String nomeUsuario :
                    listaUsuarios) {

                nomeUsuario =
                        nomeUsuario.trim();

                if (nomeUsuario.isEmpty()) {
                    continue;
                }

                if (!contemUsuario(
                        usuariosCadastrados,
                        nomeUsuario
                )) {

                    usuariosCadastrados.add(
                            nomeUsuario
                    );
                }
            }
        }

        atualizarListaContatosVisual();
    }

    /*
     * Atualiza o conjunto de usuários online
     * recebido pelo servidor.
     */
    private void atualizarUsuariosOnline(
            String usuarios
    ) {

        usuariosOnline.clear();

        if (usuarios.isEmpty()) {
            return;
        }

        String[] listaUsuarios =
                usuarios.split(",");

        for (String nomeUsuario :
                listaUsuarios) {

            nomeUsuario =
                    nomeUsuario.trim();

            if (nomeUsuario.isEmpty()) {
                continue;
            }

            usuariosOnline.add(
                    nomeUsuario
            );
        }
    }

    /*
     * Adiciona um usuário ao conjunto
     * de usuários online.
     */
    private void adicionarUsuarioOnline(
            String nomeUsuario
    ) {

        if (nomeUsuario == null
                || nomeUsuario.trim().isEmpty()) {

            return;
        }

        usuariosOnline.add(
                nomeUsuario.trim()
        );
    }

    /*
     * Remove um usuário do conjunto
     * de usuários online.
     */
    private void removerUsuarioOnline(
            String nomeUsuario
    ) {

        if (nomeUsuario == null) {
            return;
        }

        usuariosOnline.removeIf(
                usuario ->
                        usuario.equalsIgnoreCase(
                                nomeUsuario.trim()
                        )
        );
    }

    /*
     * Monta visualmente a lista de contatos
     * usando os usuários cadastrados, o status
     * online/offline e a quantidade de mensagens
     * não lidas.
     */
    private void atualizarListaContatosVisual() {

        if (listaContatos == null) {
            return;
        }

        listaContatos
                .getItems()
                .clear();

        String meuNome =
                sessao
                        .getUsuarioLogado()
                        .getNome();

        for (String nomeUsuario :
                usuariosCadastrados) {

            if (nomeUsuario.equalsIgnoreCase(
                    meuNome
            )) {

                continue;
            }

            listaContatos
                    .getItems()
                    .add(
                            formatarContato(
                                    nomeUsuario
                            )
                    );
        }
    }

    /*
     * Monta o texto visual de um contato.
     *
     * Exemplo:
     *
     * 🟢 Maria  3
     *
     * ou:
     *
     * ⚫ João
     */
    private String formatarContato(
            String nomeUsuario
    ) {

        boolean online =
                estaOnline(
                        nomeUsuario
                );

        String status;

        if (online) {

            status = "🟢 ";

        } else {

            status = "⚫ ";
        }

        int quantidadeNaoLidas =
                obterQuantidadeNaoLidas(
                        nomeUsuario
                );

        String contador = "";

        if (quantidadeNaoLidas > 0) {

            contador =
                    "  " + quantidadeNaoLidas;
        }

        return status
                + nomeUsuario
                + contador;
    }

    /*
     * Retorna a quantidade de mensagens
     * não lidas de determinado contato.
     *
     * A busca ignora diferença entre
     * maiúsculas e minúsculas.
     */
    private int obterQuantidadeNaoLidas(
            String nomeUsuario
    ) {

        if (nomeUsuario == null) {
            return 0;
        }

        for (Map.Entry<String, Integer> entrada :
                mensagensNaoLidas.entrySet()) {

            if (entrada.getKey().equalsIgnoreCase(
                    nomeUsuario
            )) {

                return entrada.getValue();
            }
        }

        return 0;
    }

    /*
     * Atualiza somente o contador visual
     * de um contato.
     */
    private void atualizarContadorNaoLidasContato(
            String nomeUsuario
    ) {

        if (listaContatos == null
                || nomeUsuario == null) {

            return;
        }

        for (int i = 0;
             i < listaContatos.getItems().size();
             i++) {

            String contatoAtualLista =
                    listaContatos
                            .getItems()
                            .get(i);

            String nome =
                    removerStatus(
                            contatoAtualLista
                    );

            if (nome.equalsIgnoreCase(
                    nomeUsuario
            )) {

                listaContatos
                        .getItems()
                        .set(
                                i,
                                formatarContato(
                                        nome
                                )
                        );

                break;
            }
        }
    }

    /*
     * Atualiza somente o status visual
     * de um contato.
     *
     * O contador de mensagens não lidas
     * é preservado.
     */
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
                    removerStatus(
                            contato
                    );

            if (nome.equalsIgnoreCase(
                    nomeUsuario
            )) {

                /*
                 * O parâmetro online já representa
                 * o novo estado do usuário.
                 *
                 * Por isso atualizamos diretamente
                 * o conjunto de usuários online
                 * antes de montar o contato.
                 */
                if (online) {

                    usuariosOnline.add(
                            nomeUsuario
                    );

                } else {

                    usuariosOnline.removeIf(
                            usuario ->
                                    usuario.equalsIgnoreCase(
                                            nomeUsuario
                                    )
                    );
                }

                listaContatos
                        .getItems()
                        .set(
                                i,
                                formatarContato(
                                        nome
                                )
                        );

                break;
            }
        }
    }

    /*
     * Verifica se um usuário está online.
     */
    private boolean estaOnline(
            String nomeUsuario
    ) {

        for (String usuario :
                usuariosOnline) {

            if (usuario.equalsIgnoreCase(
                    nomeUsuario
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * Verifica se uma lista já contém
     * determinado usuário ignorando
     * diferença entre maiúsculas e minúsculas.
     */
    private boolean contemUsuario(
            List<String> lista,
            String nomeUsuario
    ) {

        for (String usuario :
                lista) {

            if (usuario.equalsIgnoreCase(
                    nomeUsuario
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * Zera a quantidade de mensagens não
     * lidas de determinado contato.
     */
    private void zerarMensagensNaoLidas(
            String nomeUsuario
    ) {

        if (nomeUsuario == null
                || nomeUsuario.trim().isEmpty()) {

            return;
        }

        String chaveEncontrada = null;

        for (String chave :
                mensagensNaoLidas.keySet()) {

            if (chave.equalsIgnoreCase(
                    nomeUsuario
            )) {

                chaveEncontrada = chave;

                break;
            }
        }

        if (chaveEncontrada == null) {

            mensagensNaoLidas.put(
                    nomeUsuario,
                    0
            );

            return;
        }

        int quantidadeAtual =
                mensagensNaoLidas.getOrDefault(
                        chaveEncontrada,
                        0
                );

        mensagensNaoLidas.put(
                chaveEncontrada,
                0
        );

        if (quantidadeAtual > 0) {

            System.out.println(
                    "Mensagens não lidas de "
                            + nomeUsuario
                            + " zeradas."
            );
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

        /*
         * A mensagem será adicionada à tela
         * somente quando o servidor devolver
         * o ID real.
         */
        clienteTCP.enviarMensagem(
                contatoAtual,
                texto
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

        /*
         * Área inferior do balão.
         *
         * Para mensagens enviadas por mim,
         * será exibido o indicador "•".
         */
        HBox rodapeMensagem =
                new HBox();

        rodapeMensagem.setAlignment(
                Pos.CENTER_RIGHT
        );

        if (mensagem.isEnviadaPorMim()) {

            Label indicadorEntrega =
                    new Label("•");

            indicadorEntrega.setStyle(
                    "-fx-font-size: 14px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-text-fill: #555555;"
            );

            /*
             * Guarda o indicador de entrega
             * no balão para podermos alterá-lo
             * posteriormente quando a mensagem
             * for lida.
             */
            rodapeMensagem.setUserData(
                    indicadorEntrega
            );

            rodapeMensagem
                    .getChildren()
                    .add(
                            indicadorEntrega
                    );
        }

        VBox balaoMensagem =
                new VBox(
                        3,
                        remetente,
                        conteudo,
                        rodapeMensagem
                );

        /*
         * Guarda o ID real da mensagem
         * no componente visual.
         */
        balaoMensagem.setUserData(
                mensagem.getId()
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

        /*
         * Menu exibido ao clicar com o
         * botão direito na mensagem.
         */
        ContextMenu menuMensagem =
                new ContextMenu();

        MenuItem apagarParaMim =
                new MenuItem(
                        "Apagar para mim"
                );

        apagarParaMim.setOnAction(event -> {

            apagarMensagemParaMim(
                    mensagem
            );
        });

        menuMensagem
                .getItems()
                .add(
                        apagarParaMim
                );

        balaoMensagem.setOnContextMenuRequested(
                event -> {

                    menuMensagem.show(
                            balaoMensagem,
                            event.getScreenX(),
                            event.getScreenY()
                    );
                }
        );

        mensagens.getChildren().add(
                linhaMensagem
        );
    }

    /*
     * Atualiza o indicador visual de uma
     * mensagem quando o servidor informa
     * que ela foi lida.
     */
    private void atualizarIndicadorMensagemLida(
            int id
    ) {

        if (mensagens == null) {
            return;
        }

        for (javafx.scene.Node node :
                mensagens.getChildren()) {

            if (!(node instanceof HBox)) {
                continue;
            }

            HBox linhaMensagem =
                    (HBox) node;

            if (linhaMensagem
                    .getChildren()
                    .isEmpty()) {

                continue;
            }

            javafx.scene.Node balao =
                    linhaMensagem
                            .getChildren()
                            .get(0);

            if (!(balao instanceof VBox)) {
                continue;
            }

            VBox balaoMensagem =
                    (VBox) balao;

            Object idMensagem =
                    balaoMensagem.getUserData();

            if (!(idMensagem instanceof Integer)) {
                continue;
            }

            if ((Integer) idMensagem != id) {
                continue;
            }

            if (balaoMensagem
                    .getChildren()
                    .size() < 3) {

                return;
            }

            javafx.scene.Node rodape =
                    balaoMensagem
                            .getChildren()
                            .get(2);

            if (!(rodape instanceof HBox)) {
                return;
            }

            HBox rodapeMensagem =
                    (HBox) rodape;

            Object indicador =
                    rodapeMensagem.getUserData();

            if (indicador instanceof Label) {

                Label indicadorEntrega =
                        (Label) indicador;

                indicadorEntrega.setText(
                        "••"
                );

                System.out.println(
                        "Indicador da mensagem "
                                + id
                                + " atualizado para ••."
                );
            }

            return;
        }
    }

    /*
     * Solicita ao servidor a exclusão lógica
     * da mensagem para o usuário atual.
     */
    private void apagarMensagemParaMim(
            Mensagem mensagem
    ) {

        int id =
                mensagem.getId();

        if (id <= 0) {

            System.out.println(
                    "Não é possível apagar uma mensagem sem ID."
            );

            return;
        }

        System.out.println(
                "Solicitando exclusão da mensagem ID="
                        + id
        );

        clienteTCP.apagarMensagem(
                id
        );
    }

    /*
     * Remove uma mensagem da memória local.
     */
    private void removerMensagemDaMemoria(
            int id
    ) {

        for (
                List<Mensagem> mensagensConversa :
                historicoConversas.values()
        ) {

            mensagensConversa.removeIf(
                    mensagem ->
                            mensagem.getId() == id
            );
        }
    }

    /*
     * Remove a mensagem correspondente ao ID
     * da interface gráfica.
     */
    private void removerMensagemDaTela(
            int id
    ) {

        if (mensagens == null) {
            return;
        }

        for (int i = 0;
             i < mensagens.getChildren().size();
             i++) {

            javafx.scene.Node node =
                    mensagens
                            .getChildren()
                            .get(i);

            if (!(node instanceof HBox)) {
                continue;
            }

            HBox linha =
                    (HBox) node;

            if (linha.getChildren().isEmpty()) {
                continue;
            }

            javafx.scene.Node balao =
                    linha
                            .getChildren()
                            .get(0);

            if (!(balao instanceof VBox)) {
                continue;
            }

            VBox balaoMensagem =
                    (VBox) balao;

            Object idMensagem =
                    balaoMensagem.getUserData();

            if (idMensagem instanceof Integer
                    && (Integer) idMensagem == id) {

                mensagens
                        .getChildren()
                        .remove(i);

                break;
            }
        }
    }

    private void carregarHistorico() {

        mensagens
                .getChildren()
                .clear();

        if (contatoAtual == null) {
            return;
        }

        historicoConversas.remove(
                contatoAtual
        );

        clienteTCP.solicitarHistorico(
                contatoAtual
        );
    }

    private String removerStatus(
            String contato
    ) {

        if (contato == null) {
            return null;
        }

        String resultado =
                contato
                        .replace("🟢 ", "")
                        .replace("⚫ ", "");

        /*
         * Remove o contador visual do contato.
         *
         * Exemplo:
         *
         * Maria  3
         *
         * vira:
         *
         * Maria
         */
        int ultimaPosicaoEspaco =
                resultado.lastIndexOf("  ");

        if (ultimaPosicaoEspaco >= 0) {

            String possivelNumero =
                    resultado
                            .substring(
                                    ultimaPosicaoEspaco + 2
                            )
                            .trim();

            try {

                Integer.parseInt(
                        possivelNumero
                );

                resultado =
                        resultado.substring(
                                0,
                                ultimaPosicaoEspaco
                        );

            } catch (NumberFormatException e) {

                /*
                 * Não era um contador.
                 * Mantém o nome original.
                 */
            }
        }

        return resultado.trim();
    }

    public static void main(String[] args) {

        launch();
    }
}