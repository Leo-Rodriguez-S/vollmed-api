// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDePacienteActivo.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que el paciente que solicita la consulta esté activo en el
// sistema. Un paciente inactivo (eliminado lógicamente) no puede
// agendar nuevas consultas.
//
// A DIFERENCIA DE ValidadorDeMedicoActivo:
// El idPaciente siempre viene en el request (@NotNull en DatosRegistroConsulta).
// No hay guard clause — siempre se valida.
//
// System.out.println(pacienteActivo):
// Este print es un debug temporal que quedó del desarrollo.
// En producción debería reemplazarse por un logger:
//   log.debug("Estado activo del paciente {}: {}", datos.idPaciente(), pacienteActivo);
// No afecta el funcionamiento pero genera ruido en los logs de producción.
//
// VALIDACIÓN EN BACKEND:
// Aunque la UI no debería mostrar pacientes inactivos para agendar
// consultas, alguien podría llamar la API directamente con el id
// de un paciente inactivo. Esta validación cierra esa brecha.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.domain.paciente.PacienteRepository;
import med.voll.api.infra.exceptions.RecursoInactivoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidadorDePacienteActivo implements ValidadorDeConsultas {

    // Repositorio para consultar el estado activo del paciente en BD.
    @Autowired
    PacienteRepository pacienteRepository;

    @Override
    public void validar(DatosRegistroConsulta datos){

        // Consulta BD: ¿el paciente con este id tiene activo = true?
        // findActivoById() es una query personalizada en PacienteRepository.
        var pacienteActivo = pacienteRepository.findActivoById(datos.idPaciente());

        // Debug temporal — idealmente reemplazar por logger en producción.
        System.out.println(pacienteActivo);

        // Si el paciente está inactivo → no puede agendar consultas → excepción.
        // RecursoInactivoException es capturada por GestorDeErrores.
        if(!pacienteActivo){
            throw new RecursoInactivoException("No se pueden agendar citas a pacientes inactivos.");
        }
        // Si activo = true → no hace nada, continúa al siguiente validador.
    }
}