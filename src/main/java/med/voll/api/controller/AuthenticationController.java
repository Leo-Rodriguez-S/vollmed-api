package med.voll.api.controller;

import jakarta.validation.Valid;
import med.voll.api.domain.usuario.DatosAutenticacion;
import med.voll.api.domain.usuario.DatosTokenJWT;
import med.voll.api.domain.usuario.Usuario;
import med.voll.api.infra.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AuthenticationController {

    @Autowired
    TokenService tokenService;

    @Autowired
    AuthenticationManager manager;

    @PostMapping
    public ResponseEntity iniciarSesion(@RequestBody @Valid DatosAutenticacion datos){
        //Crea un objeto que contiene los datos que se desean autenticar
        var authenticationToken = new UsernamePasswordAuthenticationToken(datos.login(), datos.contrasena());

        /*
        Se autentican los datos...
        Flujo de authenticate():
        manager.authenticate(authenticationToken)
            ↓
        AuthenticationManager delega a AuthenticationProvider
            ↓
        AuthenticationProvider usa UserDetailsService
            ↓
        AuthenticationService.loadUserByUsername(login)
            ↓
        Usuario encontrado en BD
            ↓
        PasswordEncoder.matches(password_plano, password_hash)
            ↓
        Usa los métodos de la clase que implementa UserDetails (Usuario) para:
        -Validar estado
        -Construir Authentication final
            ↓
        Is everything OK? Sí → Authentication con usuario exitosa. Contiene usuario, roles, estado
        Is everything OK? No → BadCredentialsException
        */
        var autenticacion = manager.authenticate(authenticationToken);

        //Se genera el token al usuario mediante el metodo 'generarToken' en tokenService
        //Se pasa el objeto resultado de la autenticacion casteado a Usuario (como lo pide TokenService) en su metodo
        var tokenJWT = tokenService.generarToken((Usuario) autenticacion.getPrincipal());

        //Se devuelve el token generado anteriormente en el formato dado en el DTO 'DatosTokenJWT'
        return ResponseEntity.ok(new DatosTokenJWT(tokenJWT));
    }

}
