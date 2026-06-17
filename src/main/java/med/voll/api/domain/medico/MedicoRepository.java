package med.voll.api.domain.medico;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

//Métodos CRUD automáticos
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    //Devuelve página de médicos activos, mostrando el page, el size y el sort que se defina en el otro metodo que llama a este
    Page<Medico> findAllByActivoTrue(Pageable paginacion);

    //Busca un médico por email y en caso de existir devuelve el objeto completo
    Optional<Medico> findByEmailIgnoreCase(String email);

    //Busca un médico por documento y en caso de existir devuelve el objeto completo
    Optional<Medico> findByDocumento(String documento);

    //Seleccionar un médico aleatorio de la especialidad requerida por el paciente en la fecha que este indica
    @Query("""
                SELECT m FROM Medico m
                WHERE m.activo = true
                AND
                m.especialidad = :especialidad
                AND
                m.id not in(
                SELECT c.medico.id FROM Consulta c
                WHERE 
                c.fecha = :fecha
                AND
                c.motivoCancelacion is null
                )
                ORDER BY rand()
                LIMIT 1
    """)
    Medico elegirMedicoAleatorioDisponibleEnLaFecha(@NotNull @Future LocalDateTime fecha, Especialidad especialidad);

    //Buscar si el médico está activo en la base de datos
    @Query("""
    SELECT m.activo
        FROM Medico m
            WHERE m.id = :id
    """)
    Boolean findActivoById(@Param("id") Long id);
}
