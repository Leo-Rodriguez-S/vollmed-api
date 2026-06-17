package med.voll.api.infra.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Integer status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<DatosErrorValidacion> details
) {

}
