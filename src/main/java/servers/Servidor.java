package servers;

/**
 * Punto de entrada alternativo con el nombre 'Servidor'
 * Permite ejecutar: java servers.Servidor 9001 127.0.0.1:9000
 */
public class Servidor {
    public static void main(String[] args) {
        PedidoServer.main(args);
    }
}
