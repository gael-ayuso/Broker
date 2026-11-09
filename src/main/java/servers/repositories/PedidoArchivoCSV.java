package servers.repositories;

import servers.business.models.Pedido;
import servers.business.models.Producto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PedidoArchivoCSV implements PedidoRepository {
    private final String rutaArchivo;

    public PedidoArchivoCSV() {
        this("pedidos.csv");
    }

    public PedidoArchivoCSV(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    @Override
    public synchronized int guardar(Pedido pedido) {
        if (pedido == null || pedido.getId() == null) {
            return 0;
        }
        List<Pedido> pedidos = listarTodos();
        boolean actualizado = false;
        for (int i = 0; i < pedidos.size(); i++) {
            if (pedidos.get(i).getId().equals(pedido.getId())) {
                pedidos.set(i, pedido);
                actualizado = true;
                break;
            }
        }
        if (!actualizado) {
            pedidos.add(pedido);
        }

        File archivo = new File(rutaArchivo);
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            writer.println("id,cliente,subtotal,descuento,impuestos,total,estado,productos");
            for (Pedido p : pedidos) {
                writer.println(convertirALineaCSV(p));
            }
            return 1;
        } catch (IOException e) {
            System.err.println("Error al guardar pedido en CSV: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public synchronized Pedido buscarPorId(int id) {
        for (Pedido p : listarTodos()) {
            if (p.getId() != null && p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized List<Pedido> listarTodos() {
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        List<Pedido> pedidos = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("id,")) {
                    continue;
                }
                String[] partes = linea.split(",", 8);
                if (partes.length < 8) {
                    continue;
                }
                int id = Integer.parseInt(partes[0].trim());
                String cliente = partes[1].trim();
                double subtotal = Double.parseDouble(partes[2].trim());
                double descuento = Double.parseDouble(partes[3].trim());
                double impuestos = Double.parseDouble(partes[4].trim());
                double total = Double.parseDouble(partes[5].trim());
                String estado = partes[6].trim();
                String prodStr = partes[7].trim();

                List<Producto> productos = new ArrayList<>();
                if (!prodStr.isEmpty()) {
                    String[] prodItems = prodStr.split(";");
                    for (String item : prodItems) {
                        String[] datos = item.split(":");
                        if (datos.length == 4) {
                            productos.add(new Producto(
                                    datos[0],
                                    Double.parseDouble(datos[1]),
                                    Integer.parseInt(datos[2]),
                                    Integer.parseInt(datos[3])
                            ));
                        }
                    }
                }

                Pedido pedido = new Pedido(id, cliente, productos);
                pedido.setSubtotal(subtotal);
                pedido.setDescuento(descuento);
                pedido.setImpuestos(impuestos);
                pedido.setTotal(total);
                pedido.setEstado(estado);
                pedidos.add(pedido);
            }
        } catch (Exception e) {
            System.err.println("Error al leer archivo CSV: " + e.getMessage());
        }
        return pedidos;
    }

    private String convertirALineaCSV(Pedido pedido) {
        StringBuilder sbProd = new StringBuilder();
        if (pedido.getProductos() != null) {
            for (int i = 0; i < pedido.getProductos().size(); i++) {
                Producto p = pedido.getProductos().get(i);
                if (i > 0) sbProd.append(";");
                sbProd.append(p.getNombre()).append(":")
                      .append(String.format(Locale.US, "%.2f", p.getPrecio())).append(":")
                      .append(p.getCantidad()).append(":")
                      .append(p.getExistencia());
            }
        }
        return String.format(Locale.US, "%d,%s,%.2f,%.2f,%.2f,%.2f,%s,%s",
                pedido.getId(),
                pedido.getCliente(),
                pedido.getSubtotal(),
                pedido.getDescuento(),
                pedido.getImpuestos(),
                pedido.getTotal(),
                pedido.getEstado(),
                sbProd.toString()
        );
    }
}
