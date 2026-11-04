package servers.repositories;

import servers.business.models.Pedido;

import java.util.ArrayList;
import java.util.List;

public class PedidoArchivoCSV implements PedidoRepository {
    private final String rutaArchivo;

    public PedidoArchivoCSV() {
        this("pedidos.csv");
    }

    public PedidoArchivoCSV(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    @Override
    public int guardar(Pedido pedido) {
        // TODO: Implementar persistencia en archivo CSV si es requerido
        return 0;
    }

    @Override
    public Pedido buscarPorId(int id) {
        // TODO: Implementar búsqueda en archivo CSV si es requerido
        return null;
    }

    @Override
    public List<Pedido> listarTodos() {
        // TODO: Implementar listado desde archivo CSV si es requerido
        return new ArrayList<>();
    }
}
