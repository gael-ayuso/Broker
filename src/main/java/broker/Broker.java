package broker;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.*; // Asegúrate de tener Gson en tu pom.xml

public class Broker {

    // Directorio de servicios: Llave = nombre del servicio, Valor = Info de conexión
    private final Map<String, ServerInfo> directorioServicios = new ConcurrentHashMap<>();
    private int idRegistroContador = 1;

    private static class ServerInfo {
        String ip;
        int puerto;
        ServerInfo(String ip, int puerto) {
            this.ip = ip;
            this.puerto = puerto;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java broker.Broker <puerto>");
            return;
        }
        
        int puerto = Integer.parseInt(args[0]);
        Broker broker = new Broker();
        broker.iniciarServidor(puerto);
    }

    public void iniciarServidor(int puerto) {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Broker iniciado. Escuchando en el puerto: " + puerto);

            // bucle para aceptar peticiones simultáneas
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Cada conexión se atiende en un hilo nuevo para no bloquear el Broker
                new Thread(() -> atenderConexion(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el Broker: " + e.getMessage());
        }
    }

    private void atenderConexion(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
        ) {
            String linea = in.readLine(); // Leer hasta el \n[cite: 23, 26]
            if (linea != null && !linea.trim().isEmpty()) {
                
                JsonObject respuesta = new JsonObject();
                try {
                    JsonObject peticion = JsonParser.parseString(linea).getAsJsonObject();
                    String servicio = peticion.get("servicio").getAsString();
                    respuesta.addProperty("servicio", servicio);

                    JsonObject parametros = peticion.has("parametros") 
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
            try { socket.close(); } catch (IOException e) {}
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
             BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream(), "UTF-8"))) {

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
            if (respuestaServidor.get("estado").getAsString().equals("ERROR")) {
                // Si el servidor devolvió un error, el broker pasa el error al cliente
                respuestaBroker.addProperty("estado", "ERROR");
                respuestaBroker.addProperty("mensaje", respuestaServidor.get("mensaje").getAsString());
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
