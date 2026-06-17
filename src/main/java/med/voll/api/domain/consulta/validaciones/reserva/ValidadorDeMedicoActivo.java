// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ValidadorDeMedicoActivo.java
// PAQUETE: med.voll.api.domain.consulta.validaciones.reserva
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Valida que el médico especificado en la reserva esté activo (no haya
// sido eliminado lógicamente del sistema). Un médico inactivo no puede
// recibir nuevas consultas.
//
// GUARD CLAUSE — idMedico null:
// En el sistema, idMedico es opcional. Si es null significa que el
// cliente no especificó médico y el sistema elegirá uno aleatoriamente
// (en ReservaDeConsulta.elegirMedico()). En ese caso, este validador
// no aplica y retorna inmediatamente sin validar.
// Si idMedico viene con valor → sí valida que ese médico esté activo.
//
// VALIDACIÓN EN BACKEND:
// Aunque el frontend podría no mostrar médicos inactivos, alguien
// podría llamar directamente a la API con el id de un médico inactivo.
// Esta validación garantiza integridad sin importar el cliente.
//
// NOTA DE MEJORA — orden de la guard clause:
// La guard clause (if idMedico == null → return) debería ir ANTES
// de la consulta a BD para evitar ejecutar findActivoById() con null,
// lo que podría causar NullPointerException. En el código actual el
// orden es: consulta BD → guard → validación. Funciona porque Java
// evalúa la guard antes de usar medicoActivo, pero es más limpio
// hacer la guard primero.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta.validaciones.reserva;

import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.domain.medico.MedicoRepository;
import med.voll.api.infra.exceptions.RecursoInactivoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidadorDeMedicoActivo implements ValidadorDeConsultas {

    // Repositorio para consultar el estado activo del médico en BD.
    @Autowired
    MedicoRepository medicoRepository;

    @Override
    public void validar(DatosRegistroConsulta datos) {

        // Consulta BD: ¿el médico con este id tiene activo = true?
        // findActivoById() es una query personalizada en MedicoRepository
        // que retorna Boolean directamente.
        var medicoActivo = medicoRepository.findActivoById(datos.idMedico());

        // Guard clause: si no se especificó médico, no hay nada que validar.
        // El sistema elegirá un médico activo aleatoriamente más adelante.
        if (datos.idMedico() == null) {
            return;
        }

        // Si el médico existe pero está inactivo (soft delete) → no puede recibir consultas.
        // RecursoInactivoException es capturada por GestorDeErrores → respuesta HTTP apropiada.
        if (!medicoActivo) {
            throw new RecursoInactivoException("No se pueden agendar citas con médicos inactivos");
        }
        // Si activo = true → no hace nada, continúa al siguiente validador.
    }
}