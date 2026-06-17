// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ConsultaRepository.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Interfaz de acceso a datos para la entidad Consulta.
// Spring Data JPA genera automáticamente la implementación en runtime
// basándose en los nombres de los métodos (método → query SQL).
//
// HEREDA DE JpaRepository<Consulta, Long>:
// Proporciona automáticamente: save(), findById(), findAll(), existsById(),
// deleteById(), getReferenceById(), y más. No es necesario implementarlos.
//
// QUERIES POR NOMBRE DE MÉTODO (Spring Data JPA):
// Spring analiza el nombre del método y construye la query JPQL equivalente.
// La convención es: existsBy[Campo][Condición]And[Campo][Condición]...
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /*
     * ¿Tiene el paciente alguna consulta activa en el rango de fechas dado?
     *
     * Traducción JPQL generada automáticamente:
     *   SELECT COUNT(c) > 0 FROM Consulta c
     *   WHERE c.paciente.id = :id
     *   AND c.fecha BETWEEN :inicioDia AND :finalDia
     *   AND c.motivoCancelacion IS NULL
     *
     * Usada por: ValidadorDePacienteSinMasConsultasElMismoDia
     * Propósito: verificar que el paciente no tenga otra consulta activa ese día.
     *
     * El rango [inicioDia, finalDia] cubre todo el día completo:
     *   inicioDia = fecha.toLocalDate().atStartOfDay()     → 00:00:00.000
     *   finalDia  = fecha.toLocalDate().atTime(LocalTime.MAX) → 23:59:59.999
     *
     * "MotivoCancelacionIsNull" garantiza que solo se cuentan consultas
     * activas — las canceladas no bloquean el día del paciente.
     */
    Boolean existsByPacienteIdAndFechaBetweenAndMotivoCancelacionIsNull(
            Long id,
            LocalDateTime inicioDia,
            LocalDateTime finalDia
    );

    /*
     * ¿Tiene el médico una consulta activa en exactamente esa fecha y hora?
     *
     * Traducción JPQL generada automáticamente:
     *   SELECT COUNT(c) > 0 FROM Consulta c
     *   WHERE c.medico.id = :id
     *   AND c.fecha = :fecha
     *   AND c.motivoCancelacion IS NULL
     *
     * Usada por: ValidadorDeMedicoConOtraConsultaEnMismoHorario
     * Propósito: verificar que el slot del médico esté libre.
     *
     * La comparación de fecha es exacta (LocalDateTime == LocalDateTime).
     * Funciona porque ValidadorDeSlotVacio garantiza que todas las fechas
     * son en hora exacta — nunca habrá 07:09 que podría no coincidir
     * con otra consulta a las 07:00.
     *
     * "MotivoCancelacionIsNull" garantiza que una consulta cancelada
     * no bloquea el slot — el médico puede recibir otro paciente en
     * un slot donde una consulta anterior fue cancelada.
     */
    Boolean existsByMedicoIdAndFechaAndMotivoCancelacionIsNull(Long id, LocalDateTime fecha);
}