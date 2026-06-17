package med.voll.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import med.voll.api.domain.paciente.*;
import med.voll.api.infra.exceptions.DuplicadoException;
import med.voll.api.infra.exceptions.RecursoInactivoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/pacientes")
//Referencia el esquema de seguridad de Spring Doc.
@SecurityRequirement(name = "bearer-key")
public class PacienteController {

    //Se inyecta pacienteRepository
    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired // PagedResourcesAssembler se usa para convertir una Page en un PagedModel.
    private PagedResourcesAssembler<PacienteDTO> pagedResourcesAssembler;

    @Autowired // Inyectamos nuestro PacienteDTOModelAssembler para convertir PacienteDTO en EntityModel.
    private PacienteDTOModelAssembler assembler;

    //Registrar paciente
    @Transactional
    @PostMapping
    public ResponseEntity registrarPaciente(@RequestBody @Valid DatosRegistroPaciente datos, UriComponentsBuilder uriComponentsBuilder) {
        //Se valida si el email, el documento o el telefono ya corresponden a un paciente registrado para lanzar excepción
        validarExistente(pacienteRepository.findByEmailIgnoreCase(datos.email()), "Email", datos.email());
        validarExistente(pacienteRepository.findByDocumento(datos.documento()), "Documento", datos.documento());
        validarExistente(pacienteRepository.findByTelefono(datos.telefono()), "Teléfono", datos.telefono());

        //Si ninguna de las validaciones anteriores se cumple significa que el paciente aún no existe en nuestra BD y lo podemos crear y guardar
        var paciente = new Paciente(datos);
        pacienteRepository.save(paciente);

        //Se crea URI del paciente creado y guardado -- El id del paciente se obtiene hasta que se guarde en la BD, la BD lo genera
        var uri = uriComponentsBuilder.path("/pacientes/{id}").buildAndExpand(paciente.getId()).toUri();

        //Se responde con la URI del paciente y los detalles del paciente guardado
        return ResponseEntity.created(uri).body(new DatosDetallePaciente(paciente));
    }

    //Se devuelve lista completa de pacientes activos, en forma de página
    //Se muestra la página de acuerdo al pageable enviado
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PacienteDTO>>> listarPaciente(
            //Pageable por default
            @PageableDefault(size = 10, sort = "nombre")
            //Pageable personalizada (la envía el cliente)
            Pageable paginacion
    ) {
        //Se devuelve la lista de pacientes activos en forma de Page y se convierte contenido de Paciente a PacienteDTO
        Page<PacienteDTO> pagina = pacienteRepository.findAllByActivoTrue(paginacion).map(PacienteDTO::new);

        // Usamos el pagedResourcesAssembler y el pacienteDTOModelAssembler para convertir la Page en un PagedModel.
        // Esto garantiza que cada objeto PacienteDTO sea envuelto en un EntityModel, proporcionando una estructura JSON estable y permitiendo añadir links adicionales.
        var model = pagedResourcesAssembler.toModel(pagina, assembler);

        //Respondemos con el modelo
        return ResponseEntity.ok(model);
    }

    //Devolver detalle de un paciente individualmente dado su id en la URI
    @GetMapping("/{id}")
    public ResponseEntity detallarPaciente(@PathVariable Long id){
        //Se valida si el paciente existe, de lo contrario se lanza excepción
        var paciente  = pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún paciente registrado."));

        //Si el paciente existe pero está inactivo se muestra al cliente como si no existiese
        if (!paciente.getActivo()) {
            return ResponseEntity.notFound().build();
        }

        //Si el paciente existe y se encuentra activo se pasa a un DTO DatosDetallePaciente para responder
        var detalle = new DatosDetallePaciente(paciente);

        //Se agrupa el DTO en un EntityModel para agregarle links
        EntityModel<DatosDetallePaciente> model = EntityModel.of(
                detalle,
                //Link para el detalle del paciente (mismo que se está consultando ahora mismo por el usuario)
                linkTo(methodOn(PacienteController.class).detallarPaciente(id))
                        .withSelfRel(),
                //Link para la página completa de pacientes activos con la paginación por defecto
                linkTo(methodOn(PacienteController.class).listarPaciente(null))
                        .withRel("pacientes"),
                //Link para la sección de actualizar este paciente
                linkTo(methodOn(PacienteController.class).actualizarPaciente(id, null))
                        .withRel("update"),
                //Link para la sección de eliminar lógicamente este paciente
                linkTo(methodOn(PacienteController.class).eliminarPaciente(id))
                        .withRel("delete"));

        //Se responde con el modelo (detalle del paciente más links)
        return ResponseEntity.ok(model);

    }

    //Actualizar información de un paciente dado su id en la URI
    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity actualizarPaciente(@PathVariable Long id, @RequestBody @Valid DatosActualizacionPaciente datos){
        //Se valida si el paciente existe, en caso de que no, se lanza excepción
        var paciente = pacienteRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún paciente registrado."));

        //Si el paciente existe pero está inactivo, se lanza excepción personalizada
        if (!paciente.getActivo()){
            throw new RecursoInactivoException("Paciente inactivo, no se pueden actualizar los datos. Contacte a Soporte");
        }

        //Si el paciente existe y está activo, actualizamos la información requerida
        paciente.actualizarDatos(datos);

        //Se responde con los datos detallados del paciente
        return ResponseEntity.ok(new DatosDetallePaciente(paciente));
    }

    //Eliminación lógica -- No elimina por completo el registro, sino que lo desactiva
    //Se elimina paciente lógicamente según el id indicado en la URI
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity eliminarPaciente(@PathVariable Long id){
        //Se valida si el paciente con el id enviado existe en nuestra BD, en caso negativo se lanza excepción
        var paciente  = pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún paciente registrado."));

        //Si el paciente existe se desactiva
        paciente.eliminar();

        //Se responde con un 204 OK (noContent())
        return ResponseEntity.noContent().build();
    }

    //Metodo privado para validar si el dato de un perfil ya existe en nuestra BD.
    //En los parámetros se le envía un Optional de la consulta, el campo consultado y el valor pasado en la consulta
    private void validarExistente(Optional<Paciente> existente, String campo, String valor){
        //Se valida si el dato existe
        existente.ifPresent(paciente -> {
            //Si existe se valida si se encuentra INACTIVO
            /*if (!paciente.getActivo()){
                //Si se encuentra INACTIVO se lanza excepción personalizada
                throw new RecursoInactivoException(campo + " '" + valor + "' corresponde a un perfil ya registrado pero inactivo. " +
                        "Verifique la información nuevamente y en caso de requerirlo comuníquese con Soporte.");
            }*/
            //Si se encuentra ACTIVO se lanza otra excepción personalizada
            throw new DuplicadoException(campo + " '" + valor + "' corresponde a un perfil ya registrado. " +
                    "Verifique la información nuevamente y en caso de requerirlo comuníquese con Soporte."
                    );
        });
    }

}
