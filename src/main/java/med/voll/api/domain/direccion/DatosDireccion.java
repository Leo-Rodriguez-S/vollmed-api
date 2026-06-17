package med.voll.api.domain.direccion;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatosDireccion(
        //Se valida que el campo 'calle' se llene obligatoriamente, y con una longitud máxima de 100 carácteres
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 100, message = "La cantidad máxima de carácteres es de 100 en la calle.")
        String calle,

        //Se valida que el campo 'numero' cuente con una longitud máxima de 20 carácteres si se llena (es opcional)
        @Size(max = 20, message = "La cantidad máxima de carácteres es de 20 en el número.")
        String numero,

        //Se valida que el campo 'complemento' cuente con una longitud máxima de 100 carácteres si se llena (es opcional)
        @Size(max = 100, message = "La cantidad máxima de carácteres es de 100 en el complemento.")
        String complemento,

        //Se valida que el campo 'barrio' se llene obligatoriamente en un fórmato válido
        @NotBlank(message = "El barrio es obligatorio")
        @Pattern(
                regexp = "^[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}(\\s[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}){0,5}$",
                message = "¡Formato inválido! Debe ingresar al menos una palabra y máximo 6. Máximo 15 letras por palabra."
        )
        String barrio,

        //Se valida que el campo 'ciudad' se llene obligatoriamente en un fórmato válido
        @NotBlank(message = "La ciudad es obligatoria")
        @Pattern(
                regexp = "^[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}(\\s[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}){0,5}$",
                message = "¡Formato inválido! Debe ingresar al menos una palabra y máximo 6. Máximo 15 letras por palabra."
        )
        String ciudad,

        //Se valida que el campo 'codigo_postal' se llene obligatoriamente, y con una longitud máxima de 5 dígitos (sólo números)
        @NotBlank (message = "El código postal es obligatorio")
        @Pattern(regexp = "\\d{5}", message = "El código postal debe contener exactamente cinco dígitos")
        @JsonAlias("codigo_postal") String codigoPostal,

        //Se valida que el campo 'estado' se llene obligatoriamente en un fórmato válido
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(
                regexp = "^[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}(\\s[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}){0,5}$",
                message = "¡Formato inválido! Debe ingresar al menos una palabra y máximo 6. Máximo 15 letras por palabra."
        )
        String estado
) {
}
