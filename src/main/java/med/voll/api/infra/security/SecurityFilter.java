package med.voll.api.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import med.voll.api.domain.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//Esta clase intenta autenticar las requests mediante JWT
//OncePerRequestFilter garantiza que el filtro se ejecute una sola vez por request HTTP
//El token se valida una sola vez y la autenticación se setea una sola vez, evitando inconsistencias
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    UsuarioRepository repository;

    @Autowired
    TokenService tokenService;

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            //Mediante el metodo 'recuperarToken()' se toma el Header Authorization del request y se extrae el token
            var tokenJWT = recuperarToken(request);

            //Se verifica si se envió un token
            //Si hay token, se intenta autenticar
            //Si no hay token, fallará la autorización -> 401
            if (tokenJWT != null) {

                //Se valida el token y se extrae el subject en el metodo 'getSubject' de la clase TokenService
                //Si falla el metodo 'getSubject()' lanza exception y la atrapa este catch con su respectivo mensaje
                var subject = tokenService.getSubject(tokenJWT);

                //Se busca el usuario en la BD dado su subject
                //Si el usuario no existe más en nuestra BD se lanza una BadCredentialsException
                var usuario = repository.findByLoginIgnoreCase(subject)
                        .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

                //Se crea una representación de un usuario autenticado
                var authentication = new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );

                //Se indica a Spring que este request ya está autenticado
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            /*
            Si algo de lo anterior falló se pasa al catch:
            token inválido -> Lo valida y responde TokenService
            token expirado -> Lo valida y responde TokenService
            usuario no existe -> BD -> Responde la excepción que capturó el error
            error inesperado
             */
        } catch (Exception e) {
            //log del error (sólo dev):
            logger.warn("Error validando JWT: {}", e.getMessage(), e);

            //Se limpia cualquier autenticación previa y evita estados inconsistentes
            SecurityContextHolder.clearContext();

            //No lanzamos excepción para que el sistema sea flexible y no bloquee el acceso
            //a las partes públicas de la API cuando hay un token viejo "flotando" en las cabeceras del navegador.
        }

        //Continúa la ejecución y le pasa la request y la response al siguiente filtro para que pueda leerla/modificarla y así durante toda la cadena HTTP
        filterChain.doFilter(request, response);
    }

    //Metodo para retornar el token del Header Authorization en caso de existir
    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader =  request.getHeader("Authorization");

        //Verifica que empieza con "Bearer " y retorna null si el formato es incorrecto
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            //Substring es más eficiente que .replace()
            var token = authorizationHeader.substring(7);

            //Si el token no existe sale y retorna null
            if (!token.isBlank()){
                return token;
            }
        }

        return null;
    }

}
