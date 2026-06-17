package med.voll.api.domain.paciente;

import med.voll.api.controller.MedicoController;
import med.voll.api.controller.PacienteController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PacienteDTOModelAssembler
        implements RepresentationModelAssembler<PacienteDTO, EntityModel<PacienteDTO>> {

    @Override
    public EntityModel<PacienteDTO> toModel(PacienteDTO paciente) {
        return EntityModel.of(paciente,
                // Link a sí mismo (detalle del paciente)
                linkTo(methodOn(PacienteController.class)
                        .detallarPaciente(paciente.id()))
                        .withSelfRel(),

                // Link a la lista de pacientes
                linkTo(PacienteController.class)
                        .withRel("pacientes")
        );
    }
}
