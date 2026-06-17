// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeConsultaConAnticipacion.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que una consulta solo pueda agendarse con al menos 30 minutos
// de anticipación respecto a la hora actual. Evita que se agenden
// consultas en tiempo real o en el pasado inmediato.
//
// REGLA DE NEGOCIO:
//   fechaConsulta - horaActual >= 30 minutos
//   Si la diferencia es menor a 30 minutos → 409 CONFLICT
//
// ZONA HORARIA — DETALLE IMPORTANTE:
// Se usa ZoneOffset.of("-06:00") que corresponde a Costa Rica (CST).
// LocalDateTime.now() sin zona horaria usa el reloj del servidor,
// que puede ser UTC u otro. Para garantizar consistencia con la
// zona horaria del negocio (Costa Rica), se especifica explícitamente.
// Si el servidor se cambia de región, este validador sigue siendo correcto.
//
// RELACIÓN CON ValidadorDeSlotVacio:
// ValidadorDeSlotVacio verifica que la hora sea exacta (08:00, no 08:09).
// Este validador verifica que esa hora exacta tenga al menos 30 min
// de distancia de la hora actual. Ambos son necesarios y complementarios.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class ValidadorDeConsultaConAnticipacion implements ValidadorDeConsultas {

    @Override
    public void validar(DatosRegistroConsulta datos){

        // Fecha de la consulta que viene en el request.
        var horaConsulta = datos.fecha();

        // Hora actual en la zona horaria de Costa Rica (-06:00).
        // Garantiza que la comparación sea correcta independientemente
        // de dónde esté desplegado el servidor.
        var horaActual = LocalDateTime.now(ZoneOffset.of("-06:00"));

        // Duration.between(inicio, fin).toMinutes() calcula la diferencia
        // en minutos entre la hora actual y la hora de la consulta.
        // Si la consulta es en el futuro → número positivo.
        // Si la consulta ya pasó → número negativo (también < 30 → falla).
        var diferenciaEnMinutos = Duration.between(horaActual, horaConsulta).toMinutes();

        // Menos de 30 minutos de anticipación → regla violada → 409 CONFLICT.
        if (diferenciaEnMinutos < 30) {
            throw new BusinessException(
                    "No se pueden agendar consultas con menos de 30 minutos de anticipación.",
                    HttpStatus.CONFLICT
            );
        }
        // Si >= 30 minutos → no hace nada, continúa al siguiente validador.
    }
}