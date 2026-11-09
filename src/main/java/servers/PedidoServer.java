package servers;

import servers.business.services.PedidoService;
import servers.network.BrokerRegistrar;
import servers.network.AtenderSolicitud;
import servers.repositories.PedidoArchivoCSV;
import servers.repositories.PedidoRepositoryMemoria;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor de Pedidos.
 * Se encarga de:
 * 1. Inicializar la infraestructura de red del servidor.
 * 2. Registrar los servicios disponibles ante el Broker central.
 * 3. Escuchar conexiones TCP entrantes y delegar el procesamiento al Serializador.
 */
public class PedidoServer {

    public static final List<String> SERVICIOS_DISPONIBLES = List.of(
            "registrarPedido",
            "consultarPedido",
            "listarPedidos"
    );

    private final int serverPort;
    private final String brokerIp;
    private final int brokerPort;
    private final ProcesarSolicitud procesarSolicitud;
    private final ExecutorService threadPool;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public PedidoServer(int serverPort, String brokerIp, int brokerPort) {
        this(serverPort, brokerIp, brokerPort, new ProcesarSolicitud(new PedidoService(new PedidoArchivoCSV())));
    }

    public PedidoServer(int serverPort, String brokerIp, int brokerPort, PedidoService pedidoService) {
        this(serverPort, brokerIp, brokerPort, new ProcesarSolicitud(pedidoService));
    }

    public PedidoServer(int serverPort, String brokerIp, int brokerPort, ProcesarSolicitud procesarSolicitud) {
        this.serverPort = serverPort;
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.procesarSolicitud = procesarSolicitud;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        threadPool.shutdown();
    }

    public void start() {
        running = true;
        // 1. Iniciar servidor TCP de escucha primero para garantizar que acepte conexiones
        try (ServerSocket ss = new ServerSocket(serverPort)) {
            this.serverSocket = ss;
            System.out.println(" Servidor de Pedidos iniciado en el puerto: " + serverPort);
            System.out.println(" Broker objetivo: " + brokerIp + ":" + brokerPort);
            System.out.println(" Servicios disponibles: " + SERVICIOS_DISPONIBLES);

            // 2. Registrar servicios en el Broker
            registrarEnBroker();

            while (running) {
                try {
                    Socket socketBroker = ss.accept();
                    threadPool.submit(new AtenderSolicitud(socketBroker, procesarSolicitud));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[PedidoServer] Error al aceptar conexión: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[PedidoServer] Error fatal al iniciar ServerSocket: " + e.getMessage());
            }
        } finally {
            this.threadPool.shutdown();
        }
    }

    public String obtenerIpLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }

                String nombre = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();


                // Recorrer direcciones IP asignadas a esta interfaz
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // Buscar IPv4 válida que no sea de loopback ni enlace local (169.254.x.x)
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        // Priorizar direcciones de red local privada (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                        if (addr.isSiteLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }

            // Segunda pasada más permisiva en caso de que ninguna coincida con los filtros estrictos
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PedidoServer] Error al obtener IP por NetworkInterface: " + e.getMessage());
        }

        return "127.0.0.1";
    }

    private void registrarEnBroker() {
        String ipLocal = obtenerIpLocal();

        System.out.println("[PedidoServer] IP local detectada para registro: " + ipLocal);
        System.out.println("[PedidoServer] Registrando servicios con el Broker...");
        BrokerRegistrar registrar = new BrokerRegistrar(brokerIp, brokerPort);
        boolean exito = registrar.registrarServicios(SERVICIOS_DISPONIBLES, ipLocal, serverPort);
        if (exito) {
            System.out.println("[PedidoServer] Todos los servicios fueron registrados exitosamente.");
        } else {
            System.out.println("[PedidoServer] Advertencia: No todos los servicios se pudieron registrar (¿está encendido el Broker?).");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java Servidor <puertoServidor> <ipBroker:puertoBroker>");
            System.exit(1);
            return;
        }

        int puertoLocal;
        String ipBroker;
        int puertoBroker;

        try {
            puertoLocal = Integer.parseInt(args[0]);

            String direccionBroker = args[1];
            String[] partes = direccionBroker.split(":", 2);
            if (partes.length != 2) {
                System.err.println("Error: El segundo argumento debe tener el formato <ip:puerto> (ejemplo: 127.0.0.1:9000)");
                System.exit(1);
                return;
            }

            ipBroker = partes[0].trim();
            puertoBroker = Integer.parseInt(partes[1].trim());
        } catch (NumberFormatException e) {
            System.err.println("Error: Los puertos deben ser números enteros válidos.");
            System.exit(1);
            return;
        }

        System.out.println("[Configuración] Puerto Servidor: " + puertoLocal + " | Broker: " + ipBroker + ":" + puertoBroker);

        PedidoServer server = new PedidoServer(puertoLocal, ipBroker, puertoBroker);
        server.start();
    }
}
