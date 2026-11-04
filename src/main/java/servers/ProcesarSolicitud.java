package servers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import servers.business.models.Pedido;
import servers.business.models.Producto;
import servers.business.services.PedidoService;
import servers.dto.RespuestaErrorDTO;
import servers.dto.RespuestaOKDTO;

import java.util.List;
import java.util.NoSuchElementException;

public class ProcesarSolicitud {
    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;

    public ProcesarSolicitud(PedidoService pedidoService) {
        this(pedidoService, new ObjectMapper());
    }

    public ProcesarSolicitud(PedidoService pedidoService, ObjectMapper objectMapper) {
        this.pedidoService = pedidoService;
        this.objectMapper = objectMapper;
    }

    public String procesarPeticion(String jsonRecibido) {
        String servicio = "desconocido";
        try {
            if (jsonRecibido == null || jsonRecibido.trim().isEmpty()) {
                return objectMapper.writeValueAsString(new RespuestaErrorDTO("desconocido", "Petición vacía"));
            }

            JsonNode root = objectMapper.readTree(jsonRecibido);
            servicio = root.path("servicio").asText("desconocido");
            JsonNode parametros = root.path("parametros");

            return switch (servicio) {
                case "registrarPedido" -> procesarRegistrarPedido(servicio, parametros);
                case "consultarPedido" -> procesarConsultarPedido(servicio, parametros);
                case "listarPedidos" -> procesarListarPedidos(servicio);
                default -> objectMapper.writeValueAsString(
                        new RespuestaErrorDTO(servicio, "Servicio no soportado: " + servicio)
                );
            };

        } catch (NoSuchElementException | IllegalArgumentException e) {
            return serializarError(servicio, e.getMessage());
        } catch (Exception e) {
            return serializarError(servicio, "Error al procesar la solicitud: " + e.getMessage());
        }
    }

    private String procesarRegistrarPedido(String servicio, JsonNode parametros) throws Exception {
        String cliente = parametros.path("cliente").asText("");
        JsonNode productosNode = parametros.path("productos");

        List<Producto> productos = objectMapper.convertValue(
                productosNode,
                new TypeReference<>() {}
        );

        Pedido nuevoPedido = pedidoService.registrarPedido(cliente, productos);
        return objectMapper.writeValueAsString(new RespuestaOKDTO(servicio, nuevoPedido));
    }

    private String procesarConsultarPedido(String servicio, JsonNode parametros) throws Exception {
        if (!parametros.has("id")) {
            return serializarError(servicio, "El parámetro 'id' es requerido");
        }
        int id = parametros.path("id").asInt();
        Pedido pedido = pedidoService.consultarPedido(id);
        return objectMapper.writeValueAsString(new RespuestaOKDTO(servicio, pedido));
    }

    private String procesarListarPedidos(String servicio) throws Exception {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        return objectMapper.writeValueAsString(new RespuestaOKDTO(servicio, pedidos));
    }

    public String serializarError(String servicio, String mensaje) {
        try {
            return objectMapper.writeValueAsString(new RespuestaErrorDTO(servicio, mensaje));
        } catch (Exception e) {
            return "{\"servicio\":\"" + servicio + "\",\"estado\":\"ERROR\",\"mensaje\":\"" + mensaje + "\"}";
        }
    }

}
