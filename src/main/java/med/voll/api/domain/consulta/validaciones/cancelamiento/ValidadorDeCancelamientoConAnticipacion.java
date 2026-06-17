// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeCancelamientoConAnticipacion.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.cancelamiento
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que una consulta solo pueda cancelarse con al menos 24 horas
// de anticipación a su fecha programada. Protege al médico y a la
// clínica de cancelaciones de último momento.
//
// REGLA DE NEGOCIO:
//   horaConsultaAgendada - horaActual >= 24 horas
//   Si la diferencia es menor a 24 horas → 409 CONFLICT
//
// DÓNDE SE EJECUTA:
// ReservaDeConsulta.cancelarConsulta() itera sobre
// List<ValidadorDeCancelaciones> y llama validar() en cada uno.
// Este validador es el único en esa lista actualmente.
//
// VALIDACIÓN EN BACKEND (no solo frontend):
// Esta regla vive en el backend porque si alguien llama directamente
// a la API sin pasar por el frontend, la regla se sigue cumpliendo.
// La integridad del negocio no depende del cliente.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.cancelamiento;

import med.voll.api.domain.consulta.ConsultaRepository;
import med.voll.api.domain.consulta.DatosCancelamientoConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// @Component registra esta clase en el contenedor de Spring.
// Spring la detecta automáticamente como implementación de
// ValidadorDeCancelaciones y la agrega a la lista inyectada
// en ReservaDeConsulta.
@Component
public class ValidadorDeCancelamientoConAnticipacion implements ValidadorDeCancelaciones {

    // Repositorio para obtener la consulta y su fecha programada.
    @Autowired
    ConsultaRepository consultaRepository;

    @Override
    public void validar(DatosCancelamientoConsulta datos){

        // Captura el momento exacto en que se intenta cancelar.
        var horaActual = LocalDateTime.now();

        // getReferenceById() retorna una referencia lazy a la entidad.
        // No ejecuta SELECT inmediatamente — solo carga cuando se accede
        // a un campo (getFecha()). Más eficiente que findById() cuando
        // solo se necesita un campo específico.
        var horaConsultaAgendada = consultaRepository.getReferenceById(datos.idConsulta());

        // Duration.between() calcula la diferencia entre dos momentos.
        // .toHours() convierte esa diferencia a horas (trunca decimales).
        // Ej: 23h 59min → 23 horas (truncado, no redondeado).
        var diferenciaDeHoras = Duration.between(horaActual, horaConsultaAgendada.getFecha()).toHours();

        // Si quedan menos de 24 horas → regla de negocio violada.
        // Se lanza BusinessException que GestorDeErrores convierte a 409 CONFLICT.
        if (diferenciaDeHoras < 24) {
            throw new BusinessException(
                    "No se puede cancelar una consulta con menos de 24 horas de antelación.",
                    HttpStatus.CONFLICT
            );
        }
        // Si la diferencia es >= 24 horas → no hace nada, el cancelamiento puede proceder.
    }
}