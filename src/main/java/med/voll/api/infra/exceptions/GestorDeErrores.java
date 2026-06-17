package med.voll.api.infra.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

/*
Combina:
├─ @ControllerAdvice: Aplica a TODOS los controllers
└─ @ResponseBody: Serializa retorno a JSON automáticamente
*/
@RestControllerAdvice
public class GestorDeErrores {

    //Elemento buscado no encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> gestionarError404(EntityNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                crearError(HttpStatus.NOT_FOUND.value(), "Not Found", ex, "Recurso no encontrado")
        );

    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> gestionarError404(NoResourceFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                crearError(HttpStatus.NOT_FOUND.value(), "Not Found", ex, "Ruta no encontrada")
        );

    }

    //Se lanza cuando el objeto no cumple con la validación
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> gestionarError400(MethodArgumentNotValidException ex) {

        var detalles = ex.getFieldErrors().stream()
                .map(DatosErrorValidacion::new)
                .toList();

        return ResponseEntity.badRequest().body(
                crearError(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex, "Error de validación", detalles)
        );

    }

    //Se lanza cuando cuando:
    //- Se espera un body en el request y no viene
    //- La sintaxis del body esperado está incorrecta
    //- Se envía un valor para el enum que no existe -- Se trabaja con clase personalizada para capturar la exception
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> gestionarError400(HttpMessageNotReadableException ex) {

        //Se obtiene la causa más específica de la excepción HttpMessageNotReadableException
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);

        //Si la causa más específica es instancia de nuestra clase personalizada EspecialidadInvlaidaException lanzamos el mensaje del root
        var mensaje = root instanceof ENUMInvalidoException
                ? root.getMessage()
                //Si la causa no es por valores inválidos en el ENUM, se lanza el fallback siguiente
                : "JSON inválido o mal formado";


        return ResponseEntity.badRequest().body(
                crearError(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex, mensaje)
        );

    }

    //Credenciales inválidas en el login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> gestionarErrorBadCredentials(BadCredentialsException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                crearError(HttpStatus.UNAUTHORIZED.value(), "Bad Credentials", ex, "Credenciales inválidas")
        );

    }

    //Validaciones de reglas del negocio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> gestionarValidacionesDeReglasDeNegocio(BusinessException ex){

        HttpStatus status = ex.getStatus();

        return ResponseEntity.status(ex.getStatus()).body(
                crearError(status.value(), status.getReasonPhrase(), ex, "Error de validación")
        );
    }

    //Es la clase padre para tratar errores de autenticación cuando las clases hijas específicas no lo cubrieron
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> gestionarErrorAuthentication(AuthenticationException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
          crearError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex, "Error en la autenticación.")
        );

    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarErrorBD(DataIntegrityViolationException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                crearError(HttpStatus.CONFLICT.value(), "Conflict", ex, "Conflicto de integridad de datos")
        );

    }

    //Metodo HTTP no soportado en ruta existente
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex
    ) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                crearError(HttpStatus.METHOD_NOT_ALLOWED.value(), "Method Not Allowed", ex, "Método HTTP no soportado en esta ruta")
        );

    }

    //Handler para excepciones de perfiles duplicados
    @ExceptionHandler(DuplicadoException.class)
    public ResponseEntity<ErrorResponse> gestionarDuplicado(
            DuplicadoException ex
    ) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                crearError(HttpStatus.CONFLICT.value(), "Conflict", ex, "Uno o más datos corresponden a un perfil ya registrado.")
        );

    }

    //Handler para excepciones de recursos inactivos
    @ExceptionHandler(RecursoInactivoException.class)
    public ResponseEntity<ErrorResponse> gestionarRecursoInactivo(
            RecursoInactivoException ex
    ) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                crearError(HttpStatus.CONFLICT.value(), "Conflict", ex, "Perfil inactivo. Contacte a Soporte.")
        );

    }

    //Captura lo que no entró en las otras excepciones
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> gestionarError500(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          crearError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex, "Error interno del servidor.")
        );

    }

    private String obtenerMensaje(Exception ex, String mensajePorDefecto){
        return ex.getMessage() != null
                ? ex.getMessage()
                : mensajePorDefecto;
    }

    private ErrorResponse crearError(
            Integer status,
            String error,
            Exception ex,
            String mensajePorDefecto
    ){
        return crearError(
                status,
                error,
                ex,
                mensajePorDefecto,
                null
        );
    }

    private ErrorResponse crearError(
            Integer status,
            String error,
            Exception ex,
            String mensajePorDefecto,
            List<DatosErrorValidacion> detalles
    ){
        return new ErrorResponse(
                status,
                error,
                obtenerMensaje(ex, mensajePorDefecto),
                LocalDateTime.now(),
                detalles
        );
    }

}