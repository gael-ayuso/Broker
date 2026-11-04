package servers.repositories;

import servers.business.models.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PedidoRepositoryMemoria implements PedidoRepository {
    private final ConcurrentMap<Integer, Pedido> almacenamiento = new ConcurrentHashMap<>();

    @Override
    public int guardar(Pedido pedido) {
        if (pedido == null || pedido.getId() == null) {
            return 0;
        }
        almacenamiento.put(pedido.getId(), pedido);
        return 1;
    }

    @Override
    public Pedido buscarPorId(int id) {
        return almacenamiento.get(id);
    }

    @Override
    public List<Pedido> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }
}
