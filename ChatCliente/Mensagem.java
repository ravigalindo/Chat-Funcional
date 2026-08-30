package ChatCliente;

public class Mensagem {

    private final String remetente;
    private final String conteudo;
    private final boolean enviadaPorMim;

    public Mensagem(
            String remetente,
            String conteudo,
            boolean enviadaPorMim
    ) {
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.enviadaPorMim = enviadaPorMim;
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