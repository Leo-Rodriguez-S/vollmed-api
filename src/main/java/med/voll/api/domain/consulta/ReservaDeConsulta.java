// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ReservaDeConsulta.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Servicio (@Service) que encapsula toda la lógica de negocio relacionada
// con la reserva y cancelación de consultas médicas.
//
// ES LA CAPA MÁS IMPORTANTE DEL DOMINIO DE CONSULTAS:
// ConsultaController es deliberadamente delgado — delega todo aquí.
// ReservaDeConsulta es quien realmente hace el trabajo:
//   · Valida existencia de médico y paciente en BD
//   · Ejecuta todos los validadores de negocio
//   · Elige médico si no se especificó
//   · Persiste la consulta
//   · Valida y ejecuta cancelamientos
//
// PATRÓN STRATEGY — INYECCIÓN DE LISTAS:
// Spring detecta automáticamente todas las clases @Component que
// implementan ValidadorDeConsultas y ValidadorDeCancelaciones,
// y las inyecta como listas. El forEach ejecuta todos los validadores
// sin que ReservaDeConsulta sepa cuáles son ni cuántos hay.
// Agregar un nuevo validador = crear una nueva clase @Component.
// No se modifica ReservaDeConsulta. (Open/Closed Principle — SOLID)
//
// PRINCIPIO SOLID — Single Responsibility:
// Cada validador tiene su propia responsabilidad. ReservaDeConsulta
// orquesta pero no valida — delega esa responsabilidad a los validadores.
//
// FLUJO RESERVA:
//   1. Verificar que paciente existe en BD
//   2. Verificar que médico existe en BD (si se especificó)
//   3. Ejecutar todos los ValidadorDeConsultas (reglas de negocio)
//   4. Obtener el objeto Paciente de BD
//   5. Elegir médico (especificado o aleatorio)
//   6. Crear y persistir la Consulta
//   7. Retornar DatosDetalleConsulta
//
// FLUJO CANCELAMIENTO:
//   1. Verificar que la consulta existe en BD
//   2. Ejecutar todos los ValidadorDeCancelaciones
//   3. Obtener la Consulta de BD
//   4. Setear el motivo de cancelación (cancelación lógica)
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import jakarta.persistence.EntityNotFoundException;
import med.voll.api.domain.consulta.validaciones.cancelamiento.ValidadorDeCancelaciones;
import med.voll.api.domain.consulta.validaciones.reserva.ValidadorDeConsultas;
import med.voll.api.domain.medico.Medico;
import med.voll.api.domain.medico.MedicoRepository;
import med.voll.api.domain.paciente.PacienteRepository;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

// @Service marca esta clase como servicio de Spring.
// Semánticamente equivale a @Component pero comunica que esta clase
// contiene lógica de negocio — no es un repositorio ni un controller.
@Service
public class ReservaDeConsulta {

    // ══════════════════════════════════════════════════════════════
    // DEPENDENCIAS
    // ══════════════════════════════════════════════════════════════

    // Acceso a BD para médicos — verificar existencia y obtener referencia.
    @Autowired
    private MedicoRepository medicoRepository;

    // Acceso a BD para pacientes — verificar existencia y obtener objeto.
    @Autowired
    private PacienteRepository pacienteRepository;

    // Acceso a BD para consultas — persistir y verificar existencia.
    @Autowired
    private ConsultaRepository consultaRepository;

    /*
     * Lista de todos los validadores de RESERVA detectados por Spring.
     * Spring inyecta automáticamente todas las clases @Component que
     * implementen ValidadorDeConsultas:
     *   · ValidadorDeSlotVacio
     *   · ValidadorDeFueraHorarioLaboral
     *   · ValidadorDeConsultaConAnticipacion
     *   · ValidadorDePacienteActivo
     *   · ValidadorDeMedicoActivo
     *   · ValidadorDePacienteSinMasConsultasElMismoDia
     *   · ValidadorDeMedicoConOtraConsultaEnMismoHorario
     */
    @Autowired
    private List<ValidadorDeConsultas> validadores;

    /*
     * Lista de todos los validadores de CANCELAMIENTO detectados por Spring.
     * Actualmente contiene:
     *   · ValidadorDeCancelamientoConAnticipacion
     */
    @Autowired
    private List<ValidadorDeCancelaciones> validadorCancelaciones;

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ══════════════════════════════════════════════════════════════

