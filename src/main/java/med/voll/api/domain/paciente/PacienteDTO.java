package med.voll.api.domain.paciente;

public record PacienteDTO(
        Long id,
        String nombre,
        String email,
        String documento
) {

    public PacienteDTO(Paciente paciente){
        this(
                paciente.getId(),
                paciente.getNombre(),
                paciente.getEmail(),
                paciente.getDocumento()
        );
    }

}


