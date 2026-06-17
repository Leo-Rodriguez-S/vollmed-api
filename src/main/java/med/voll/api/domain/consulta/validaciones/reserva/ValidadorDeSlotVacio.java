// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeSlotVacio.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que la hora de la consulta sea en punto exacto, sin minutos
// ni segundos. Implementa el sistema de slots fijos de 1 hora que
// define cómo funciona el calendario de la clínica.
//
// REGLA DE NEGOCIO — SLOTS FIJOS:
// La clínica opera con slots de exactamente 1 hora en horas exactas.
// Solo son válidos: 07:00, 08:00, 09:00, 10:00, 11:00, 12:00,
//                   14:00, 15:00, 16:00, 17:00, 18:00
// Cualquier hora con minutos o segundos (07:09, 10:30, 11:59) → inválida.
//
// ¿POR QUÉ ES UN VALIDADOR DE BACKEND Y NO SOLO DE FRONTEND?
// Si el frontend envía 07:00 y lo valida, perfecto. Pero si alguien
// llama directamente a la API con 07:09, el slot se crearía roto.
// Esta validación garantiza que la API nunca acepte horas irregulares
// independientemente del cliente que la llame.
//
// ¿POR QUÉ 409 CONFLICT Y NO 400 BAD REQUEST?
// 400 es para datos malformados técnicamente (JSON roto, tipo incorrecto).
// 07:09 es una fecha técnicamente válida — el problema es que viola
// una regla de negocio (slots fijos). Por eso 409 CONFLICT es más preciso.
//
// RELACIÓN CON ValidadorDeFueraHorarioLaboral:
// Este validador verifica que la hora sea exacta.
// ValidadorDeFueraHorarioLaboral verifica que esa hora esté en horario laboral.
// Ambos son necesarios: una hora exacta puede estar fuera de horario (19:00),
// y una hora dentro de horario puede no ser exacta (07:09).
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ValidadorDeSlotVacio implements ValidadorDeConsultas {

    @Override
    public void validar(DatosRegistroConsulta datos) {

        // Extrae solo la parte de hora del LocalDateTime para analizar minutos y segundos.
        var hora = datos.fecha().toLocalTime();

        // Verifica que tanto minutos como segundos sean 0.
        // getMinute() → 0-59. getSecond() → 0-59.
        // Si cualquiera es != 0 → la hora no es exacta → regla violada.
        //
        // Ejemplos:
        //   07:00:00 → minuto=0, segundo=0 → válido ✅
        //   07:09:00 → minuto=9, segundo=0 → inválido ❌
        //   07:00:30 → minuto=0, segundo=30 → inválido ❌
        //   07:30:30 → minuto=30, segundo=30 → inválido ❌
        if (hora.getMinute() != 0 || hora.getSecond() != 0){
            throw new BusinessException(
                    "Las consultas sólo pueden agendarse en horas exactas (07:00, 08:00, 09:00)",
                    HttpStatus.CONFLICT
            );
        }
        // Si minuto=0 y segundo=0 → hora exacta → no hace nada, continúa.
    }
}