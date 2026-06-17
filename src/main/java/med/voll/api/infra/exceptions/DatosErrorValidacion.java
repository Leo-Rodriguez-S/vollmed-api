package med.voll.api.infra.exceptions;

import org.springframework.validation.FieldError;

//Record para mostrar los errores ocurridos en la validación
//El FieldError viene de la excepción MethodArgumentNotValidException, capturada por el @RestControllerAdvice
//El campo y mensaje son con respecto a la validación que salte y el mensaje que colocamos para esa validación
public record DatosErrorValidacion(String campo, String mensaje) {
    public DatosErrorValidacion(FieldError error){
        this(error.getField(), error.getDefaultMessage());
    }
}
