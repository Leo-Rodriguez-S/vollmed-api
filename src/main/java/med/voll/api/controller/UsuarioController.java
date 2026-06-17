package med.voll.api.controller;

import jakarta.validation.Valid;
import med.voll.api.domain.usuario.*;
import med.voll.api.infra.exceptions.DuplicadoException;
import med.voll.api.infra.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

//Registro de usuarios
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    TokenService tokenService;

    //Metodo para crear un usuario nuevo
    @PostMapping
    @Transactional
    public ResponseEntity crearUsuario(@RequestBody @Valid DatosRegistroUsuario datos, UriComponentsBuilder uriBuilder){

        //Validar que login no exista
        var login = datos.login().trim().toLowerCase();
        if (repository.existsByLoginIgnoreCase(login)) {
            throw new DuplicadoException(
                    "El login ya existe en el sistema."
            );
        }

        //Hashear contraseña
        var contrasenaHasheada = passwordEncoder.encode(datos.contrasena());

        //Crear usuario
        var usuario = new Usuario(datos, contrasenaHasheada);

        //Guardar nuevo usuario en BD
        repository.save(usuario);

        //Construir URI
        var uri = uriBuilder.path("/usuarios/{id}").buildAndExpand(usuario.getId()).toUri();

        //Generar token
        var tokenJWT = tokenService.generarToken(usuario);

        //Retornar 201 created
        return ResponseEntity.created(uri).body(
                new  DatosRespuestaRegistro(
                        new DatosDetalleUsuario(usuario),
                        tokenJWT
                )
        );
    }

}
