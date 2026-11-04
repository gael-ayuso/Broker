package servers.business.services;

import servers.business.models.Pedido;
import servers.business.models.Producto;
import servers.repositories.PedidoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reglas de negocio:
 * - el cliente no puede estar vacío;
 * - debe existir al menos un producto;
 * - la cantidad debe ser mayor que cero;
 * - no puede superar la existencia;
 * - subtotal = Σ precio × cantidad;
 * - descuento del 10 % si subtotal ≥ $1,000;
 * - impuesto del 16 % sobre subtotal − descuento;
 * - estado final = PROCESADO.
 */
public class PedidoService {

    private static final double UMBRAL_DESCUENTO = 1000.0;
    private static final double PORCENTAJE_DESCUENTO = 0.10;
    private static final double TASA_IVA = 0.16;

    private final PedidoRepository repository;
    private final AtomicInteger siguienteId = new AtomicInteger(1);

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public synchronized Pedido registrarPedido(String cliente, List<Producto> productos) {
        Pedido pedido = new Pedido(siguienteId.get(), cliente, productos);
        validarDatos(pedido);
        comprobarDisponibilidad(pedido);
        calcularSubtotal(pedido);
        aplicarDescuento(pedido);
        calcularImpuestos(pedido);
        calcularTotal(pedido);
        confirmarPedido(pedido);
        guardarPedido(pedido);

        // Incrementamos el ID una vez confirmado
        siguienteId.incrementAndGet();
        return pedido;
    }

    public Pedido consultarPedido(int id) {
        Pedido pedido = repository.buscarPorId(id);
        if (pedido == null) {
            throw new NoSuchElementException("Pedido no encontrado");
        }
        return pedido;
    }

    public List<Pedido> listarPedidos() {
        return repository.listarTodos();
    }

    private void validarDatos(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser nulo.");
        }
        if (pedido.getCliente() == null || pedido.getCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("El cliente no puede estar vacío.");
        }
        List<Producto> productos = pedido.getProductos();
        if (productos == null || productos.isEmpty()) {
            pedido.setEstado("Pedido sin productos.");
            throw new IllegalArgumentException("Debe existir al menos un producto.");
        }
        for (Producto p : productos) {
            if (p == null || p.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero para cada producto.");
            }
        }
        pedido.setEstado("Pedido validado.");
    }

    private void comprobarDisponibilidad(Pedido pedido) {
        Map<String, Integer> cantidadesTotales = new HashMap<>();
        for (Producto p : pedido.getProductos()) {
            cantidadesTotales.put(p.getNombre(),
                    cantidadesTotales.getOrDefault(p.getNombre(), 0) + p.getCantidad());
        }

        for (Producto p : pedido.getProductos()) {
            int solicitada = cantidadesTotales.get(p.getNombre());
            if (solicitada > p.getExistencia()) {
                pedido.setEstado("Pedido con productos no disponibles.");
                // Mensaje exacto según especificación JSON (errores/06_existencia_insuficiente_respuesta.json)
                throw new IllegalArgumentException("Existencia insuficiente");
            }
        }
    }

    private void calcularSubtotal(Pedido pedido) {
        double subtotal = 0.0;
        for (Producto p : pedido.getProductos()) {
            subtotal += p.getPrecio() * p.getCantidad();
        }
        pedido.setSubtotal(subtotal);
    }

    private void aplicarDescuento(Pedido pedido) {
        if (pedido.getSubtotal() >= UMBRAL_DESCUENTO) {
            double descuento = pedido.getSubtotal() * PORCENTAJE_DESCUENTO;
            pedido.setDescuento(descuento);
        } else {
            pedido.setDescuento(0.0);
        }
    }

    private void calcularImpuestos(Pedido pedido) {
        double baseImponible = Math.max(0, pedido.getSubtotal() - pedido.getDescuento());
        double impuestos = baseImponible * TASA_IVA;
        pedido.setImpuestos(impuestos);
    }

    private void calcularTotal(Pedido pedido) {
        double total = (pedido.getSubtotal() - pedido.getDescuento()) + pedido.getImpuestos();
        pedido.setTotal(total);
    }

    private void confirmarPedido(Pedido pedido) {
        pedido.setEstado("PROCESADO");
    }

    private void guardarPedido(Pedido pedido) {
        repository.guardar(pedido);
    }
}
