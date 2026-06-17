package med.voll.api.domain.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements UserDetailsService {

    @Autowired
    UsuarioRepository usuarioRepository;

    //Metodo utilizado por AuthenticationManager para buscar si un usuario existe
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //Buscamos usuario y devolvemos objeto UserDetails completo (login, contraseña hasheada, roles, authorities) en caso de existir
        return usuarioRepository.findByLoginIgnoreCase(username)
                //En caso de no existir lanzamos exception UsernameNotFoundException
                //AuthenticationManager convierte a BadCredentialsException
                //Lo captura el GestorDeErrores -> 401
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
    }

}
