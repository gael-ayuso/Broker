package broker;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.*;

public class Broker {

    // Directorio de servicios: Llave = nombre del servicio, Valor = Info de conexión
    private final Map<String, ServerInfo> directorioServicios = new ConcurrentHashMap<>();
    private int idRegistroContador = 1;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    private static class ServerInfo {
        String ip;
        int puerto;
        ServerInfo(String ip, int puerto) {
            this.ip = ip;
            this.puerto = puerto;
        }
    }

    public static void main(String[] args) {
        int puerto = 9000;
        if (args.length == 1) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: El puerto debe ser un número entero.");
                return;
            }
        } else if (args.length > 1) {
            System.out.println("Uso: java broker.Broker [puerto]");
            return;
        }

        Broker broker = new Broker();
        broker.iniciarServidor(puerto);
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

    public void iniciarServidor(int puerto) {
        running = true;
        try (ServerSocket ss = new ServerSocket(puerto)) {
            this.serverSocket = ss;
            System.out.println("Broker iniciado. Escuchando en el puerto: " + puerto);

            // bucle para aceptar peticiones simultáneas
            while (running) {
                try {
                    Socket clientSocket = ss.accept();
                    // Cada conexión se atiende en un hilo nuevo para no bloquear el Broker
                    new Thread(() -> atenderConexion(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error al aceptar conexión en el Broker: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error al iniciar el Broker: " + e.getMessage());
            }
        }
    }

    private void atenderConexion(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            String linea = in.readLine();
            if (linea != null && !linea.trim().isEmpty()) {
                
                JsonObject respuesta = new JsonObject();
                try {
                    JsonObject peticion = JsonParser.parseString(linea).getAsJsonObject();
                    if (!peticion.has("servicio") || peticion.get("servicio").isJsonNull()) {
                        respuesta.addProperty("estado", "ERROR");
                        respuesta.addProperty("mensaje", "Parámetro obligatorio ausente: servicio");
                    } else {
                        String servicio = peticion.get("servicio").getAsString();
                        respuesta.addProperty("servicio", servicio);

                        JsonObject parametros = peticion.has("parametros") && peticion.get("parametros").isJsonObject()
                                ? peticion.getAsJsonObject("parametros") 
                                : new JsonObject();

                        // Enrutador principal del Broker
                        switch (servicio) {
                            case "registrar":
                                registerServer(parametros, respuesta);
                                break;
                            case "listar":
                                listServers(parametros, respuesta);
                                break;
                            case "ejecutar":
                                runService(parametros, respuesta);
                                break;
                            default:
                                respuesta.addProperty("estado", "ERROR");
                                respuesta.addProperty("mensaje", "Servicio del Broker inexistente");
                        }
                    }
                } catch (JsonSyntaxException e) {
                    respuesta.addProperty("estado", "ERROR");
                    respuesta.addProperty("mensaje", "JSON inválido");
                }

                out.write(respuesta.toString() + "\n");
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Error en la conexión: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // 1. Servicio: Registrar
    private void registerServer(JsonObject parametros, JsonObject respuesta) {
        if (!parametros.has("nombre") || !parametros.has("ip") || !parametros.has("puerto")) {
            respuesta.addProperty("estado", "ERROR");
            respuesta.addProperty("mensaje", "Parámetro obligatorio ausente");
            return;
        }

        String nombre = parametros.get("nombre").getAsString();
        String ip = parametros.get("ip").getAsString();
        int puerto = parametros.get("puerto").getAsInt();

        directorioServicios.put(nombre, new ServerInfo(ip, puerto));

        respuesta.addProperty("estado", "OK");
        JsonObject resultado = new JsonObject();
        resultado.addProperty("id", idRegistroContador++);
        respuesta.add("resultado", resultado);
        
        System.out.println("Servicio registrado: " + nombre + " -> " + ip + ":" + puerto);
    }

    // 2. Servicio: Listar y Buscar
    private void listServers(JsonObject parametros, JsonObject respuesta) {
        JsonArray lista = new JsonArray();
        String busqueda = parametros.has("buscar") ? parametros.get("buscar").getAsString().toLowerCase() : null;

        for (String nombreServicio : directorioServicios.keySet()) {
            if (busqueda == null || nombreServicio.toLowerCase().contains(busqueda)) {
                JsonObject item = new JsonObject();
                item.addProperty("nombre", nombreServicio);
                lista.add(item);
            }
        }

        respuesta.addProperty("estado", "OK");
        respuesta.add("resultado", lista);
    }

    // 3. Servicio: Ejecutar (El Broker actúa como cliente hacia el Servidor)
    private void runService(JsonObject parametros, JsonObject respuestaBroker) {
        if (!parametros.has("servicio") || !parametros.has("argumentos")) {
            respuestaBroker.addProperty("estado", "ERROR");
            respuestaBroker.addProperty("mensaje", "Parámetro obligatorio ausente");
            return;
        }

        String nombreServicio = parametros.get("servicio").getAsString();
        
        // Localizar el servicio[cite: 25]
        if (!directorioServicios.containsKey(nombreServicio)) {
            respuestaBroker.addProperty("estado", "ERROR");
            respuestaBroker.addProperty("mensaje", "Servicio no registrado");
            return;
        }

        ServerInfo info = directorioServicios.get(nombreServicio);

        // Crear una nueva conexión TCP hacia el Servidor real
        try (Socket serverSocket = new Socket(info.ip, info.puerto);
             BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            // Construir el JSON que el Broker enviará al Servidor
            JsonObject peticionServidor = new JsonObject();
            peticionServidor.addProperty("servicio", nombreServicio);
            peticionServidor.add("parametros", parametros.get("argumentos"));

            out.write(peticionServidor.toString() + "\n");
            out.flush();

            // Leer la respuesta del Servidor
            String respuestaStr = in.readLine();
            JsonObject respuestaServidor = JsonParser.parseString(respuestaStr).getAsJsonObject();

            // Evaluar la respuesta del servidor real
            if (respuestaServidor.has("estado") && "ERROR".equalsIgnoreCase(respuestaServidor.get("estado").getAsString())) {
                // Si el servidor devolvió un error, el broker pasa el error al cliente
                respuestaBroker.addProperty("estado", "ERROR");
                respuestaBroker.addProperty("mensaje", respuestaServidor.has("mensaje") ? respuestaServidor.get("mensaje").getAsString() : "Error en servidor");
            } else {
                // Si fue OK, se empaqueta la respuesta
                respuestaBroker.addProperty("estado", "OK");
                JsonObject resultado = new JsonObject();
                resultado.addProperty("servicio", nombreServicio);
                resultado.add("respuesta", respuestaServidor.get("resultado"));
                respuestaBroker.add("resultado", resultado);
            }

        } catch (Exception e) {
            respuestaBroker.addProperty("estado", "ERROR");
            respuestaBroker.addProperty("mensaje", "No es posible contactar al servidor");
        }
    }
}
