package servers.network;

import servers.ProcesarSolicitud;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class AtenderCliente implements Runnable {

    private final Socket socket;
    private final ProcesarSolicitud procesarSolicitud;

    public AtenderCliente(Socket socket, ProcesarSolicitud procesarSolicitud) {
        this.socket = socket;
        this.procesarSolicitud = procesarSolicitud;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String lineaPeticion;
            while ((lineaPeticion = entrada.readLine()) != null) {
                if (lineaPeticion.trim().isEmpty()) {
                    continue;
                }

                // PROCESAR
                String jsonRespuesta = procesarSolicitud.procesarPeticion(lineaPeticion);

                salida.println(jsonRespuesta);
            }

        } catch (Exception e) {
            System.err.println("[ClientConnectionHandler] Error en la conexión con el Broker: " + e.getMessage());
        }
    }
}
