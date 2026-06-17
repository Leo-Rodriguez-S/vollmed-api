package med.voll.api.domain.medico;

import med.voll.api.controller.MedicoController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MedicoDTOModelAssembler
        implements RepresentationModelAssembler<MedicoDTO, EntityModel<MedicoDTO>> {

    @Override
    public EntityModel<MedicoDTO> toModel(MedicoDTO medico) {
        return EntityModel.of(medico,
                // Link a sí mismo (detalle del médico)
                linkTo(methodOn(MedicoController.class)
                        .detallarMedico(medico.id()))
                        .withSelfRel(),

                // Link a la lista de médicos
                linkTo(MedicoController.class)
                        .withRel("medicos")
        );
    }
}
