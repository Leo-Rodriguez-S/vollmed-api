package med.voll.api.domain.usuario;

public record DatosRespuestaRegistro(
        DatosDetalleUsuario usuario,
        String tokenJWT
        ) {
}
