package servers.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PeticionDTO(String servicio, JsonNode parametros) {
}
