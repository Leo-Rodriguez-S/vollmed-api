package med.voll.api.domain.medico;

import jakarta.persistence.EntityManager;
import med.voll.api.domain.consulta.Consulta;
import med.voll.api.domain.direccion.DatosDireccion;
import med.voll.api.domain.paciente.DatosRegistroPaciente;
import med.voll.api.domain.paciente.Paciente;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("test")
class MedicoRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MedicoRepository medicoRepository;

    @Test
    @DisplayName("El método debe de retornar null al intentar agendar una cita con un médico que ya tiene ocupado ese slot")
    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario1() {
        //Given
        var lunesSiguienteALas10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
        var paciente = registrarPaciente("Lidia Artavia Rojas", "lidiartrojas@gmail.com", "123456789", "88990077");
        var medico = registrarMedico("Leonardo Rodríguez", "leonardo.rodriguez@voll.med", "712344640", "87675633", Especialidad.ORTOPEDIA);
        registrarConsulta(medico, paciente,  lunesSiguienteALas10);

        //When
        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(lunesSiguienteALas10, Especialidad.ORTOPEDIA);

        //Then
        Assertions.assertThat(medicoLibre).isNull();
    }

    @Test
    @DisplayName("El método debe de retornar un médico disponible")
    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario2() {
        //Given
        var lunesSiguienteALas10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
        var medico = registrarMedico("Leonardo Rodríguez", "leonardo.rodriguez@voll.med", "712344640", "87675633", Especialidad.ORTOPEDIA);

        //When
        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(lunesSiguienteALas10, Especialidad.ORTOPEDIA);

        //Then
        Assertions.assertThat(medicoLibre).isEqualTo(medico);
    }

    @Test
    @DisplayName("El método NO debe de retornar el médico guardado en la BD porque está inactivo")
    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario3() {
        //Given
        var lunesSiguienteALas10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10, 0);
        var medico = registrarMedico("Leonardo Rodríguez", "leonardo.rodriguez@voll.med", "712344640", "87675633", Especialidad.ORTOPEDIA);
        medico.eliminar();

        //When
        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(lunesSiguienteALas10, Especialidad.ORTOPEDIA);

        //Then
        Assertions.assertThat(medicoLibre).isNull();
    }

    @Test
    @DisplayName("El método debe de retornar null cuando no hay médicos de la especialidad buscada")
    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario4() {
        //Given
        var lunesSiguienteALas10 = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atTime(10, 0);

        //Registramos médico de ORTOPEDIA
        registrarMedico("Leonardo Rodríguez", "leonardo.rodriguez@voll.med",
                "712344640", "87675633", Especialidad.ORTOPEDIA);

        //When -- Buscamos médico de CARDIOLOGÍA
        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(
                lunesSiguienteALas10, Especialidad.CARDIOLOGIA);

        //Then
        Assertions.assertThat(medicoLibre).isNull();
    }


    @Test
    @DisplayName("Debe retornar null cuando no hay médicos registrados")
    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario5() {
        //Given
        var lunesSiguienteALas10 = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atTime(10, 0);

        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(lunesSiguienteALas10,  Especialidad.ORTOPEDIA);

        Assertions.assertThat(medicoLibre).isNull();
    }

    private void registrarConsulta(Medico medico, Paciente paciente, LocalDateTime fecha) {
        var consulta = new Consulta(paciente, medico, fecha);
        entityManager.persist(consulta);
    }

    private Medico registrarMedico(String nombre, String email, String documento, String telefono, Especialidad especialidad) {
        var medico = new Medico(datosMedico(nombre, email, documento, telefono, especialidad));
        entityManager.persist(medico);
        return medico;
    }

    private Paciente registrarPaciente(String nombre, String email, String documento, String telefono){
        var paciente = new Paciente(datosPaciente(nombre, email, documento, telefono));
        entityManager.persist(paciente);
        return paciente;
    }


    private DatosRegistroMedico datosMedico(String nombre, String email, String documento, String telefono, Especialidad especialidad) {
        return new DatosRegistroMedico(
                nombre,
                email,
                documento,
                telefono,
                especialidad,
                datosDireccion()
        );
    }

    private DatosRegistroPaciente datosPaciente(String nombre, String email, String documento, String telefono) {
        return new DatosRegistroPaciente(
                nombre,
                email,
                documento,
                telefono,
                datosDireccion()
        );
    }

    private DatosDireccion datosDireccion() {
        return new DatosDireccion(
                "Test",
                "123",
                "Data JPA Test",
                "API Voll.med",
                "Maven",
                "10190",
                "Backend"
        );
    }
}