package med.voll.api.infra.exceptions;

//Clase para trabajar la excepción de especialidades invalidad en el ENUM
public class ENUMInvalidoException extends RuntimeException {
    public ENUMInvalidoException(String message) {
        super(message);
    }
}
