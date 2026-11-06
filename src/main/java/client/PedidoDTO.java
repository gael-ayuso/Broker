package client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoDTO(
        Integer id,
        String cliente,
        List<ProductoDTO> productos,
        Double subtotal,
        Double descuento,
        Double impuestos,
        Double total,
        String estado
) {
}
