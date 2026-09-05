package ChatServidor;

import java.util.ArrayList;
import java.util.List;

public class GerenciadorClientes {

    private final List<ClienteHandler> clientes =
            new ArrayList<>();

    public synchronized void adicionarCliente(
            ClienteHandler cliente
    ) {

        clientes.add(cliente);

        System.out.println(
                "Cliente adicionado. Total conectado: "
                        + clientes.size()
        );
    }

    public synchronized void removerCliente(
            ClienteHandler cliente
    ) {

        clientes.remove(cliente);

        System.out.println(
                "Cliente removido. Total conectado: "
                        + clientes.size()
        );

        String nomeUsuario =
                cliente.getNomeUsuario();

        if (nomeUsuario != null) {

            enviarParaTodos(
                    "OFFLINE|" + nomeUsuario
            );
        }
    }

    public synchronized ClienteHandler encontrarCliente(
            String nomeUsuario
    ) {

        for (ClienteHandler cliente : clientes) {

            if (nomeUsuario.equalsIgnoreCase(
                    cliente.getNomeUsuario()
            )) {

                return cliente;
            }
        }

        return null;
    }

    public synchronized List<ClienteHandler> getClientes() {

        return new ArrayList<>(clientes);
    }

    public synchronized void enviarParaTodos(
            String mensagem
    ) {

        for (ClienteHandler cliente : clientes) {

            cliente.enviarMensagem(
                    mensagem
            );
        }
    }

    public synchronized String obterUsuariosOnline() {

        StringBuilder usuarios =
                new StringBuilder();

        for (ClienteHandler cliente : clientes) {

            String nome =
                    cliente.getNomeUsuario();

            if (nome == null) {
                continue;
            }

            if (usuarios.length() > 0) {
                usuarios.append(",");
            }

            usuarios.append(nome);
        }

        return usuarios.toString();
    }
}