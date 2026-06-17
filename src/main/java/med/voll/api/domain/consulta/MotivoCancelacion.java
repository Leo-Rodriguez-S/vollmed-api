// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: MotivoCancelacion.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Enum que define los motivos válidos para cancelar una consulta.
// Actúa como contrato de dominio — solo estos tres valores son aceptados.
//
// VALORES:
//   PACIENTE_DESISTIO → el paciente decide no asistir
//   MEDICO_CANCELO    → el médico no puede atender
//   OTROS             → cualquier otro motivo no contemplado
//
// DESERIALIZACIÓN PERSONALIZADA CON @JsonCreator:
// Por defecto Jackson es case-sensitive: "paciente_desistio" fallaría
// porque el enum se llama PACIENTE_DESISTIO. @JsonCreator define un
// método estático que Jackson llama para deserializar cualquier String
// del JSON al enum correspondiente, de forma case-insensitive.
//
// FLUJO DE DESERIALIZACIÓN:
//   JSON: { "motivoCancelacion": "paciente_desistio" }
//         → Jackson llama getMotivoCancelacion("paciente_desistio")
//         → loop compara con equalsIgnoreCase
//         → retorna MotivoCancelacion.PACIENTE_DESISTIO ✅
//
//   JSON: { "motivoCancelacion": "INVALIDO" }
//         → loop no encuentra match
//         → throw ENUMInvalidoException
//         → GestorDeErrores → 400 BAD REQUEST con mensaje de valores válidos
//
// ALMACENAMIENTO EN BD:
// @Enumerated(EnumType.STRING) en Consulta.motivoCancelacion guarda
// el nombre del enum como String: "PACIENTE_DESISTIO", "MEDICO_CANCELO", "OTROS".
// Más legible y resistente a cambios de orden que EnumType.ORDINAL.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import com.fasterxml.jackson.annotation.JsonCreator;
import med.voll.api.infra.exceptions.ENUMInvalidoException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum MotivoCancelacion {

    PACIENTE_DESISTIO,
    MEDICO_CANCELO,
    OTROS;

    /*
     * Deserializador personalizado invocado por Jackson al procesar el JSON.
     * Permite recibir el enum en cualquier capitalización.
     *
     * @param motivoCancelacion  String del JSON (ej: "paciente_desistio", "MEDICO_CANCELO")
     * @return El valor del enum correspondiente.
     * @throws ENUMInvalidoException si el String no coincide con ningún valor.
     */
    @JsonCreator
    public static MotivoCancelacion getMotivoCancelacion(String motivoCancelacion) {

        // Itera sobre todos los valores del enum buscando coincidencia case-insensitive.
        for (MotivoCancelacion m : MotivoCancelacion.values()) {
            if (m.toString().equalsIgnoreCase(motivoCancelacion)) {
                return m;  // Match encontrado → retorna el enum.
            }
        }

        // Si ningún valor coincide → lanza excepción con los valores válidos.
        // GestorDeErrores la captura y retorna 400 BAD REQUEST.
        throw new ENUMInvalidoException(
                "Motivo de cancelación inválido. Seleccione un motivo válido: "
                        + motivosCancelacionValidos()
        );
    }

    /*
     * Genera un String con todos los valores válidos del enum separados por coma.
     * Usado en el mensaje de error para guiar al cliente sobre qué valores enviar.
     * Ejemplo de output: "PACIENTE_DESISTIO, MEDICO_CANCELO, OTROS"
     */
    private static String motivosCancelacionValidos() {
        return Arrays.stream(MotivoCancelacion.values())
                .map(MotivoCancelacion::name)
                .collect(Collectors.joining(", "));
    }
}