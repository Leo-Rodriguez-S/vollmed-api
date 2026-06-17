package med.voll.api.domain.paciente;

import med.voll.api.domain.direccion.Direccion;

public record DatosDetallePaciente(
        Long id,
        String nombre,
        String email,
        String documentoIdentidad,
        String telefono,
        Direccion direccion
) {
    public DatosDetallePaciente(Paciente paciente){
        this(
                paciente.getId(),
                paciente.getNombre(),
                paciente.getEmail(),
                paciente.getDocumento(),
                paciente.getTelefono(),
                paciente.getDireccion()
        );
    }
}
