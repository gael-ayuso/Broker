package servers.repositories;


import servers.business.models.Pedido;

import java.util.List;
import java.util.Map;

public interface PedidoRepository {
    int guardar(Pedido pedido);

    Pedido buscarPorId(String id);

    Map<String, Pedido> listarTodos();
}
