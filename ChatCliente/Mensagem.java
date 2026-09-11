package ChatCliente;

public class Mensagem {

    private int id;
    private String remetente;
    private String conteudo;
    private boolean enviadaPorMim;

    public Mensagem(
            String remetente,
            String conteudo,
            boolean enviadaPorMim
    ) {
        this(
                0,
                remetente,
                conteudo,
                enviadaPorMim
        );
    }

    public Mensagem(
            int id,
            String remetente,
            String conteudo,
            boolean enviadaPorMim
    ) {
        this.id = id;
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.enviadaPorMim = enviadaPorMim;
    }

    public int getId() {
        return id;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public boolean isEnviadaPorMim() {
        return enviadaPorMim;
    }
}