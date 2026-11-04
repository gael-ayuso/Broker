package servers.dto;

public record RespuestaOKDTO(String servicio, String estado, Object resultado) {
    public RespuestaOKDTO(String servicio, Object resultado) {
        this(servicio, "OK", resultado);
    }
}
