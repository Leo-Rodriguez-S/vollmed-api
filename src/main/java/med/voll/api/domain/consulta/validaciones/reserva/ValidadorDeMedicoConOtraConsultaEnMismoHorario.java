// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeMedicoConOtraConsultaEnMismoHorario.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que el médico especificado no tenga ya una consulta activa
// en exactamente el mismo slot horario. Garantiza que cada médico
// atienda un solo paciente por slot.
//
// FUNCIONAMIENTO CON SLOTS FIJOS:
// Dado que ValidadorDeSlotVacio garantiza que las horas son exactas
// (07:00, 08:00, etc.), la comparación puede ser por igualdad exacta
// de fecha+hora. Si idMedico tiene una consulta a las 10:00 del mismo
// día → no puede agendarse otra a las 10:00 ese día.
//
// GUARD CLAUSE — idMedico null:
// Igual que ValidadorDeMedicoActivo, si idMedico es null el sistema
// elige médico aleatoriamente. La query aleatoria ya filtra médicos
// sin consultas en ese horario (elegirMedicoAleatorioDisponibleEnLaFecha),
// por lo que este validador no aplica.
//
// QUERY USADA:
// existsByMedicoIdAndFechaAndMotivoCancelacionIsNull()
//   → medico_id = idMedico
//   → fecha = datos.fecha() (comparación exacta de LocalDateTime)
//   → motivo_cancelacion IS NULL (solo consultas activas, no canceladas)
//
// MOTIVO DE "IS NULL":
// Una consulta cancelada setea motivoCancelacion con un valor del enum.
// Solo las consultas con motivoCancelacion = null están activas.
// Si se verificara sin este filtro, una consulta cancelada bloquearía
// el slot indefinidamente.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.ConsultaRepository;
import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ValidadorDeMedicoConOtraConsultaEnMismoHorario implements ValidadorDeConsultas {

    // Repositorio para verificar existencia de consultas en ese slot.
    @Autowired
    ConsultaRepository consultaRepository;

    @Override
    public void validar(DatosRegistroConsulta datos){

        // Guard clause: si no se especificó médico, este validador no aplica.
        // La elección aleatoria ya maneja la disponibilidad internamente.
        if (datos.idMedico() == null){
            return;
        }

        // Consulta BD: ¿existe alguna consulta activa de este médico en este slot exacto?
        // La comparación de fecha es exacta (LocalDateTime == LocalDateTime).
        // IS NULL en motivo_cancelacion filtra solo consultas no canceladas.
        var medicoConOtraConsulta = consultaRepository
                .existsByMedicoIdAndFechaAndMotivoCancelacionIsNull(datos.idMedico(), datos.fecha());

        // Si ya tiene consulta en ese slot → no puede recibir otra → 409 CONFLICT.
        if (medicoConOtraConsulta){
            throw new BusinessException(
                    "El médico tiene agendada otra consulta en esa misma fecha y hora",
                    HttpStatus.CONFLICT
            );
        }
        // Si el slot está libre → no hace nada, continúa.
    }
}