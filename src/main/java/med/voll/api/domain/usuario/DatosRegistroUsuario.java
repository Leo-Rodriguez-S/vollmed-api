package med.voll.api.domain.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatosRegistroUsuario(
        @NotBlank(message = "Debe de indicar un email válido")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Fórmato de email inválido")
        String login,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
        )
        String contrasena
        ) {

}
