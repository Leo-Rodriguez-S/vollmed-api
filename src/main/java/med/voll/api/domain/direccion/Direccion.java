package med.voll.api.domain.direccion;

import jakarta.persistence.Embeddable;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Direccion {
    private String calle;
    private String numero;
    private String complemento;
    private String barrio;
    private String ciudad;
    private String codigoPostal;
    private String estado;

    public Direccion(DatosDireccion datosDireccion) {
        this(
                datosDireccion.calle(),
                datosDireccion.numero(),
                datosDireccion.complemento(),
                datosDireccion.barrio(),
                datosDireccion.ciudad(),
                datosDireccion.codigoPostal(),
                datosDireccion.estado()
        );
    }

    public void actualizarDireccion(DatosDireccion direccion) {
        if (direccion.calle() != null) {
            this.calle = direccion.calle();
        }
        if (direccion.numero() != null) {
            this.numero = direccion.numero();
        }
        if (direccion.complemento() != null) {
            this.complemento = direccion.complemento();
        }
        if (direccion.barrio() != null) {
            this.barrio = direccion.barrio();
        }
        if (direccion.ciudad() != null) {
            this.ciudad = direccion.ciudad();
        }
        if (direccion.codigoPostal() != null) {
            this.codigoPostal = direccion.codigoPostal();
        }
        if (direccion.estado() != null) {
            this.estado = direccion.estado();
        }

    }
}
