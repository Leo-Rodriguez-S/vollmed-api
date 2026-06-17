// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeCancelaciones.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.cancelamiento
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Contrato (interfaz) que deben implementar todos los validadores
// de la operación de cancelamiento de consultas.
//
// PATRÓN DE DISEÑO — Strategy + Open/Closed Principle (SOLID):
// Define un contrato único con un método validar(). Cada regla de
// negocio de cancelamiento vive en su propia clase que implementa
// esta interfaz. Para agregar una nueva regla, solo se crea una nueva
// clase — sin modificar código existente (Open/Closed).
//
// INYECCIÓN AUTOMÁTICA POR SPRING:
// ReservaDeConsulta inyecta List<ValidadorDeCancelaciones>. Spring
// detecta automáticamente todas las clases @Component que implementen
// esta interfaz y las agrega a la lista. El foreach ejecuta todos
// los validadores en orden.
//
// VALIDADORES ACTUALES QUE IMPLEMENTAN ESTA INTERFAZ:
//   · ValidadorDeCancelamientoConAnticipacion
//     → cancela solo con 24+ horas de anticipación
//
// CÓMO AGREGAR UNA NUEVA REGLA DE CANCELAMIENTO:
//   1. Crear nueva clase @Component
//   2. Implementar ValidadorDeCancelaciones
//   3. Implementar el método validar()
//   4. Spring la detecta automáticamente — sin tocar nada más
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.cancelamiento;

import med.voll.api.domain.consulta.DatosCancelamientoConsulta;

public interface ValidadorDeCancelaciones {

    /*
     * Contrato que todo validador de cancelamiento debe cumplir.
     *
     * @param datos  Contiene idConsulta y motivoCancelacion del request.
     *               El validador usa estos datos para verificar su regla.
     *
     * Comportamiento esperado:
     *   - Si la regla se cumple    → no hace nada, retorna normalmente.
     *   - Si la regla se viola     → lanza BusinessException con 409 CONFLICT.
     *
     * La excepción es capturada por GestorDeErrores (@RestControllerAdvice)
     * que la convierte en la respuesta HTTP apropiada al cliente.
     */
    void validar(DatosCancelamientoConsulta datos);
}