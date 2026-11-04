import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import servers.ProcesarSolicitud;
import servers.business.services.PedidoService;
import servers.repositories.PedidoRepositoryMemoria;

import static org.junit.jupiter.api.Assertions.*;

public class PedidoSerializeTest {

    private ProcesarSolicitud procesarSolicitud;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PedidoRepositoryMemoria repository = new PedidoRepositoryMemoria();
        PedidoService pedidoService = new PedidoService(repository);
        procesarSolicitud = new ProcesarSolicitud(pedidoService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Debe registrar un pedido y devolver JSON con subtotal, descuento, impuestos y estado PROCESADO")
    void testRegistrarPedidoExitoso() throws Exception {
        String peticionJson = """
        {
          "servicio": "registrarPedido",
          "parametros": {
            "cliente": "Ana López",
            "productos": [
              {
                "nombre": "Producto A",
                "precio": 600.0,
                "cantidad": 2,
                "existencia": 10
              }
            ]
          }
        }
        """;

        String respuestaJson = procesarSolicitud.procesarPeticion(peticionJson);
        assertNotNull(respuestaJson);

        JsonNode root = objectMapper.readTree(respuestaJson);
        assertEquals("registrarPedido", root.path("servicio").asText());
        assertEquals("OK", root.path("estado").asText());

        JsonNode resultado = root.path("resultado");
        assertEquals(1, resultado.path("id").asInt());
        assertEquals("Ana López", resultado.path("cliente").asText());
        assertEquals(1200.0, resultado.path("subtotal").asDouble());
        assertEquals(120.0, resultado.path("descuento").asDouble());
        assertEquals(172.8, resultado.path("impuestos").asDouble());
        assertEquals(1252.8, resultado.path("total").asDouble());
        assertEquals("PROCESADO", resultado.path("estado").asText());
    }

    @Test
    @DisplayName("Debe devolver error cuando la cantidad supera la existencia")
    void testRegistrarPedidoExistenciaInsuficiente() throws Exception {
        String peticionJson = """
        {
          "servicio": "registrarPedido",
          "parametros": {
            "cliente": "Ana López",
            "productos": [
              {
                "nombre": "Producto A",
                "precio": 600.0,
                "cantidad": 20,
                "existencia": 10
              }
            ]
          }
        }
        """;

        String respuestaJson = procesarSolicitud.procesarPeticion(peticionJson);
        JsonNode root = objectMapper.readTree(respuestaJson);

        assertEquals("registrarPedido", root.path("servicio").asText());
        assertEquals("ERROR", root.path("estado").asText());
        assertEquals("Existencia insuficiente", root.path("mensaje").asText());
    }

    @Test
    @DisplayName("Debe consultar un pedido existente por id")
    void testConsultarPedidoExistente() throws Exception {
        testRegistrarPedidoExitoso();

        String peticionJson = """
        {
          "servicio": "consultarPedido",
          "parametros": {
            "id": 1
          }
        }
        """;

        String respuestaJson = procesarSolicitud.procesarPeticion(peticionJson);
        JsonNode root = objectMapper.readTree(respuestaJson);

        assertEquals("consultarPedido", root.path("servicio").asText());
        assertEquals("OK", root.path("estado").asText());
        assertEquals(1, root.path("resultado").path("id").asInt());
    }

    @Test
    @DisplayName("Debe devolver error Pedido no encontrado al consultar un id inexistente")
    void testConsultarPedidoInexistente() throws Exception {
        String peticionJson = """
        {
          "servicio": "consultarPedido",
          "parametros": {
            "id": 999
          }
        }
        """;

        String respuestaJson = procesarSolicitud.procesarPeticion(peticionJson);
        JsonNode root = objectMapper.readTree(respuestaJson);

        assertEquals("consultarPedido", root.path("servicio").asText());
        assertEquals("ERROR", root.path("estado").asText());
        assertEquals("Pedido no encontrado", root.path("mensaje").asText());
    }

    @Test
    @DisplayName("Debe listar pedidos registrados como un array JSON")
    void testListarPedidos() throws Exception {
        testRegistrarPedidoExitoso();

        String peticionJson = """
        {
          "servicio": "listarPedidos",
          "parametros": {}
        }
        """;

        String respuestaJson = procesarSolicitud.procesarPeticion(peticionJson);
        JsonNode root = objectMapper.readTree(respuestaJson);

        assertEquals("listarPedidos", root.path("servicio").asText());
        assertEquals("OK", root.path("estado").asText());
        assertTrue(root.path("resultado").isArray());
        assertEquals(1, root.path("resultado").size());
    }
}
