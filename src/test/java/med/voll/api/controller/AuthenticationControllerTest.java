package med.voll.api.controller;

import med.voll.api.domain.usuario.DatosAutenticacion;
import med.voll.api.domain.usuario.Usuario;
import med.voll.api.infra.security.TokenService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/*
================================================================================
CLASE DE TESTS DE INTEGRACIÓN - AUTHENTICATION CONTROLLER
================================================================================

¿Qué se prueba aquí?
--------------------
Esta clase prueba el endpoint HTTP POST /login del AuthenticationController.

Se verifica:
1. Validación del request (body requerido)
2. Flujo exitoso de autenticación
3. Integración entre:
   - MockMvc
   - Spring Security
   - Controller
   - JSON serialization
   - AuthenticationManager mockeado
   - TokenService mockeado

IMPORTANTE:
------------
NO se prueba la autenticación REAL contra base de datos.

¿Por qué?
Porque AuthenticationManager y TokenService están MOCKEADOS.

Entonces:
- NO se consulta BD
- NO se ejecuta UserDetailsService real
- NO se valida password BCrypt real
- NO se genera JWT real

Lo que sí se prueba:
- Que el controller responda correctamente
- Que el endpoint funcione
- Que Spring MVC procese bien el request
- Que los mocks interactúen correctamente
- Que el HTTP status esperado sea correcto

Tipo de test:
--------------
Esto es un:
- Integration Test liviano
o también conocido como:
- Slice Integration Test

Porque:
- Levanta contexto Spring
- Usa MockMvc real
- Usa serialización JSON real
- Pero mockea dependencias externas

================================================================================
*/

@SpringBootTest
/*
@SpringBootTest
----------------

Levanta el contexto COMPLETO de Spring Boot.

Incluye:
- Controllers
- Security config
- Beans
- Filters
- Contexto web
- Serialización JSON
- etc.

Es más pesado que un unit test, pero más realista.

Sin esto:
- MockMvc no funcionaría correctamente
- No existiría el endpoint /login
*/
@AutoConfigureMockMvc
/*
@AutoConfigureMockMvc
----------------------

Inyecta y configura MockMvc automáticamente.

MockMvc:
---------
Permite simular requests HTTP SIN levantar servidor real.

Es decir:
NO inicia Tomcat real.
NO abre puertos.

Pero sí ejecuta:
- DispatcherServlet
- Controllers
- Filtros
- Serialización JSON
- Spring MVC completo

Es una simulación HTTP interna muy rápida.
*/
@AutoConfigureJsonTesters
/*
@AutoConfigureJsonTesters
--------------------------

Configura automáticamente herramientas JSON de Spring Boot.

En este caso:
JacksonTester<DatosAutenticacion>

Permite:
- Convertir objetos Java → JSON
- Convertir JSON → objetos Java

Usa Jackson internamente.
*/
class AuthenticationControllerTest {

    @Autowired
    /*
    MockMvc inyectado por Spring.

    Es el componente principal del test.

    Se usa para:
    - Simular requests HTTP
    - Ejecutar endpoints
    - Obtener responses

    Ejemplo:
    mockMvc.perform(post("/login"))
    */
    private MockMvc mockMvc;

    @Autowired
    /*
    JacksonTester especializado para DatosAutenticacion.

    Permite serializar el DTO a JSON automáticamente.

    Ejemplo:
    datosAutenticacionJson.write(datos).getJson()

    Resultado:
    {
      "login":"testing@voll.med",
      "contrasena":"P4s$w0Rd"
    }

    Ventaja:
    --------
    Evita escribir JSON manualmente.
    */
    private JacksonTester<DatosAutenticacion> datosAutenticacionJson;

    @MockitoBean
    /*
    Mock del AuthenticationManager REAL de Spring Security.

    ¿Por qué mockearlo?
    -------------------
    Porque NO queremos probar:
    - Base de datos
    - PasswordEncoder
    - UserDetailsService
    - BCrypt
    - Seguridad real

    Sólo queremos probar:
    - El controller
    - El flujo HTTP

    Entonces:
    manager.authenticate(...)
    devolverá lo que nosotros configuremos.
    */
    private AuthenticationManager authenticationManager;

    @MockitoBean
    /*
    Mock del TokenService.

    ¿Por qué mockearlo?
    -------------------
    Porque NO queremos generar JWT reales.

    Sólo queremos:
    - Simular que se generó un token
    - Verificar que el endpoint responde bien

    Entonces:
    tokenService.generarToken(...)
    devolverá el token falso configurado.
    */
    private TokenService tokenService;

