package med.voll.api.domain.usuario;

public record DatosDetalleUsuario(
        Long id,
        String login,
        String rol
){
    public DatosDetalleUsuario(Usuario usuario){
        this(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getRol()
        );
    }
}
