// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeConsultas.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Contrato (interfaz) que deben implementar todos los validadores
// de la operación de RESERVA de consultas.
//
// PATRÓN DE DISEÑO — Strategy + Open/Closed Principle (SOLID):
// Igual que ValidadorDeCancelaciones pero para reserva. Permite agregar
// nuevas reglas de negocio sin modificar código existente.
//
// VALIDADORES ACTUALES QUE IMPLEMENTAN ESTA INTERFAZ (en orden lógico):
//   1. ValidadorDeSlotVacio
//      → la hora debe ser exacta (07:00, 08:00...), sin minutos ni segundos
//   2. ValidadorDeFueraHorarioLaboral
//      → lunes a sábado, 07:00-18:00, sin almuerzo 13:00-14:00
//   3. ValidadorDeConsultaConAnticipacion
//      → mínimo 30 minutos de anticipación (zona horaria -06:00 Costa Rica)
//   4. ValidadorDePacienteActivo
//      → el paciente debe estar activo en BD
//   5. ValidadorDeMedicoActivo
//      → el médico (si se especificó) debe estar activo en BD
//   6. ValidadorDePacienteSinMasConsultasElMismoDia
//      → el paciente no puede tener otra consulta el mismo día
//   7. ValidadorDeMedicoConOtraConsultaEnMismoHorario
//      → el médico (si se especificó) no puede tener otro slot ocupado
//
// NOTA: Spring no garantiza orden de ejecución entre @Component.
// Si el orden importa, implementar Ordered o usar @Order.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;

public interface ValidadorDeConsultas {

    /*
     * Contrato que todo validador de reserva debe cumplir.
     *
     * @param datos  Contiene idMedico (opcional), idPaciente, fecha,
     *               especialidad del request de reserva.
     *
     * Comportamiento esperado:
     *   - Si la regla se cumple → no hace nada, continúa al siguiente validador.
     *   - Si la regla se viola  → lanza BusinessException o RecursoInactivoException
     *                             con el HttpStatus correspondiente (generalmente 409).
     *
     * La excepción es capturada por GestorDeErrores que la convierte
     * en la respuesta HTTP apropiada al cliente.
     */
    void validar(DatosRegistroConsulta datos);
}