// ══════════════════════════════════════════════════════════════════════
// ARCHIVO: Consulta.java
// PAQUETE: med.voll.api.domain.consulta
// ══════════════════════════════════════════════════════════════════════
//
// RESPONSABILIDAD:
// Entidad JPA que representa una consulta médica en la base de datos.
// Es el núcleo del dominio de consultas — conecta un paciente con un
// médico en una fecha y hora específica.
//
// TABLA EN BD: consultas
// Columnas: id, paciente_id (FK), medico_id (FK), fecha, motivo_cancelacion
//
// CANCELACIÓN LÓGICA — NO ELIMINACIÓN FÍSICA:
// Las consultas nunca se borran de la BD. Cuando se cancela una consulta,
// se setea el campo motivoCancelacion con el motivo del enum.
// Una consulta activa tiene motivoCancelacion = null.
// Una consulta cancelada tiene motivoCancelacion = PACIENTE_DESISTIO | MEDICO_CANCELO | OTROS.
// Esto permite auditoría completa del historial de consultas.
//
// RELACIONES JPA:
//   ManyToOne con Paciente: muchas consultas pueden pertenecer a un paciente.
//   ManyToOne con Medico:   muchas consultas pueden pertenecer a un médico.
//   FetchType.LAZY: no carga el objeto relacionado hasta que se accede
//   explícitamente. Evita N+1 queries y mejora el rendimiento.
//   @JoinColumn define el nombre de la columna FK en la tabla consultas.
//
// ANOTACIONES LOMBOK:
//   @Getter          → genera getters para todos los campos.
//   @NoArgsConstructor → constructor sin argumentos (requerido por JPA/Hibernate).
//   @AllArgsConstructor → constructor con todos los campos (usado en tests).
//   @EqualsAndHashCode(of = "id") → equals y hashCode basados solo en id.
//                                    Dos consultas son iguales si tienen el mismo id.
//
// ══════════════════════════════════════════════════════════════════════

package med.voll.api.domain.consulta;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import med.voll.api.domain.medico.Medico;
import med.voll.api.domain.paciente.Paciente;

import java.time.LocalDateTime;

// @Entity marca esta clase como entidad JPA — Hibernate la mapea a una tabla.
@Entity

// @Table especifica el nombre exacto de la tabla en BD.
// Sin esta anotación Hibernate usaría "consulta" (nombre de la clase en minúsculas).
@Table(name = "consultas")

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Consulta {

    // ══════════════════════════════════════════════════════════════
    // CAMPOS — mapeo con la tabla consultas
    // ══════════════════════════════════════════════════════════════

    // PK autoincremental generada por la BD (MySQL AUTO_INCREMENT).
    // Hibernate nunca asigna el id manualmente — lo obtiene de la BD
    // tras el INSERT.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación muchos-a-uno con Paciente.
    // FetchType.LAZY: no carga el Paciente hasta que se acceda a paciente.getNombre() u otro campo.
    // @JoinColumn: la columna FK en la tabla consultas se llama "paciente_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    // Relación muchos-a-uno con Medico.
    // Misma lógica que paciente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    // Fecha y hora de la consulta.
    // Siempre en hora exacta gracias a ValidadorDeSlotVacio.
    // Dentro del horario laboral gracias a ValidadorDeFueraHorarioLaboral.
    private LocalDateTime fecha;

    // Motivo de cancelación — null si la consulta está activa.
    // @Column especifica el nombre de la columna en BD.
    // @Enumerated(EnumType.STRING): guarda el enum como String en BD
    // ("PACIENTE_DESISTIO", "MEDICO_CANCELO", "OTROS") en lugar de
    // índice numérico (0, 1, 2). Más legible y resistente a reordenamientos.
    @Column(name = "motivo_cancelacion")
    @Enumerated(EnumType.STRING)
    private MotivoCancelacion motivoCancelacion;

    // ══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ══════════════════════════════════════════════════════════════

    /*
     * Constructor de conveniencia para crear consultas nuevas.
     * Las consultas nuevas siempre tienen id = null (la BD lo genera)
     * y motivoCancelacion = null (están activas).
     *
     * Delega al constructor @AllArgsConstructor con null en id y motivoCancelacion.
     * Usado en ReservaDeConsulta.reservarConsulta():
     *   new Consulta(null, paciente, medico, datos.fecha(), null)
     *   → se puede simplificar a:
     *   new Consulta(paciente, medico, datos.fecha())
     */
    public Consulta(Paciente paciente, Medico medico, LocalDateTime fecha) {
        this(
                null,           // id → null, la BD lo genera
                paciente,
                medico,
                fecha,
                null            // motivoCancelacion → null = consulta activa
        );
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ══════════════════════════════════════════════════════════════

    /*
     * Cancelación lógica de la consulta.
     * No elimina el registro — setea el motivo de cancelación.
     *
     * Después de llamar cancelar():
     *   motivoCancelacion != null → la consulta ya no aparece en
     *   queries que filtran por motivoCancelacion IS NULL.
     *
     * Las queries de validación (existsByMedicoIdAndFecha...,
     * existsByPacienteIdAndFechaBetween...) incluyen
     * "AndMotivoCancelacionIsNull" para ignorar consultas canceladas.
     *
     * @param motivoCancelacion  El motivo del enum: PACIENTE_DESISTIO,
     *                           MEDICO_CANCELO u OTROS.
     */
    public void cancelar(MotivoCancelacion motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }
}