package ChatServidor;

import java.util.ArrayList;
import java.util.List;

public class GerenciadorClientes {

    private final List<ClienteHandler> clientes = new ArrayList<>();

    public synchronized void adicionarCliente(ClienteHandler cliente) {

        clientes.add(cliente);

        System.out.println(
                "Cliente adicionado. Total conectado: "
                        + clientes.size()
        );
    }

    public synchronized void removerCliente(ClienteHandler cliente) {

        clientes.remove(cliente);

        System.out.println(
                "Cliente removido. Total conectado: "
                        + clientes.size()
        );
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
}