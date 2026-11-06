package client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcesarRespuesta {

    private final ObjectMapper objectMapper;

    public ProcesarRespuesta() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Construccion de peticiones JSON
    public String construirPeticionListarServicios() {
        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("servicio", "listar");
        peticion.put("parametros", Map.of());
        return serializar(peticion);
    }

    public String construirPeticionBuscarServicios(String criterio) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("buscar", criterio != null ? criterio : "");

        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("servicio", "listar");
        peticion.put("parametros", parametros);
        return serializar(peticion);
    }

    public String construirPeticionRegistrarPedido(String cliente, List<ProductoDTO> productos) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("cliente", cliente != null ? cliente : "");
        argumentos.put("productos", productos != null ? productos : List.of());

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("servicio", "registrarPedido");
        parametros.put("argumentos", argumentos);

        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("servicio", "ejecutar");
        peticion.put("parametros", parametros);
        return serializar(peticion);
    }

    public String construirPeticionConsultarPedido(int id) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("id", id);

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("servicio", "consultarPedido");
        parametros.put("argumentos", argumentos);

        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("servicio", "ejecutar");
        peticion.put("parametros", parametros);
        return serializar(peticion);
    }

    public String construirPeticionListarPedidos() {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("servicio", "listarPedidos");
        parametros.put("argumentos", Map.of());

        Map<String, Object> peticion = new LinkedHashMap<>();
        peticion.put("servicio", "ejecutar");
        peticion.put("parametros", parametros);
        return serializar(peticion);
    }

    // Validacion y extraccion de respuestas
    public boolean esExitosa(String jsonRespuesta) {
        if (jsonRespuesta == null || jsonRespuesta.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonRespuesta);
            return "OK".equalsIgnoreCase(root.path("estado").asText(""));
        } catch (Exception e) {
            return false;
        }
    }

    public String extraerMensajeError(String jsonRespuesta) {
        if (jsonRespuesta == null || jsonRespuesta.isBlank()) {
            return "Respuesta vacia del Broker.";
        }
        try {
            JsonNode root = objectMapper.readTree(jsonRespuesta);
            if (root.has("mensaje")) {
                return root.path("mensaje").asText();
            }
            if (root.has("error")) {
                return root.path("error").asText();
            }
            return "Error reportado por el Broker: " + jsonRespuesta;
        } catch (Exception e) {
            return "Error al interpretar respuesta: " + jsonRespuesta;
        }
    }

    public List<String> extraerServicios(String jsonRespuesta) {
        List<String> servicios = new ArrayList<>();
        if (jsonRespuesta == null || jsonRespuesta.isBlank()) {
            return servicios;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonRespuesta);
            JsonNode resultadoNode = root.path("resultado");
            if (resultadoNode.isArray()) {
                for (JsonNode item : resultadoNode) {
                    if (item.isTextual()) {
                        servicios.add(item.asText());
                    } else if (item.has("nombre")) {
                        servicios.add(item.path("nombre").asText());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al procesar servicios: " + e.getMessage());
        }
        return servicios;
    }

    public PedidoDTO extraerPedido(String jsonRespuesta) {
        if (jsonRespuesta == null || jsonRespuesta.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonRespuesta);
            JsonNode datosPedido = desempaquetarResultado(root.path("resultado"));
            if (datosPedido == null || datosPedido.isMissingNode() || datosPedido.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(datosPedido, PedidoDTO.class);
        } catch (Exception e) {
            System.err.println("Error al deserializar pedido: " + e.getMessage());
            return null;
        }
    }

    public List<PedidoDTO> extraerListaPedidos(String jsonRespuesta) {
        List<PedidoDTO> pedidos = new ArrayList<>();
        if (jsonRespuesta == null || jsonRespuesta.isBlank()) {
            return pedidos;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonRespuesta);
            JsonNode nodoLista = desempaquetarResultado(root.path("resultado"));
            if (nodoLista != null && nodoLista.isArray()) {
                pedidos = objectMapper.convertValue(nodoLista, new TypeReference<List<PedidoDTO>>() {});
            }
        } catch (Exception e) {
            System.err.println("Error al deserializar lista de pedidos: " + e.getMessage());
        }
        return pedidos;
    }

    // Formateo de texto para la consola
    public String formatearServicios(String jsonRespuesta) {
        if (!esExitosa(jsonRespuesta)) {
            return "ERROR al consultar servicios: " + extraerMensajeError(jsonRespuesta);
        }
        List<String> servicios = extraerServicios(jsonRespuesta);
        if (servicios.isEmpty()) {
            return "No hay servicios disponibles en el Broker.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SERVICIOS REGISTRADOS EN EL BROKER\n");
        sb.append("----------------------------------------\n");
        for (int i = 0; i < servicios.size(); i++) {
            sb.append((i + 1)).append(". ").append(servicios.get(i)).append("\n");
        }
        sb.append("----------------------------------------");
        return sb.toString();
    }

    public String formatearPedido(String jsonRespuesta) {
        if (!esExitosa(jsonRespuesta)) {
            return "ERROR: " + extraerMensajeError(jsonRespuesta);
        }
        PedidoDTO pedido = extraerPedido(jsonRespuesta);
        if (pedido == null) {
            return "No fue posible procesar los datos del pedido.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DETALLE DEL PEDIDO\n");
        sb.append("----------------------------------------\n");
        sb.append("ID Pedido:  ").append(pedido.id() != null ? pedido.id() : "N/A").append("\n");
        sb.append("Cliente:    ").append(pedido.cliente() != null ? pedido.cliente() : "").append("\n");
        sb.append("Estado:     ").append(pedido.estado() != null ? pedido.estado() : "").append("\n");
        sb.append("Productos:\n");

        if (pedido.productos() != null && !pedido.productos().isEmpty()) {
            for (ProductoDTO p : pedido.productos()) {
                sb.append(String.format("  - %s | Cantidad: %d | Precio: $%.2f | Stock: %d\n",
                        p.nombre(), p.cantidad(), p.precio(), p.existencia()));
            }
        } else {
            sb.append("  (Sin productos)\n");
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("Subtotal:   $%.2f\n", pedido.subtotal() != null ? pedido.subtotal() : 0.0));
        sb.append(String.format("Descuento:  $%.2f\n", pedido.descuento() != null ? pedido.descuento() : 0.0));
        sb.append(String.format("Impuestos:  $%.2f\n", pedido.impuestos() != null ? pedido.impuestos() : 0.0));
        sb.append(String.format("Total:      $%.2f\n", pedido.total() != null ? pedido.total() : 0.0));
        sb.append("----------------------------------------");
        return sb.toString();
    }

    public String formatearListaPedidos(String jsonRespuesta) {
        if (!esExitosa(jsonRespuesta)) {
            return "ERROR al listar pedidos: " + extraerMensajeError(jsonRespuesta);
        }
        List<PedidoDTO> pedidos = extraerListaPedidos(jsonRespuesta);
        if (pedidos.isEmpty()) {
            return "No hay pedidos registrados en el sistema.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("LISTA DE PEDIDOS REGISTRADOS (Total: %d)\n", pedidos.size()));
        sb.append("----------------------------------------\n");
        for (PedidoDTO p : pedidos) {
            double total = p.total() != null ? p.total() : 0.0;
            String estado = p.estado() != null ? p.estado() : "N/A";
            sb.append(String.format("Pedido #%d | Cliente: %s | Total: $%.2f | Estado: %s\n",
                    p.id(), p.cliente(), total, estado));
            if (p.productos() != null && !p.productos().isEmpty()) {
                for (ProductoDTO prod : p.productos()) {
                    sb.append(String.format("  - %s (x%d) @ $%.2f\n", prod.nombre(), prod.cantidad(), prod.precio()));
                }
            }
            sb.append("----------------------------------------\n");
        }
        return sb.toString().stripTrailing();
    }

    // Desempaqueta la respuesta del broker si viene envuelta en resultado.respuesta
    private JsonNode desempaquetarResultado(JsonNode resultadoNode) {
        if (resultadoNode == null || resultadoNode.isMissingNode() || resultadoNode.isNull()) {
            return null;
        }
        if (resultadoNode.has("respuesta")) {
            return resultadoNode.get("respuesta");
        }
        return resultadoNode;
    }

    private String serializar(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar objeto a JSON: " + e.getMessage(), e);
        }
    }
}
