package servers.repositories;

import servers.business.models.Pedido;
import java.util.List;

public interface PedidoRepository {
    int guardar(Pedido pedido);

    Pedido buscarPorId(int id);

    List<Pedido> listarTodos();
}
