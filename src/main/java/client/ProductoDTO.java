package client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDTO(
        String nombre,
        double precio,
        int cantidad,
        int existencia
) {
}
