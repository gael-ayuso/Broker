package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

    private final String brokerIp;
    private final int brokerPort;
    private final ProcesarRespuesta procesarRespuesta;

    public Client(String brokerIp, int brokerPort) {
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.procesarRespuesta = new ProcesarRespuesta();
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    // Comunicacion TCP con el Broker
    public String enviarSolicitud(String jsonSolicitud) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(brokerIp, brokerPort), 10000);
            socket.setSoTimeout(10000);

            try (PrintWriter salida = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                salida.println(jsonSolicitud);
                String respuesta = entrada.readLine();
                if (respuesta == null) {
                    throw new IOException("El Broker cerro la conexion sin responder.");
                }
                return respuesta;
            }
        }
    }

    // Operaciones del cliente
    public String listarServicios() {
        try {
            String peticion = procesarRespuesta.construirPeticionListarServicios();
            String respuesta = enviarSolicitud(peticion);
            return procesarRespuesta.formatearServicios(respuesta);
        } catch (IOException e) {
            return "Error de red: No se pudo conectar al Broker (" + e.getMessage() + ")";
        }
    }

    public String buscarServicio(String criterio) {
        try {
            String peticion = procesarRespuesta.construirPeticionBuscarServicios(criterio);
            String respuesta = enviarSolicitud(peticion);
            return procesarRespuesta.formatearServicios(respuesta);
        } catch (IOException e) {
            return "Error de red: No se pudo conectar al Broker (" + e.getMessage() + ")";
        }
    }

    public String registrarPedido(String cliente, List<ProductoDTO> productos) {
        try {
            String peticion = procesarRespuesta.construirPeticionRegistrarPedido(cliente, productos);
            String respuesta = enviarSolicitud(peticion);
            return procesarRespuesta.formatearPedido(respuesta);
        } catch (IOException e) {
            return "Error de red: No se pudo conectar al Broker (" + e.getMessage() + ")";
        }
    }

    public String consultarPedido(int id) {
        try {
            String peticion = procesarRespuesta.construirPeticionConsultarPedido(id);
            String respuesta = enviarSolicitud(peticion);
            return procesarRespuesta.formatearPedido(respuesta);
        } catch (IOException e) {
            return "Error de red: No se pudo conectar al Broker (" + e.getMessage() + ")";
        }
    }

    public String listarPedidos() {
        try {
            String peticion = procesarRespuesta.construirPeticionListarPedidos();
            String respuesta = enviarSolicitud(peticion);
            return procesarRespuesta.formatearListaPedidos(respuesta);
        } catch (IOException e) {
            return "Error de red: No se pudo conectar al Broker (" + e.getMessage() + ")";
        }
    }

    // Interfaz por consola
    public void iniciar() {
        iniciar(System.in, System.out);
    }

    public void iniciar(InputStream in, PrintStream out) {
        Scanner scanner = new Scanner(in);
        boolean continuar = true;

        out.println("\nCLIENTE DE PEDIDOS (BROKER)");
        out.println("----------------------------------------");
        out.println("Broker: " + brokerIp + ":" + brokerPort);
        out.println("----------------------------------------");

        while (continuar) {
            mostrarMenu(out);
            int opcion = leerEntero(scanner, out, "Seleccione una opcion: ", 1, 3);

            switch (opcion) {
                case 1 -> menuListarYEjecutar(scanner, out);
                case 2 -> menuBuscarYEjecutar(scanner, out);
                case 3 -> {
                    continuar = false;
                    out.println("\nSaliendo del cliente.");
                }
            }

            if (continuar) {
                out.print("\nPresione Enter para volver al menu principal...");
                scanner.nextLine();
            }
        }
    }

    private void mostrarMenu(PrintStream out) {
        out.println("\nMENU PRINCIPAL");
        out.println("----------------------------------------");
        out.println("1. Listar servicios disponibles en el Broker");
        out.println("2. Buscar servicio en el Broker");
        out.println("3. Salir");
        out.println("----------------------------------------");
    }

    // --- LOGICA DE DESCUBRIMIENTO Y EJECUCION ---

    private void menuListarYEjecutar(Scanner scanner, PrintStream out) {
        out.println("\n[Consultando servicios en el Broker...]");
        out.println(listarServicios());
        seleccionarYEjecutar(scanner, out);
    }

    private void menuBuscarYEjecutar(Scanner scanner, PrintStream out) {
        out.println("\nBUSQUEDA DE SERVICIOS");
        out.println("----------------------------------------");
        String criterio = leerTexto(scanner, out, "Termino a buscar: ");
        out.println("\n[Buscando servicios...]");
        out.println(buscarServicio(criterio));
        seleccionarYEjecutar(scanner, out);
    }

    private void seleccionarYEjecutar(Scanner scanner, PrintStream out) {
        out.println("\nEscriba el nombre exacto del servicio que desea ejecutar");
        out.println("(o presione Enter sin escribir nada para cancelar y volver):");
        out.print("Servicio: ");
        String servicio = scanner.nextLine().trim();

        if (servicio.isEmpty()) {
            return; // El usuario canceló la acción
        }

        // Ruteador local del cliente hacia las interfaces de captura
        switch (servicio) {
            case "registrarPedido" -> menuRegistrarPedido(scanner, out);
            case "consultarPedido" -> menuConsultarPedido(scanner, out);
            case "listarPedidos" -> menuListarPedidos(out);
            default -> out.println("\n El servicio '" + servicio + "' no esta implementado en la interfaz de este cliente.");
        }
    }

    // --- MENUS DE CAPTURA DE DATOS PARA LOS SERVICIOS ---

    private void menuRegistrarPedido(Scanner scanner, PrintStream out) {
        out.println("\n>>> EJECUTANDO: registrarPedido");
        out.println("----------------------------------------");
        String cliente = leerTexto(scanner, out, "Nombre del cliente: ");

        List<ProductoDTO> productos = new ArrayList<>();
        boolean agregarMas = true;
        int contador = 1;

        while (agregarMas) {
            out.println("\nProducto #" + contador + ":");
            String nombreProd = leerTexto(scanner, out, "Nombre: ");
            double precio = leerDouble(scanner, out, "Precio unitario: ", 0.01);
            int cantidad = leerEntero(scanner, out, "Cantidad: ", 1, Integer.MAX_VALUE);
            int existencia = leerEntero(scanner, out, "Existencia disponible: ", 0, Integer.MAX_VALUE);

            productos.add(new ProductoDTO(nombreProd, precio, cantidad, existencia));
            contador++;

            String respuesta = leerTexto(scanner, out, "Agregar otro producto? (s/n): ");
            agregarMas = respuesta.trim().equalsIgnoreCase("s") || respuesta.trim().equalsIgnoreCase("si");
        }

        out.println("\n[Enviando pedido al Broker...]");
        out.println(registrarPedido(cliente, productos));
    }

    private void menuConsultarPedido(Scanner scanner, PrintStream out) {
        out.println("\n>>> EJECUTANDO: consultarPedido");
        out.println("----------------------------------------");
        int id = leerEntero(scanner, out, "ID del pedido: ", 1, Integer.MAX_VALUE);

        out.println("\n[Consultando pedido...]");
        out.println(consultarPedido(id));
    }

    private void menuListarPedidos(PrintStream out) {
        out.println("\n>>> EJECUTANDO: listarPedidos");
        out.println("----------------------------------------");
        out.println("\n[Consultando lista de pedidos al Broker...]");
        out.println(listarPedidos());
    }

    // --- VALIDACION DE ENTRADA DE DATOS ---

    private int leerEntero(Scanner scanner, PrintStream out, String mensaje, int min, int max) {
        while (true) {
            out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(linea);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                out.println("Ingrese un numero entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                out.println("Entrada invalida. Ingrese un numero entero.");
            }
        }
    }

    private double leerDouble(Scanner scanner, PrintStream out, String mensaje, double min) {
        while (true) {
            out.print(mensaje);
            String linea = scanner.nextLine().trim().replace(',', '.');
            try {
                double valor = Double.parseDouble(linea);
                if (valor >= min) {
                    return valor;
                }
                out.println("El valor debe ser mayor o igual a " + min + ".");
            } catch (NumberFormatException e) {
                out.println("Entrada invalida. Ingrese un valor numerico.");
            }
        }
    }

    private String leerTexto(Scanner scanner, PrintStream out, String mensaje) {
        while (true) {
            out.print(mensaje);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) {
                return linea;
            }
            out.println("Este campo no puede estar vacio.");
        }
    }

    // --- METODO MAIN ---
    // Formato: java client.Client 127.0.0.1:9000
    public static void main(String[] args) {
        String brokerIp = "127.0.0.1";
        int brokerPort = 9000;

        if (args.length == 1) {
            String param = args[0].trim();
            if (param.contains(":")) {
                String[] partes = param.split(":", 2);
                brokerIp = partes[0].trim();
                try {
                    brokerPort = Integer.parseInt(partes[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Error: Puerto invalido.");
                    System.exit(1);
                    return;
                }
            } else {
                System.err.println("Uso: java client.Client <ipBroker:puertoBroker>");
                System.exit(1);
                return;
            }
        } else if (args.length == 2) {
            brokerIp = args[0].trim();
            try {
                brokerPort = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: Puerto invalido.");
                System.exit(1);
                return;
            }
        } else if (args.length != 0) {
            System.err.println("Uso: java client.Client <ipBroker:puertoBroker>");
            System.exit(1);
            return;
        }

        Client cliente = new Client(brokerIp, brokerPort);
        cliente.iniciar();
    }
}
