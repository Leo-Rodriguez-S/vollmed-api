// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: DatosDetalleConsulta.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// DTO de SALIDA que representa la respuesta del endpoint POST /consultas.
// Contiene el detalle de una consulta recién creada o existente,
// sin exponer la entidad JPA directamente al cliente.
//
// ¿POR QUÉ NO EXPONER LA ENTIDAD DIRECTAMENTE?
// Las entidades JPA tienen relaciones lazy, anotaciones de persistencia
// y campos que no deberían salir en la API (como motivoCancelacion).
// Los DTOs permiten controlar exactamente qué datos salen en cada respuesta.
//
// CAMPOS EXPUESTOS EN EL RESPONSE:
//   id          → id generado por la BD tras el INSERT
//   idMedico    → id del médico que atenderá la consulta
//   idPaciente  → id del paciente
//   fecha       → fecha y hora de la consulta en formato dd/MM/yyyy HH:mm
//   especialidad → especialidad del médico
//
// @JsonFormat(pattern = "dd/MM/yyyy HH:mm"):
// Controla el formato de serialización de fecha en el JSON de respuesta.
// Sin esta anotación Jackson usaría el formato ISO 8601: "2025-06-09T10:00:00".
// Con esta anotación → "09/06/2025 10:00" — más legible para el cliente.
//
// CONSTRUCTOR DE CONVENIENCIA (Consulta → DatosDetalleConsulta):
// Permite crear el DTO directamente desde la entidad sin mapear
// campo por campo en cada lugar donde se necesite. DRY principle.
// Usado en ReservaDeConsulta.reservarConsulta() al retornar el resultado.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import com.fasterxml.jackson.annotation.JsonFormat;
import med.voll.api.domain.medico.Especialidad;

import java.time.LocalDateTime;

public record DatosDetalleConsulta(

        // Id de la consulta generado por la BD.
        // Puede ser null en tests donde no hay BD real.
        Long id,

        // Id del médico asignado (puede ser el elegido aleatoriamente).
        Long idMedico,

        Long idPaciente,

        // Fecha formateada para el response.
        // El cliente recibe "09/06/2025 10:00" en lugar del formato ISO.
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime fecha,

        Especialidad especialidad
) {

    /*
     * Constructor de conveniencia: Consulta → DatosDetalleConsulta.
     * Extrae los campos necesarios de la entidad JPA para construir el DTO.
     *
     * Accede a consulta.getMedico().getId() y consulta.getMedico().getEspecialidad()
     * → si el fetch es LAZY, Hibernate carga el Medico aquí si no estaba cargado.
     * Esto es seguro dentro de una transacción (@Transactional en el controller).
     */
    public DatosDetalleConsulta(Consulta consulta) {
        this(
                consulta.getId(),
                consulta.getMedico().getId(),
                consulta.getPaciente().getId(),
                consulta.getFecha(),
                consulta.getMedico().getEspecialidad()
        );
    }
}