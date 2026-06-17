package med.voll.api.domain.paciente;

import jakarta.persistence.*;
import lombok.*;
import med.voll.api.domain.direccion.Direccion;

/*
CASI IDENTICO QUE LA CLASE MEDICO, PERO CON LA SIGUIENTE DIFERENCIA:
├─ SIN especialidad
*/

@Entity
@Table(name = "pacientes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean activo;
    private String nombre;
    private String email;
    private String telefono;
    private String documento;
    @Embedded
    private Direccion direccion;

    public Paciente(DatosRegistroPaciente datos) {
        this(
                null,
                true,
                datos.nombre(),
                datos.email(),
                datos.telefono(),
                datos.documento(),
                new Direccion(datos.datosDireccion())
        );
    }

    public void actualizarDatos(DatosActualizacionPaciente datos) {
        if (datos.nombre() != null) {
            this.nombre = datos.nombre();
        }
        if (datos.telefono() != null) {
            this.telefono = datos.telefono();
        }
        if (datos.direccion() != null) {
            this.direccion.actualizarDireccion(datos.direccion());
        }
    }

    public void eliminar() {
        this.activo = false;
    }
}

