package med.voll.api.domain.paciente;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import med.voll.api.domain.direccion.DatosDireccion;

public record DatosRegistroPaciente(
        //En lugar de colocar el valor del mensaje directamente, se puede colocar en un archivo ValidationMessages.properties en resources y
        //usar el valor de la siguiente forma abajo, que es el nombre que usamos en el archivo properties.
        @NotBlank(message = "{nombre.obligatorio}")
        //Se valida que se ingrese al menos un nombre y ambos apellidos
        //Se permite un máximo de 6 grupos de hasta 15 carácteres cada uno
        @Pattern(
                regexp = "^[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}(\\s[A-Za-zÑñáéíóúÁÉÍÓÚ]{1,15}){2,6}$",
                message = "¡Formato inválido! Debe ingresar al menos un nombre y ambos apellidos (entre 3 y 6 palabras). Máximo 15 letras por palabra."
        )
        String nombre,

        //Se valida que se ingrese un email con fórmato válido y una longitud máxima de 100 carácteres
        @NotBlank(message = "El email es obligatorio")
        @Size(max = 100, message = "El email no puede exceder los 100 carácteres.")
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
                message = "Fórmato de email inválido"
        )
        String email,

        //Se valida que se ingrese un documento con fórmato válido
        @NotBlank(message = "El documento es obligatorio")
        @Pattern(
                regexp = "\\d{9,12}",
                message = "Documento debe tener entre 9 y 12 dígitos, ingresados sin separaciones."
        )
        String documento,

        //Se valida que se ingrese un teléfono con fórmato válido
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(
                regexp = "\\d{8}",
                message = "El teléfono debe tener 8 dígitos exactos."
        )
        String telefono,

        //Se valida que se ingrese una dirección válida
        @NotNull(message = "La dirección es obligatoria")
        @Valid
        @JsonAlias("direccion") DatosDireccion datosDireccion
) {
}

