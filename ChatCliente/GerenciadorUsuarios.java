package ChatCliente;

import java.util.ArrayList;
import java.util.List;

public class GerenciadorUsuarios {

    private final List<Usuario> usuarios;

    public GerenciadorUsuarios() {

        usuarios = new ArrayList<>();
    }

    // ==========================================
    // CADASTRAR USUÁRIO
    // ==========================================

    public boolean cadastrarUsuario(
            String nome,
            String senha
    ) {

        if (buscarUsuario(nome) != null) {
            return false;
        }

        Usuario usuario =
                new Usuario(
                        nome,
                        senha
                );

        usuarios.add(usuario);

        return true;
    }

    // ==========================================
    // AUTENTICAR USUÁRIO
    // ==========================================

    public boolean autenticar(
            String nome,
            String senha
    ) {

        Usuario usuario =
                buscarUsuario(nome);

        if (usuario == null) {
            return false;
        }

        return usuario.getSenha().equals(senha);
    }

    // ==========================================
// OBTER USUÁRIO AUTENTICADO
// ==========================================

public Usuario obterUsuarioAutenticado(
        String nome,
        String senha
) {

    Usuario usuario =
            buscarUsuario(nome);

    if (usuario == null) {
        return null;
    }

    if (!usuario.getSenha().equals(senha)) {
        return null;
    }

    return usuario;
}
    
// ==========================================
    // BUSCAR USUÁRIO
    // ==========================================

    private Usuario buscarUsuario(
            String nome
    ) {

        for (Usuario usuario : usuarios) {

            if (usuario.getNome()
                    .equals(nome)) {

                return usuario;
            }
        }

        return null;
    }
}