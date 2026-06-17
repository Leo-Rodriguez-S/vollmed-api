package med.voll.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import med.voll.api.domain.medico.*;
import med.voll.api.infra.exceptions.DuplicadoException;
import med.voll.api.infra.exceptions.RecursoInactivoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/medicos")
//Referencia el esquema de seguridad de Spring Doc.
@SecurityRequirement(name = "bearer-key")
public class MedicoController {

    //Inyección de medicoRepository
    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired // PagedResourcesAssembler se usa para convertir una Page en un PagedModel, la cuál agrega paginación + estructura de lista.
    private PagedResourcesAssembler<MedicoDTO> pagedResourcesAssembler;

    //Agregar links a un objeto
    @Autowired
    MedicoDTOModelAssembler assembler;

    //Registro de un médico
    @Transactional
    @PostMapping
    public ResponseEntity registrarMedico(@RequestBody @Valid DatosRegistroMedico datos, UriComponentsBuilder uriComponentsBuilder) {

        //Se valida si el email o el documento ya corresponden a un médico registrado para lanzar excepción
        validarExistente(medicoRepository.findByEmailIgnoreCase(datos.email()), "Email", datos.email());
        validarExistente(medicoRepository.findByDocumento(datos.documento()), "Documento", datos.documento());

        //Si ninguna de las validaciones anteriores se cumple significa que el médico aún no existe en nuestra BD y lo podemos crear y guardar
        var medico = new Medico(datos);
        medicoRepository.save(medico);

        //Se crea URI del médico creado y guardado -- El id del médico se obtiene hasta que se guarde en la BD, la BD lo genera
        var uri = uriComponentsBuilder.path("/medicos/{id}").buildAndExpand(medico.getId()).toUri();

        //Se responde con la URI del médico y los detalles del médico guardado
        return ResponseEntity.created(uri).body(new DatosDetalleMedico(medico));
    }

    //Se devuelve lista completa de médicos activos, en forma de página
    //Se muestra la página de acuerdo al pageable enviado
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<MedicoDTO>>> listarMedico(
            //Pageable por default
            @PageableDefault(size = 5, sort = {"nombre"}, direction = Sort.Direction.ASC)
            //Pageable personalizada (la envía el cliente)
            Pageable paginacion
    ){
        //Se devuelve la lista de médicos activos en forma de Page y se convierte contenido de Medico a MedicoDTO
        Page<MedicoDTO> pagina = medicoRepository.findAllByActivoTrue(paginacion).map(MedicoDTO::new);

        // Usamos el pagedResourcesAssembler y el assembler para convertir la Page en un PagedModel y darle links a los objetos y partes de la página.
        // Esto garantiza que cada objeto MedicoDTO sea envuelto en un EntityModel, proporcionando una estructura JSON estable y permitiendo añadir links adicionales.
        var model = pagedResourcesAssembler.toModel(pagina, assembler);

        //Respondemos con el modelo
        return ResponseEntity.ok(model);
    }

    //Devolver detalle de un médico individualmente dado su id en la URI
    @GetMapping("/{id}")
    public ResponseEntity detallarMedico(@PathVariable Long id){
        //Se valida si el médico existe, de lo contrario se lanza excepción
        var medico = medicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún medico registrado."));

        //Si el médico existe pero está inactivo se muestra al cliente como si no existiese
        if (!medico.getActivo()) {
            return ResponseEntity.notFound().build();
        }

        //Si el médico existe y se encuentra activo se pasa a un DTO DatosDetalleMedico para responder
        var detalle = new DatosDetalleMedico(medico);

        //Se agrupa el DTO en un EntityModel para agregarle links
        EntityModel<DatosDetalleMedico> model = EntityModel.of(
                detalle,
                //Link para el detalle del médico (mismo que se está consultando ahora mismo por el usuario)
                linkTo(methodOn(MedicoController.class).detallarMedico(id))
                .withSelfRel(),
        //Link para la página completa de médicos activos con la paginación por defecto
        linkTo(methodOn(MedicoController.class).listarMedico(null))
                .withRel("medicos"),
        //Link para la sección de actualizar este médico
        linkTo(methodOn(MedicoController.class).actualizarMedico(id, null))
                .withRel("update"),
        //Link para la sección de eliminar lógicamente este médico
        linkTo(methodOn(MedicoController.class).eliminarMedico(id))
                .withRel("delete"));

        //Se responde con el modelo (detalle del médico más links)
        return ResponseEntity.ok(model);
    }

    //Actualizar información de un médico dado su id en la URL
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity actualizarMedico(@PathVariable Long id, @RequestBody @Valid DatosActualizacionMedico datos) {
        //Se valida si el médico existe, en caso de que no, se lanza excepción
        var medico = medicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún médico registrado."));

        //Si el médico existe pero está inactivo, se lanza excepción personalizada
        if (!medico.getActivo()) {
            throw new RecursoInactivoException("Médico inactivo, no se pueden actualizar los datos. Contacte a Soporte.");
        }

        //Si el médico existe y está activo, actualizamos la información requerida
        medico.actualizarInformaciones(datos);

        //Se responde con los datos detallados del médico
        return ResponseEntity.ok(new DatosDetalleMedico(medico)) ;
    }

    //Eliminación lógica -- No elimina por completo el registro, sino que lo desactiva
    //Se elimina médico lógicamente según el id indicado en la URI
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity eliminarMedico(@PathVariable Long id){
        //Se valida si el médico con el id enviado existe en nuestra BD, en caso negativo se lanza excepción
        var medico = medicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El id '" + id + "' no corresponde a ningún médico registrado."));

        //Si el médico existe se desactiva
        medico.eliminar();

        //Se responde con un 204 OK (noContent())
        return ResponseEntity.noContent().build();
    }

    //Metodo privado para validar si el dato de un perfil ya existe en nuestra BD.
    //En los parámetros se le envía un Optional de la consulta, el campo consultado y el valor pasado en la consulta
    private void validarExistente(Optional<Medico> existente, String campo, String valor){
        //Se valida si el dato existe
        existente.ifPresent(medico -> {
            //Si existe se valida si se encuentra INACTIVO
            /*if (!medico.getActivo()) {
                //Si se encuentra INACTIVO se lanza excepción personalizada
                throw new RecursoInactivoException(
                        campo + " '" + valor + "' corresponde a un perfil ya registrado pero inactivo. " +
                                "Verifique la información nuevamente y en caso de requerirlo comuníquese con Soporte."
                );
            }*/
            //Si se encuentra ACTIVO se lanza otra excepción personalizada
            throw new DuplicadoException(campo + " '" + valor + "' corresponde a un perfil ya registrado. " +
                    "Verifique la información nuevamente y en caso de requerirlo comuníquese con Soporte.");
        });
    }

}
