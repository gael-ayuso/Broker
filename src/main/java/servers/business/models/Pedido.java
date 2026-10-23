package servers.business.models;


import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private final String id;
    private final List<Producto> productos;
    private String cliente;
    private Double subtotal;
    private Double descuento;
    private Double impuestos;
    private Double total;
    private String estado;

    public Pedido(String id, String cliente, List<Producto> productos) {
        this.id = id;
        this.cliente = cliente;
        this.productos = productos;
        this.subtotal = 0.0;
        this.descuento = 0.0;
        this.impuestos = 0.0;
        this.total = 0.0;
        this.estado = "Pendiente";
    }

    public Pedido(String id, String cliente) {
        this(id, cliente, new ArrayList<>());
    }

    public String getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Double getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(Double impuestos) {
        this.impuestos = impuestos;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean agregarProducto(Producto producto) {
        if (producto == null) return false;
        return productos.add(producto);
    }
}
