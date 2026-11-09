package servers.network;

import servers.ProcesarSolicitud;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class AtenderSolicitud implements Runnable {

    private final Socket socket;
    private final ProcesarSolicitud procesarSolicitud;

    public AtenderSolicitud(Socket socket, ProcesarSolicitud procesarSolicitud) {
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
                System.out.println("[AtenderSolicitud] Recibiendo peticion: \n" + lineaPeticion);

                // PROCESAR
                String jsonRespuesta = procesarSolicitud.procesarPeticion(lineaPeticion);

                System.out.println("[AtenderSolicitud] Enviando respuesta: \n" + jsonRespuesta);
                salida.println(jsonRespuesta);
            }

        } catch (Exception e) {
            System.err.println("[AtenderSolicitud] Error en la conexión con el Broker: " + e.getMessage());
        }
    }
}