    @Test
    @DisplayName("Debe devolver 400 cuando no envía body")
    /*
    ESCENARIO 1
    ===========

    Objetivo:
    ----------
    Verificar que el endpoint /login valide correctamente
    cuando NO se envía body.

    Flujo esperado:
    ----------------
    POST /login
    sin JSON
        ↓
    @RequestBody @Valid DatosAutenticacion datos
        ↓
    Spring detecta body faltante
        ↓
    HttpMessageNotReadableException
        ↓
    Response HTTP 400 BAD REQUEST

    IMPORTANTE:
    ------------
    El controller NI SIQUIERA entra al método iniciarSesion().

    El error ocurre ANTES,
    durante el binding/deserialización del request.
    */
    void iniciarSesionEscenario1() throws Exception {

        /*
        Simula:
        POST /login

        SIN body.
        */
        var response = mockMvc.perform(post("/login"))
                .andReturn()
                .getResponse();

        /*
        Verifica:
        HTTP Status == 400

        value():
        retorna el int del enum HttpStatus.

        HttpStatus.BAD_REQUEST.value()
        == 400
        */
        Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    @DisplayName("Debe devolver 200 cuando las credenciales sean válidas")
    /*
    ESCENARIO 2
    ===========

    Objetivo:
    ----------
    Verificar el flujo exitoso de autenticación.

    IMPORTANTE:
    ------------
    NO se autentica realmente.

    El AuthenticationManager está mockeado.

    Entonces:
    - No hay BD
    - No hay BCrypt
    - No hay UserDetailsService real

    Estamos simulando una autenticación exitosa.

    Flujo del test:
    ----------------

    1. Crear DTO login
    2. Crear Usuario fake autenticado
    3. Crear Authentication fake
    4. Configurar mocks
    5. Simular POST /login
    6. Verificar HTTP 200

    Flujo REAL simulado:
    ---------------------

    Controller:
    manager.authenticate(authenticationToken)
        ↓
    Mockito intercepta llamada
        ↓
    retorna autenticacion fake
        ↓
    controller obtiene:
    autenticacion.getPrincipal()
        ↓
    retorna usuario fake
        ↓
    tokenService.generarToken(usuario)
        ↓
    Mockito retorna token fake
        ↓
    ResponseEntity.ok(...)
        ↓
    HTTP 200
    */
    void iniciarSesionEscenario2() throws Exception {

        /*
        DTO usado como body del request.

        Representa:
        {
          "login":"testing@voll.med",
          "contrasena":"P4s$w0Rd"
        }
        */
        var datos = new DatosAutenticacion("testing@voll.med", "P4s$w0Rd");

        /*
        Usuario fake autenticado.

        IMPORTANTE:
        ------------
        Este objeto será el PRINCIPAL del Authentication.

        En Spring Security:
        authentication.getPrincipal()
        retorna el usuario autenticado.

        En tu controller:
        (Usuario) autenticacion.getPrincipal()

        Por eso aquí el principal DEBE ser un Usuario.
        */
        var usuario = new Usuario(datos.login(), datos.contrasena(), "ROLE_USER");

        /*
        Authentication fake exitoso.

        UsernamePasswordAuthenticationToken:
        ------------------------------------

        Representa autenticación autenticada exitosamente.

        Parámetros:
        ------------
        principal:
            usuario autenticado

        credentials:
            null
            (ya no importa después de autenticarse)

        authorities:
            roles/permisos del usuario

        Este objeto es exactamente lo que Spring Security
        devolvería después de autenticar correctamente.
        */
        var autenticacion = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        );

        /*
        Configuración Mockito:
        ----------------------

        Cuando:
        authenticationManager.authenticate(any())

        sea llamado...

        Entonces:
        retornar autenticacion fake.

        any():
        ------
        matcher que significa:
        "cualquier argumento"

        No importa qué Authentication reciba.
        */
        when(authenticationManager.authenticate(any()))
                .thenReturn(autenticacion);

        /*
        Configuración Mockito:
        ----------------------

        Cuando:
        tokenService.generarToken(any())

        sea llamado...

        Entonces:
        retornar string fake.

        Simula JWT generado.
        */
        when(tokenService.generarToken(any()))
                .thenReturn("TOKEN JWT SIMULADO");

        /*
        Simulación HTTP REAL usando MockMvc.

        Equivale a:

        POST /login
        Content-Type: application/json

        {
          "login":"testing@voll.med",
          "contrasena":"P4s$w0Rd"
        }

        contentType(MediaType.APPLICATION_JSON):
        ----------------------------------------
        indica que el body es JSON.

        content(...):
        -------------
        body serializado.

        datosAutenticacionJson.write(datos).getJson():
        ------------------------------------------------
        convierte DTO → JSON.
        */
        var response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosAutenticacionJson.write(datos).getJson())
                )
                .andReturn()
                .getResponse();

        /*
        Verificación final:
        -------------------

        Esperamos:
        HTTP 200 OK

        Porque:
        - autenticación fue exitosa
        - token fue generado
        - controller retornó ResponseEntity.ok(...)
        */
        Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Debe devolver 401 cuando el usuario no existe o sus credenciales son inválidas")
