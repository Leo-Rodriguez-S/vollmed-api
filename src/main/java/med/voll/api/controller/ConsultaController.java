// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: ConsultaController.java
// PAQUETE: med.voll.api.controller
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Punto de entrada HTTP para todo lo relacionado con consultas médicas.
// Es el controller más delgado del sistema — no contiene lógica de
// negocio. Delega completamente a ReservaDeConsulta (@Service), que
// es quien ejecuta validadores, elige médico y persiste en BD.
//
// PRINCIPIO SOLID APLICADO — Single Responsibility:
// El controller solo sabe recibir HTTP y devolver respuestas.
// Todo lo demás es responsabilidad de la capa de servicio.
//
// ENDPOINTS EXPUESTOS:
//   POST   /consultas  → reservar una consulta médica   → 200 OK
//   DELETE /consultas  → cancelar una consulta médica   → 204 NO CONTENT
//
// SEGURIDAD:
// Todos los endpoints requieren autenticación JWT (anyRequest().authenticated()).
// No requieren rol ADMIN — cualquier usuario autenticado puede operar.
// @SecurityRequirement vincula este controller al esquema "bearer-key"
// de SpringDoc para mostrar el candadito en Swagger UI.
//
// FLUJO COMPLETO DE UNA RESERVA:
//   Cliente → POST /consultas + JWT + JSON
//       ↓
//   Spring Security valida JWT → 401 si inválido
//       ↓
//   Bean Validation valida @NotNull, @Future → 400 si inválido
//       ↓
//   crearConsulta() delega a reserva.reservarConsulta()
//       ↓
//   ReservaDeConsulta ejecuta validadores → 409 si regla de negocio falla
//       ↓
//   Persiste consulta en BD → devuelve DatosDetalleConsulta → 200 OK
//
// FLUJO COMPLETO DE UN CANCELAMIENTO:
//   Cliente → DELETE /consultas + JWT + JSON
//       ↓
//   Spring Security valida JWT → 401 si inválido
//       ↓
//   Bean Validation valida @NotNull → 400 si inválido
//       ↓
//   eliminarConsulta() delega a reserva.cancelarConsulta()
//       ↓
//   ReservaDeConsulta valida anticipación → 409 si < 24 horas
//       ↓
//   Setea motivoCancelacion en la consulta → 204 NO CONTENT
//
// NOTA SOBRE @Transactional:
// Garantiza que si algo falla durante la operación, todos los cambios
// en BD se revierten automáticamente (rollback). En reserva: si la
// consulta se guarda pero falla algo posterior, se deshace. En
// cancelamiento: si se setea el motivo pero falla algo, se revierte.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import med.voll.api.domain.consulta.DatosCancelamientoConsulta;
import med.voll.api.domain.consulta.DatosRegistroConsulta;
import med.voll.api.domain.consulta.ReservaDeConsulta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

// Marca esta clase como controller REST — todos los métodos retornan
// JSON automáticamente (equivale a @Controller + @ResponseBody).
@RestController

// Prefijo base de todos los endpoints de este controller.
@RequestMapping("/consultas")

// Vincula este controller al esquema de seguridad "bearer-key" definido
// en SpringDocConfiguration. Hace que Swagger UI muestre el candado 🔒
// en cada endpoint, indicando que requieren token JWT en el header
// Authorization: Bearer {token}.
@SecurityRequirement(name = "bearer-key")
public class ConsultaController {

    // ══════════════════════════════════════════════════════════════
    // DEPENDENCIAS
    // ══════════════════════════════════════════════════════════════

    // Servicio que encapsula toda la lógica de negocio de consultas.
    // Inyectado por Spring — no instanciado manualmente.
    // Contiene: validadores, elección de médico, persistencia.
    @Autowired
    ReservaDeConsulta reserva;

    // ══════════════════════════════════════════════════════════════
    // ENDPOINTS
    // ══════════════════════════════════════════════════════════════

