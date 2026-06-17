package med.voll.api.domain.medico;

import jakarta.validation.Valid;
import med.voll.api.domain.direccion.DatosDireccion;

public record DatosActualizacionMedico(
        String nombre,
        String telefono,
        @Valid DatosDireccion direccion
) {
}