    /*
     * Reserva una consulta médica ejecutando todas las validaciones
     * de negocio y persistiendo en BD.
     *
     * @param datos  DTO con idMedico (opcional), idPaciente, fecha, especialidad.
     * @return DatosDetalleConsulta con el detalle de la consulta creada.
     * @throws EntityNotFoundException si paciente o médico no existen en BD.
     * @throws BusinessException       si algún validador de negocio falla.
     * @throws RecursoInactivoException si médico o paciente están inactivos.
     */
    public DatosDetalleConsulta reservarConsulta(DatosRegistroConsulta datos){

        // PASO 1 — Verificar existencia del paciente.
        // existsById() es más eficiente que findById() cuando solo se necesita
        // saber si existe, sin cargar el objeto completo.
        if (!pacienteRepository.existsById(datos.idPaciente())){
            throw new EntityNotFoundException(
                    "Paciente con id '" + datos.idPaciente() + "' no existe."
            );
        }

        // PASO 2 — Verificar existencia del médico (solo si se especificó).
        // Si idMedico es null, el sistema elegirá uno aleatorio más adelante.
        if (datos.idMedico() != null && !medicoRepository.existsById(datos.idMedico())){
            throw new EntityNotFoundException(
                    "Medico con id '" + datos.idMedico() + "' no existe."
            );
        }

        // PASO 3 — Ejecutar todos los validadores de negocio.
        // Si cualquier validador lanza excepción, el forEach se detiene
        // y la excepción se propaga hacia GestorDeErrores.
        // El orden de ejecución depende del orden en que Spring detectó
        // los @Component (no garantizado sin @Order).
        validadores.forEach(v -> v.validar(datos));

        // PASO 4 — Obtener el objeto Paciente completo de BD.
        // .get() es seguro aquí porque ya verificamos existencia en el PASO 1.
        // En producción se podría reemplazar por orElseThrow() para mayor seguridad.
        var paciente = pacienteRepository.findById(datos.idPaciente()).get();

        // PASO 5 — Elegir el médico (especificado o aleatorio).
        var medico = elegirMedico(datos);

        // Si elegirMedico retorna null significa que no hay médicos disponibles
        // de esa especialidad en ese horario → informar al cliente.
        if (medico == null){
            throw new BusinessException(
                    "No hay médicos disponibles en el horario seleccionado.",
                    HttpStatus.CONFLICT
            );
        }

        // PASO 6 — Crear la entidad Consulta.
        // Se pasa null como id → la BD lo genera con AUTO_INCREMENT.
        // Se pasa null como motivoCancelacion → consulta activa.
        // El constructor simplificado (paciente, medico, fecha) hace lo mismo.
        var consulta = new Consulta(null, paciente, medico, datos.fecha(), null);

        // Persistir en BD — genera el INSERT y asigna el id al objeto consulta.
        consultaRepository.save(consulta);

        // PASO 7 — Retornar el detalle de la consulta creada.
        // El constructor de conveniencia extrae los campos necesarios de la entidad.
        return new DatosDetalleConsulta(consulta);
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ══════════════════════════════════════════════════════════════

    /*
     * Determina qué médico atenderá la consulta.
     *
     * Dos caminos:
     *   1. idMedico especificado → retorna ese médico directamente.
     *   2. idMedico null → elige médico aleatorio disponible por especialidad.
     *
     * getReferenceById() retorna una referencia lazy — no ejecuta SELECT.
     * Es más eficiente cuando solo se necesita la referencia para persistir
     * la relación (FK), no para acceder a campos del médico.
     *
     * elegirMedicoAleatorioDisponibleEnLaFecha() ejecuta una query JPQL
     * que filtra por especialidad, activo=true y sin consultas en esa fecha.
     * Puede retornar null si no hay médicos disponibles.
     *
     * @param datos  DTO del request con idMedico y especialidad.
     * @return Medico elegido, o null si no hay disponibles.
     * @throws BusinessException si idMedico es null y especialidad también es null.
     */
    private Medico elegirMedico(DatosRegistroConsulta datos) {

        // Si se especificó médico → usar ese directamente.
        if (datos.idMedico() != null){
            return medicoRepository.getReferenceById(datos.idMedico());
        }

        // Si no se especificó médico, especialidad es obligatoria para la búsqueda aleatoria.
        if (datos.especialidad() == null){
            throw new BusinessException(
                    "Es necesario elegir una especialidad cuando no se selecciona un médico",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Buscar médico aleatorio activo y disponible en ese horario y especialidad.
        // La query filtra: activo=true, especialidad=X, sin consultas en esa fecha.
        // Puede retornar null si no hay nadie disponible → manejado en reservarConsulta().
        return medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(
                datos.fecha(), datos.especialidad()
        );
    }

    /*
     * Cancela una consulta existente de forma lógica.
     * No elimina el registro — setea el motivo de cancelación.
     *
     * @param datos  DTO con idConsulta y motivoCancelacion.
     * @throws EntityNotFoundException si la consulta no existe en BD.
     * @throws BusinessException       si el cancelamiento es con menos de 24h de anticipación.
     */
    public void cancelarConsulta(DatosCancelamientoConsulta datos){

        // Verificar que la consulta existe antes de intentar cancelarla.
        if (!consultaRepository.existsById(datos.idConsulta())){
            throw new EntityNotFoundException(
                    "Consulta con id '" + datos.idConsulta() + "' no existe."
            );
        }

        // Ejecutar validadores de cancelamiento.
        // Actualmente: ValidadorDeCancelamientoConAnticipacion (24h mínimo).
        validadorCancelaciones.forEach(v -> v.validar(datos));

        // Obtener la referencia a la consulta.
        // getReferenceById() retorna proxy lazy — el SELECT se ejecuta
        // cuando se llama cancelar() y Hibernate necesita el objeto real.
        // Dentro de una transacción (@Transactional en el controller) esto es seguro.
        var consulta = consultaRepository.getReferenceById(datos.idConsulta());

        // Cancelación lógica: setea el motivo → la consulta queda inactiva.
        // No hace DELETE. La consulta permanece en BD para auditoría.
        // Hibernate detecta el cambio (dirty checking) y ejecuta UPDATE
        // automáticamente al finalizar la transacción.
        consulta.cancelar(datos.motivoCancelacion());
    }
}