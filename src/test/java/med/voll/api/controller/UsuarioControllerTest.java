package med.voll.api.controller;

import med.voll.api.domain.usuario.DatosRegistroUsuario;
import med.voll.api.domain.usuario.UsuarioRepository;
import med.voll.api.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/*
================================================================================
CLASE DE TESTS - USUARIO CONTROLLER
================================================================================

OBJETIVO GENERAL
-----------------
Esta clase valida el comportamiento del endpoint:

    POST /usuarios

del UsuarioController.

Se prueban escenarios de creación de usuarios bajo diferentes condiciones:

1. Request inválido (sin body)
2. Usuario duplicado
3. Registro exitoso con generación de JWT

================================================================================
TIPO DE TEST
================================================================================

Esto es un:

- Spring Boot Integration Test (ligero)
- con MockMvc
- con dependencias parcialmente mockeadas

LEVANTA:
--------
✔ Spring context completo
✔ Controllers reales
✔ Validaciones (@Valid)
✔ Serialización JSON (Jackson real)
✔ Security filters (si aplican)

MOCKEA:
-------
✘ UsuarioRepository
✘ TokenService

NO se prueba:
-------------
- Persistencia real en base de datos
- Generación real de JWT
- Lógica interna del repository
- Password encoding real

================================================================================
ARQUITECTURA DE LOS TESTS
================================================================================

Se sigue un patrón de escenarios:

ESCENARIO 1:
------------
Validación de request inválido
→ Spring intercepta antes de entrar al controller
→ Respuesta esperada: 400 BAD REQUEST

ESCENARIO 2:
------------
Regla de negocio: usuario ya existe
→ usuarioRepository.existsByLoginIgnoreCase() = true
→ Respuesta esperada: 409 CONFLICT

ESCENARIO 3:
------------
Flujo feliz de creación de usuario
→ Usuario creado correctamente
→ Token generado
→ Respuesta esperada: 201 CREATED + JWT

================================================================================
MOCKMVC
================================================================================

MockMvc permite simular requests HTTP reales sin servidor.

Simula:
-------
POST /usuarios
Headers
Body JSON
DispatcherServlet
Controllers
Validation layer

NO levanta Tomcat real.

================================================================================
MOCKITO (@MockitoBean)
================================================================================

Se utilizan mocks para aislar dependencias externas:

UsuarioRepository:
------------------
Simula acceso a base de datos

TokenService:
--------------
Simula generación de JWT

Esto permite:
- control total del comportamiento
- tests determinísticos
- independencia de infraestructura

================================================================================
IMPORTANCIA DE ESTOS TESTS
================================================================================

Estos tests aseguran:

✔ Correcta validación de entrada
✔ Reglas de negocio del controller
✔ Integración MVC correcta
✔ Respuestas HTTP correctas
✔ Interacción con servicios externos

================================================================================
*/

@SpringBootTest
/*
Carga el contexto completo de Spring Boot.
Permite ejecutar tests de integración realistas.
*/
@AutoConfigureMockMvc
/*
Configura MockMvc para simular requests HTTP.
*/
@AutoConfigureJsonTesters
/*
Habilita JacksonTester para serialización JSON en tests.
*/
class UsuarioControllerTest {

    @Autowired
    private JacksonTester<DatosRegistroUsuario> datosRegistroUsuarioJson;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private TokenService tokenService;

    /*
    ============================================================================
    ESCENARIO 1
    ============================================================================
    OBJETIVO:
    ---------
    Validar que el endpoint rechaza requests sin body.

    FLUJO:
    ------
    POST /usuarios (sin JSON)
        ↓
    Spring no puede deserializar DatosRegistroUsuario
        ↓
    Error de validación / binding
        ↓
    HTTP 400 BAD REQUEST

    NOTA:
    -----
    El controller ni siquiera se ejecuta.
    ============================================================================
    */
    @Test
    @DisplayName("Debe devolver 400 cuando el body esté vacío")
    void crearUsuarioEscenario1() throws Exception {
        var response = mockMvc.perform(post("/usuarios"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /*
    ============================================================================
    ESCENARIO 2
    ============================================================================
    OBJETIVO:
    ---------
    Validar regla de negocio:
    "No se puede registrar un usuario con login ya existente"

    FLUJO:
    ------
    Controller recibe request válido
        ↓
    usuarioRepository.existsByLoginIgnoreCase(login)
        ↓
    devuelve true (mock)
        ↓
    Controller responde:
        HTTP 409 CONFLICT

    SIGNIFICADO HTTP 409:
    ---------------------
    Conflicto de estado en el recurso (usuario duplicado)
    ============================================================================
    */
    @Test
    @DisplayName("Debe devolver 409 si el login ya existe")
    void crearUsuarioEscenario2() throws Exception {

        var datos = new DatosRegistroUsuario(
                "testing@voll.med",
                "P4s$w0rd"
        );

        when(usuarioRepository.existsByLoginIgnoreCase(datos.login()))
                .thenReturn(true);

        var response = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroUsuarioJson.write(datos).getJson())
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    /*
    ============================================================================
    ESCENARIO 3
    ============================================================================
    OBJETIVO:
    ---------
    Validar flujo exitoso de creación de usuario.

    FLUJO COMPLETO SIMULADO:
    ------------------------

    1. Request válido llega al controller
    2. Usuario se registra correctamente
    3. TokenService genera JWT
    4. Controller devuelve 201 CREATED

    MOCK IMPORTANTE:
    -----------------
    tokenService.generarToken(any())
        ↓
    retorna token simulado

    VERIFICACIONES:
    ---------------
    ✔ HTTP status 201 CREATED
    ✔ TokenService fue invocado
    ✔ Response contiene token simulado

    SIGNIFICADO HTTP 201:
    ---------------------
    Recurso creado exitosamente
    ============================================================================
    */
    @Test
    @DisplayName("Debe devolver 200 OK cuando el usuario se logra registrar")
    void crearUsuarioEscenario3() throws Exception {
        var datos = new DatosRegistroUsuario(
                "testing@voll.med",
                "P4s$w0rd"
        );

        when(tokenService.generarToken(any()))
                .thenReturn("TOKEN JWT SIMULADO");

        var response = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroUsuarioJson.write(datos).getJson())
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());

        verify(tokenService).generarToken(any());

        var json = response.getContentAsString();
        assertThat(json).contains("TOKEN JWT SIMULADO");
    }
}