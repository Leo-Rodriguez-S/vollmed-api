// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDePacienteSinMasConsultasElMismoDia.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que un paciente no tenga más de una consulta activa por día.
// Evita que un paciente acapare múltiples slots en un mismo día.
//
// REGLA DE NEGOCIO:
// Un paciente puede tener como máximo 1 consulta activa por día calendario,
// independientemente del médico, especialidad u hora.
//
// CÓMO SE DEFINE "EL MISMO DÍA":
// Se usa el día completo como rango: desde el inicio hasta el final del día.
//   inicioDia = fecha.toLocalDate().atStartOfDay()  → ej: 2025-06-09T00:00:00
//   finalDia  = fecha.toLocalDate().atTime(LocalTime.MAX) → ej: 2025-06-09T23:59:59.999999999
//
// QUERY USADA:
// existsByPacienteIdAndFechaBetweenAndMotivoCancelacionIsNull()
//   → paciente_id = idPaciente
//   → fecha BETWEEN inicioDia AND finalDia (cualquier hora del mismo día)
//   → motivo_cancelacion IS NULL (solo consultas activas, no canceladas)
//
// MOTIVO DE "IS NULL":
// Una consulta cancelada no debería contar como "consulta del día".
// Si un paciente cancela su consulta de la mañana, puede agendar
// una nueva en la tarde. Sin el filtro IS NULL, la consulta cancelada
// seguiría bloqueando el día completo.
//
// RELACIÓN CON ValidadorDeMedicoConOtraConsultaEnMismoHorario:
// Este validador es más restrictivo — bloquea TODO el día del paciente.
// El de médico solo bloquea el slot exacto. Son complementarios y
// cada uno protege su propia entidad (paciente vs médico).
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.ConsultaRepository;
import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.infra.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ValidadorDePacienteSinMasConsultasElMismoDia implements ValidadorDeConsultas {

    // Repositorio para verificar consultas existentes del paciente en ese día.
    @Autowired
    ConsultaRepository consultaRepository;

    @Override
    public void validar(DatosRegistroConsulta datos){

        // Define el rango del día completo para la búsqueda.
        // atStartOfDay() → 00:00:00.000000000 del día de la consulta.
        var inicioDia = datos.fecha().toLocalDate().atStartOfDay();

        // LocalTime.MAX → 23:59:59.999999999 — el último nanosegundo del día.
        // Garantiza que cualquier consulta en ese día caiga dentro del rango.
        var finalDia = datos.fecha().toLocalDate().atTime(LocalTime.MAX);

        // Consulta BD: ¿tiene el paciente alguna consulta activa en ese día?
        // BETWEEN es inclusivo en ambos extremos.
        // IS NULL filtra solo consultas no canceladas.
        var consultaActiva = consultaRepository
                .existsByPacienteIdAndFechaBetweenAndMotivoCancelacionIsNull(
                        datos.idPaciente(), inicioDia, finalDia
                );

        // Si ya tiene consulta activa ese día → no puede agendar otra → 409 CONFLICT.
        if (consultaActiva){
            throw new BusinessException(
                    "No se puede agendar más de una consulta por día",
                    HttpStatus.CONFLICT
            );
        }
        // Si no tiene consulta ese día → no hace nada, continúa.
    }
}