/*
================================================================================
ESCENARIO 3 - AUTENTICACIÓN FALLIDA
================================================================================

Objetivo:
----------
Verificar que el endpoint /login responda correctamente
cuando las credenciales son inválidas.

Casos representados:
---------------------
- Usuario no existe
- Password incorrecto
- Login inválido
- Usuario eliminado
- Credenciales incorrectas en general

IMPORTANTE:
------------
NO se ejecuta autenticación REAL.

El AuthenticationManager está MOCKEADO.

Entonces:
- NO se consulta base de datos
- NO se usa BCrypt real
- NO se ejecuta UserDetailsService
- NO se valida password real

Estamos simulando el comportamiento de Spring Security
cuando falla la autenticación.

================================================================================
FLUJO REAL DE SPRING SECURITY
================================================================================

En producción ocurriría algo así:

manager.authenticate(authenticationToken)
    ↓
AuthenticationProvider
    ↓
UserDetailsService.loadUserByUsername()
    ↓
Usuario no encontrado
    O
PasswordEncoder.matches() == false
    ↓
Spring lanza:
BadCredentialsException
    ↓
ExceptionTranslationFilter
    ↓
AuthenticationEntryPoint
    ↓
HTTP 401 Unauthorized

================================================================================
QUÉ ESTAMOS HACIENDO EN EL TEST
================================================================================

En lugar de ejecutar toda la autenticación real:

MOCKEAMOS directamente el resultado final del fallo.

Es decir:

Cuando:
authenticationManager.authenticate(...)

sea llamado...

Mockito lanzará:
BadCredentialsException

Simulando exactamente lo que haría Spring Security
si las credenciales fueran incorrectas.

================================================================================
¿POR QUÉ 401 Y NO 400?
================================================================================

400 BAD REQUEST:
----------------
El request está mal construido.
Ejemplo:
- JSON inválido
- Body faltante
- Campos requeridos faltantes

401 UNAUTHORIZED:
------------------
El request es válido,
pero el usuario NO está autenticado.

En este caso:
- El JSON es correcto
- El endpoint existe
- La estructura del request es válida
- Pero las credenciales fallan

Entonces corresponde:
HTTP 401 Unauthorized

================================================================================
*/
    void iniciarSesionEscenario3() throws Exception {

    /*
    DTO enviado como body del request.

    Representa:
    {
      "login":"testing@voll.med",
      "contrasena":"P4s$w0Rd"
    }

    IMPORTANTE:
    ------------
    El formato es válido.

    El problema NO es el request.
    El problema son las credenciales.
    */
        var datos = new DatosAutenticacion("testing@voll.med", "P4s$w0Rd");

    /*
    Configuración Mockito:
    ----------------------

    Cuando:
    authenticationManager.authenticate(any())

    sea ejecutado...

    Entonces:
    lanzar BadCredentialsException.

    any():
    ------
    matcher que significa:
    "sin importar el argumento recibido"

    Simulación conceptual:
    ----------------------

    manager.authenticate(...)
        ↓
    usuario no encontrado
        O
    password incorrecto
        ↓
    Spring Security lanza:
    BadCredentialsException

    Estamos reproduciendo exactamente ese comportamiento.
    */
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

    /*
    Simulación HTTP del request.

    Equivale a:

    POST /login
    Content-Type: application/json

    {
      "login":"testing@voll.med",
      "contrasena":"P4s$w0Rd"
    }

    MockMvc ejecuta:
    - DispatcherServlet
    - Filters
    - Controller
    - Spring Security
    - Manejo de excepciones

    Todo SIN levantar servidor real.
    */
        var response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosAutenticacionJson.write(datos).getJson()))
                .andReturn()
                .getResponse();

    /*
    Verificación final:
    -------------------

    Esperamos:
    HTTP 401 Unauthorized

    Porque:
    - El request es válido
    - Pero la autenticación falló

    Spring Security transforma automáticamente:
    BadCredentialsException
        ↓
    HTTP 401
    */
        Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}