// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeFueraHorarioLaboral.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que la consulta esté dentro del horario laboral de la clínica.
// Es el guardián del calendario — ninguna consulta puede caer fuera
// de los slots disponibles.
//
// HORARIO LABORAL DE LA CLÍNICA:
//   Días:    Lunes a Sábado (domingos NO)
//   Mañana:  07:00 — 13:00 (slots: 07, 08, 09, 10, 11, 12)
//   Almuerzo 13:00 — 14:00 (NO disponible)
//   Tarde:   14:00 — 18:00 (slots: 14, 15, 16, 17, 18)
//   Total:   11 slots por médico por día
//
// SLOTS VÁLIDOS (en combinación con ValidadorDeSlotVacio):
//   07:00, 08:00, 09:00, 10:00, 11:00, 12:00,
//   14:00, 15:00, 16:00, 17:00, 18:00
//
// CONSTANTES COMO static final:
// Las horas de referencia se declaran como constantes de clase porque:
//   · Son valores fijos que nunca cambian en runtime.
//   · Se crean una sola vez (no en cada llamada a validar()).
//   · Son autodocumentadas — el nombre explica su propósito.
//
// LÓGICA DE ALMUERZO:
//   esAlmuerzo = !horaConsulta.isBefore(INICIO_ALMUERZO)   → hora >= 13:00
//             && horaConsulta.isBefore(FIN_ALMUERZO)        → hora < 14:00
//   Es decir: [13:00, 14:00) — incluye las 13:00, excluye las 14:00.
//
// LÓGICA DE DESPUÉS DE CIERRE:
//   despuesCierre = horaConsulta.isAfter(ULTIMO_SLOT)  → hora > 18:00
//   Las 18:00 son válidas (consulta termina a las 19:00 = cierre).
//   Las 18:01+ son inválidas.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
public class ValidadorDeFueraHorarioLaboral implements ValidadorDeConsultas {

    // Constantes del horario laboral — inmutables, creadas una sola vez.
    private static final LocalTime APERTURA        = LocalTime.of(7, 0);   // 07:00
    private static final LocalTime INICIO_ALMUERZO = LocalTime.of(13, 0);  // 13:00
    private static final LocalTime FIN_ALMUERZO    = LocalTime.of(14, 0);  // 14:00
    private static final LocalTime ULTIMO_SLOT     = LocalTime.of(18, 0);  // 18:00

    @Override
    public void validar(DatosRegistroConsulta datos){

        var fechaConsulta = datos.fecha();

        // Extrae solo la parte de hora (sin la fecha) para comparar con las constantes.
        var horaConsulta = fechaConsulta.toLocalTime();

        // ¿Es domingo? getDayOfWeek() retorna el enum DayOfWeek.
        var domingo = fechaConsulta.getDayOfWeek().equals(DayOfWeek.SUNDAY);

        // ¿Es antes de las 07:00?
        var antesApertura = horaConsulta.isBefore(APERTURA);

        // ¿Está en la franja de almuerzo [13:00, 14:00)?
        // !isBefore(13:00) → hora >= 13:00
        // isBefore(14:00)  → hora < 14:00
        var esAlmuerzo = !horaConsulta.isBefore(INICIO_ALMUERZO) && horaConsulta.isBefore(FIN_ALMUERZO);

        // ¿Es después de las 18:00? (18:01, 19:00, etc.)
        // isAfter es estricto: 18:00 NO es después de 18:00 → slot válido.
        var despuesCierre = horaConsulta.isAfter(ULTIMO_SLOT);

        // Si cualquiera de las condiciones es true → fuera de horario → 409.
        if (domingo || antesApertura || esAlmuerzo || despuesCierre) {
            throw new BusinessException(
                    "Día u hora seleccionados están fuera del horario laboral de la clínica",
                    HttpStatus.CONFLICT
            );
        }
        // Si ninguna condición se cumple → horario válido → continúa.
    }
}