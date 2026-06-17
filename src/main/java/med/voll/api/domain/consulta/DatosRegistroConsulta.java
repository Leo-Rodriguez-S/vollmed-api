// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: DatosRegistroConsulta.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// DTO de ENTRADA que representa el body del request POST /consultas.
// Contiene los datos que el cliente envía para reservar una consulta.
//
// CAMPOS Y SUS REGLAS:
//
//   idMedico    → opcional (sin @NotNull). null = sistema elige médico aleatorio.
//                 Si se especifica, ese médico específico atenderá la consulta.
//                 Si es null, especialidad es obligatoria (validado en ReservaDeConsulta).
//
//   idPaciente  → obligatorio (@NotNull). Sin paciente no hay consulta.
//
//   fecha       → obligatorio (@NotNull) + debe ser futura (@Future).
//                 @Future: Bean Validation rechaza fechas en el pasado → 400.
//                 @JsonFormat: Jackson deserializa el String "09/06/2025 10:00"
//                 al LocalDateTime correspondiente. Sin este formato,
//                 Jackson no sabría cómo parsear la fecha → error de deserialización.
//
//   especialidad → opcional. Requerida solo si idMedico es null.
//                  Validado en ReservaDeConsulta, no aquí con Bean Validation.
//
// EJEMPLO DE JSON VÁLIDO:
//   { "idMedico": 2, "idPaciente": 5, "fecha": "09/06/2025 10:00", "especialidad": "CARDIOLOGIA" }
//   { "idPaciente": 5, "fecha": "09/06/2025 10:00", "especialidad": "CARDIOLOGIA" }  ← sin médico, aleatorio
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import med.voll.api.domain.medico.Especialidad;

import java.time.LocalDateTime;

public record DatosRegistroConsulta(

        // Opcional — si null el sistema elige médico disponible por especialidad.
        Long idMedico,

        // Obligatorio — identifica al paciente que solicita la consulta.
        @NotNull
        Long idPaciente,

        // Obligatorio + debe ser fecha futura + formato específico para deserialización.
        // @Future evalúa contra LocalDateTime.now() del servidor en zona UTC.
        // @JsonFormat indica a Jackson cómo parsear el String del JSON a LocalDateTime.
        @NotNull
        @Future
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime fecha,

        // Opcional en el request, pero obligatoria si idMedico es null.
        // Validado en ReservaDeConsulta.elegirMedico() con BusinessException → 400.
        Especialidad especialidad
) {
}