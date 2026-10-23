package servers.repositories;

import servers.business.models.Pedido;

import java.util.List;
import java.util.Map;

public class PedidoArchivoCSV implements PedidoRepository{
    @Override
    public int guardar(Pedido pedido) {
        return 0;
    }

    @Override
    public Pedido buscarPorId(String id) {
        return null;
    }

    @Override
    public Map<String, Pedido> listarTodos() {
        return Map.of();
    }
}
