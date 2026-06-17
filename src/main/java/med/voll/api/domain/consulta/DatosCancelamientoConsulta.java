// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: DatosCancelamientoConsulta.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// DTO (Data Transfer Object) que representa el body del request
// DELETE /consultas. Contiene exactamente los datos necesarios
// para cancelar una consulta: su id y el motivo.
//
// ¿POR QUÉ UN RECORD?
// Los records de Java son inmutables por diseño — perfectos para DTOs
// de entrada donde los datos no deben modificarse tras la deserialización.
// Generan automáticamente: constructor, getters, equals, hashCode, toString.
//
// DESERIALIZACIÓN JSON → JAVA:
// Jackson convierte el JSON del request a este record:
//   { "idConsulta": 5, "motivoCancelacion": "PACIENTE_DESISTIO" }
//   → new DatosCancelamientoConsulta(5L, MotivoCancelacion.PACIENTE_DESISTIO)
//
// La conversión del String "PACIENTE_DESISTIO" al enum MotivoCancelacion
// la hace @JsonCreator en el propio enum (case-insensitive).
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import jakarta.validation.constraints.NotNull;

public record DatosCancelamientoConsulta(

        // Id de la consulta a cancelar.
        // @NotNull: si viene null en el JSON → Bean Validation falla → 400 BAD REQUEST.
        // Mensaje personalizado para ser más descriptivo que el genérico de @NotNull.
        @NotNull(message = "Debe indicar el número de consulta")
        Long idConsulta,

        // Motivo de la cancelación — uno de los valores del enum MotivoCancelacion.
        // @NotNull: el motivo es obligatorio — no se puede cancelar sin justificación.
        // Si viene un String inválido → @JsonCreator en MotivoCancelacion lanza
        // ENUMInvalidoException → GestorDeErrores → 400 BAD REQUEST.
        @NotNull
        MotivoCancelacion motivoCancelacion
) {
}