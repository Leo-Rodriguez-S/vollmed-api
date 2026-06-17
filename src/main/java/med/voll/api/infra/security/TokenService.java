package med.voll.api.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import med.voll.api.domain.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

//Clase para generar y validar tokens
@Service
public class TokenService {

    //El valor se encuentra en application.properties, en una variable de entorno
    @Value("${api.security.token.secret}")
    private String secret;

    //Metodo para generar tokens en caso de que no existan problemas con la autenticación
    //Si existiesen problemas con la autenticación, el AuthenticationManager lanza una BadCredentialsException y el GestorDeErrores lo captura
    public String generarToken(Usuario usuario){
            //Se utiliza el algoritmo HMAC256 con una clave secreta para firmar los tokens
            var algoritmo = Algorithm.HMAC256(secret);

            //Se devuelve un JWT con Issuer, Subject, fecha de expiración y firma
            return JWT.create()
                    .withIssuer("API Voll.med")
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(fechaExpiracion())
                    .sign(algoritmo);
    }

    //Metodo para generar la fecha de expiración
    public Instant fechaExpiracion(){
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-06:00"));
    }

    //Metodo para validar token y obtener su subject para autenticación de requests en SecurityFilter
    public String getSubject(String tokenJWT){
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("API Voll.med")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception){
            throw new BadCredentialsException(exception.getMessage());
        }
    }

}
