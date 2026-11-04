package servers.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrokerRegistrar {

    private final String brokerIp;
    private final int brokerPort;
    private final ObjectMapper objectMapper;

    public BrokerRegistrar(String brokerIp, int brokerPort) {
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.objectMapper = new ObjectMapper();
    }


    public boolean registrarServicios(List<String> nombresServicios, String serverIp, int serverPort) {
        boolean todosExitosos = true;
        for (String nombreServicio : nombresServicios) {
            boolean exito = registrarServicio(nombreServicio, serverIp, serverPort);
            if (!exito) {
                todosExitosos = false;
            }
        }
        return todosExitosos;
    }


    public boolean registrarServicio(String nombreServicio, String serverIp, int serverPort) {
        try (Socket socket = new Socket(brokerIp, brokerPort);
             PrintWriter salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("nombre", nombreServicio);
            parametros.put("ip", serverIp);
            parametros.put("puerto", serverPort);

            Map<String, Object> solicitud = new HashMap<>();
            solicitud.put("servicio", "registrar");
            solicitud.put("parametros", parametros);

            String jsonSolicitud = objectMapper.writeValueAsString(solicitud);
            salida.println(jsonSolicitud);

            String respuesta = entrada.readLine();
            if (respuesta != null) {
                JsonNode resNode = objectMapper.readTree(respuesta);
                String estado = resNode.path("estado").asText("");
                if ("OK".equalsIgnoreCase(estado)) {
                    System.out.println("[BrokerRegistrar] Servicio '" + nombreServicio + "' registrado correctamente con el Broker.");
                    return true;
                } else {
                    System.err.println("[BrokerRegistrar] Error al registrar '" + nombreServicio + "': " + respuesta);
                }
            }
        } catch (Exception e) {
            System.err.println("[BrokerRegistrar] No se pudo conectar al Broker en " + brokerIp + ":" + brokerPort + " para registrar '" + nombreServicio + "': " + e.getMessage());
        }
        return false;
    }
}
