package med.voll.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

//Métodos CRUD automáticos
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    //Busca un usuario por login, ignorando mayúsculas y minúsculas
    //En caso de existir devuelve el objeto completo tipado como UserDetails para que SpringSecurity pueda utilizarlo
    Optional<UserDetails> findByLoginIgnoreCase(String login);

    //Busca si un usuario existe dado su login, ignorando mayúsculas y minúsculas
    boolean existsByLoginIgnoreCase(String login);
}
