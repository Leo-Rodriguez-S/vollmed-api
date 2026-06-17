package med.voll.api.domain.medico;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import med.voll.api.domain.direccion.Direccion;

//Marca clase como entidad JPA
//Hibernate la gestiona (persiste, carga, y actualiza)
@Entity

//Mapea entidad a tabla específica con nombre "medicos"
@Table(name = "medicos")

//Lombok genera métodos Getter para todos los campos.
//No hay Setter porque se busca la inmutabilidad parcial, controlando cómo y cuándo se actualizan los campos,
//utilizando únicamente métodos específicos como 'eliminar()' o 'actualizarInformaciones()'
@Getter

//Genera constructor sin parámetros para que Hibernate pueda cargar entidad de BD
@NoArgsConstructor

//Genera constructor con todos los campos.
//En este caso posteriormente será utilizado en nuestro constructor personalizado para crear un objeto
@AllArgsConstructor

//Genera metodo equals y hashCode para comparar objetos en memoria
@EqualsAndHashCode(of = "id")


public class Medico {

    //Primary key
    //único y obligatorio
    @Id
    //ID generado automáticamente por BD de forma autoincremental
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Campos básicos sin anotaciones especiales
    private Boolean activo;
    private String nombre;
    private String email;
    private String documento;
    private String telefono;

    //Guarda el nombre del ENUM de manera segura (agregar nuevos valores no afecta existentes) y legible
    @Enumerated(EnumType.STRING)
    private Especialidad especialidad;

    //Incrusta objeto en la misma tabla de medicos
    @Embedded
    private Direccion direccion;

    public Medico(DatosRegistroMedico datos) {
        this(
                null, //MySQL lo asigna automáticamente de manera autoincremental
                true, //Medicos nuevos siempre activos
                datos.nombre(),
                datos.email(),
                datos.documento(),
                datos.telefono(),
                datos.especialidad(),
                new Direccion(datos.datosDireccion()) //Convierte DTO DatosDireccion a entidad encapsulada Direccion
        );
    }

    //Actualización parcial de datos
    //Se verifica null para que únicamente modifiquemos aquellos campos que vienen acompañados de un valor válido y lo null no sobreescriba el valor anterior
    public void actualizarInformaciones(DatosActualizacionMedico datos) {
        if (datos.nombre() != null) {
            this.nombre = datos.nombre();
        }
        if (datos.telefono() != null) {
            this.telefono = datos.telefono();
        }
        //Se delega a objeto embebido para que realice su propia actualización parcial de datos bajo la misma lógica si es que hay datos por actualizar
        if (datos.direccion() != null) {
            this.direccion.actualizarDireccion(datos.direccion());
        }
    }

    //Se marca el objeto como inactivo en lugar de eliminarlo
    public void eliminar() {
        this.activo = false;
    }
}
