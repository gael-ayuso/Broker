package servers.dto;

public record RespuestaErrorDTO(String servicio, String estado, String mensaje) {
    public RespuestaErrorDTO(String servicio, String mensaje) {
        this(servicio, "ERROR", mensaje);
    }
}
