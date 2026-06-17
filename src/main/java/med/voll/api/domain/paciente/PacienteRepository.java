package med.voll.api.domain.paciente;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

//Métodos CRUD automáticos
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    //Devuelve página de pacientes activos, mostrando el page, el size y el sort que se defina en el otro metodo que llama a este
    Page<Paciente> findAllByActivoTrue(Pageable paginacion);

    //Busca un paciente por email y en caso de existir devuelve el objeto completo
    Optional<Paciente> findByEmailIgnoreCase(String email);

    //Busca un paciente por documento y en caso de existir devuelve el objeto completo
    Optional<Paciente> findByDocumento(String documentoIdentidad);

    //Busca un paciente por telefono y en caso de existir devuelve el objeto completo
    Optional<Paciente> findByTelefono(String telefono);

    //Busca si un paciente está activo
    @Query("""
    SELECT p.activo 
        FROM Paciente p
        WHERE p.id = :id
    """)
    Boolean findActivoById(@Param("id") Long id);
}