    /*
     * POST /consultas — Reservar una consulta médica
     * ─────────────────────────────────────────────
     * Recibe los datos de registro, ejecuta todas las validaciones
     * de negocio y persiste la consulta en BD si todo es válido.
     *
     * @RequestBody  → deserializa el JSON del body a DatosRegistroConsulta
     * @Valid        → activa Bean Validation sobre el objeto deserializado.
     *                 Si falla → 400 BAD REQUEST antes de entrar al método.
     * @Transactional → envuelve la operación en una transacción BD.
     *                  Rollback automático si algo falla.
     *
     * CAMPOS DEL REQUEST:
     *   idMedico    → opcional. Si null, el sistema elige médico aleatorio.
     *   idPaciente  → obligatorio (@NotNull)
     *   fecha       → obligatorio (@NotNull), debe ser futura (@Future)
     *                 formato: "dd/MM/yyyy HH:mm" (@JsonFormat)
     *                 debe ser en hora exacta (07:00, 08:00...) — validado por ValidadorDeSlotVacio
     *   especialidad → requerida si idMedico es null (validado en ReservaDeConsulta)
     *
     * RESPUESTAS:
     *   200 OK              → consulta reservada exitosamente + DatosDetalleConsulta en body
     *   400 BAD REQUEST     → campos obligatorios ausentes o fecha en el pasado
     *   401 UNAUTHORIZED    → token JWT ausente, inválido o expirado
     *   404 NOT FOUND       → médico o paciente no existen en BD
     *   409 CONFLICT        → algún validador de negocio falló:
     *                         · Menos de 30 min de anticipación
     *                         · Fuera de horario laboral (lun-sáb 7-18, sin almuerzo 13-14)
     *                         · Hora no exacta (ej: 7:09)
     *                         · Médico inactivo
     *                         · Paciente inactivo
     *                         · Médico ocupado en ese slot
     *                         · Paciente ya tiene consulta ese día
     *                         · No hay médicos disponibles
     */
    @PostMapping
    @Transactional
    public ResponseEntity crearConsulta(@RequestBody @Valid DatosRegistroConsulta datos){

        // Delega completamente a ReservaDeConsulta.
        // Si cualquier validador lanza excepción, GestorDeErrores la atrapa
        // y retorna el código HTTP correspondiente sin llegar al return.
        var detalleConsulta = reserva.reservarConsulta(datos);

        // 200 OK con el detalle de la consulta creada en el body.
        // Incluye: id, idMedico, idPaciente, fecha formateada, especialidad.
        return ResponseEntity.ok(detalleConsulta);
    }

    /*
     * DELETE /consultas — Cancelar una consulta médica
     * ──────────────────────────────────────────────────
     * Recibe el id de la consulta y el motivo de cancelación.
     * No elimina el registro de BD — es una cancelación lógica:
     * setea motivoCancelacion en la entidad Consulta, que queda
     * registrado para auditoría y deja de aparecer en queries activas.
     *
     * NOTA ARQUITECTÓNICA:
     * A diferencia del patrón REST convencional donde DELETE usa
     * path variable (/consultas/{id}), aquí se usa @RequestBody
     * porque además del id se necesita el motivo de cancelación,
     * que no puede ir en la URL.
     *
     * @RequestBody  → deserializa el JSON a DatosCancelamientoConsulta
     * @Valid        → valida @NotNull en idConsulta y motivoCancelacion
     * @Transactional → rollback automático si algo falla
     *
     * CAMPOS DEL REQUEST:
     *   idConsulta         → obligatorio (@NotNull) — id de la consulta a cancelar
     *   motivoCancelacion  → obligatorio (@NotNull) — enum: PACIENTE_DESISTIO,
     *                        MEDICO_CANCELO, OTROS. Case-insensitive gracias
     *                        a @JsonCreator en MotivoCancelacion.
     *
     * RESPUESTAS:
     *   204 NO CONTENT   → cancelación exitosa (sin body en response)
     *   400 BAD REQUEST  → campos obligatorios ausentes o enum inválido
     *   401 UNAUTHORIZED → token JWT ausente, inválido o expirado
     *   404 NOT FOUND    → la consulta con ese id no existe en BD
     *   409 CONFLICT     → cancelamiento con menos de 24h de anticipación
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity eliminarConsulta(@RequestBody @Valid DatosCancelamientoConsulta datos){

        // Delega a ReservaDeConsulta que verifica existencia,
        // ejecuta validadores de cancelamiento y setea el motivo.
        reserva.cancelarConsulta(datos);

        // 204 NO CONTENT — operación exitosa sin body.
        // Es el estándar HTTP para operaciones de eliminación/cancelación.
        return ResponseEntity.noContent().build();
    }

}