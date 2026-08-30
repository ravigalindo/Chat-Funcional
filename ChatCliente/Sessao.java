package ChatCliente;

public class Sessao {

    private Usuario usuarioLogado;

    public void iniciarSessao(
            Usuario usuario
    ) {

        this.usuarioLogado = usuario;
    }

    public Usuario getUsuarioLogado() {

        return usuarioLogado;
    }

    public void encerrarSessao() {

        usuarioLogado = null;
    }

    public boolean estaLogado() {

        return usuarioLogado != null;
    }
}