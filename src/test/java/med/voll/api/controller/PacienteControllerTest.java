package med.voll.api.controller;

/*
 * =====================================================================
 * CLASE: PacienteControllerTest
 * =====================================================================
 * Integration test de la capa web para PacienteController.
 *
 * Estrategia: se simula HTTP con MockMvc sin levantar un servidor
 * real, y se aísla el controller mockeando PacienteRepository para que
 * el test no toque la base de datos.
 *
 * Diferencia con ConsultaControllerTest:
 * PacienteController llama directamente a PacienteRepository sin pasar
 * por un servicio intermedio, por eso se mockea el repositorio
 * directamente en lugar de un servicio.
 *
 * Pirámide de testing: Integration Test.
 *
 * Cobertura:
 *   POST   /pacientes       → escenarios 1-6
 *   GET    /pacientes       → escenarios 1-3
 *   GET    /pacientes/{id}  → escenarios 1-4
 *   PUT    /pacientes/{id}  → escenarios 1-5
 *   DELETE /pacientes/{id}  → escenarios 1-5
 * =====================================================================
 */

import med.voll.api.domain.direccion.DatosDireccion;
import med.voll.api.domain.direccion.Direccion;
import med.voll.api.domain.paciente.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/*
 * @SpringBootTest
 * Levanta el contexto completo de Spring para el test.
 * Necesario para que la inyección de dependencias, Spring Security
 * y todos los beans estén disponibles.
 *
 * @AutoConfigureMockMvc
 * Habilita y autoconfigura MockMvc — el "Insomnia del código".
 * Permite simular requests HTTP sin levantar un servidor real.
 *
 * @AutoConfigureJsonTesters
 * Habilita JacksonTester, que convierte objetos Java a JSON string
 * usando el mismo ObjectMapper configurado en la aplicación.
 * Garantiza consistencia de formato (fechas, campos, orden).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class PacienteControllerTest {

    /*
     * MockMvc: el simulador de HTTP.
     * Equivale a Insomnia pero dentro del código.
     * Spring lo inyecta automáticamente gracias a @AutoConfigureMockMvc.
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * JacksonTester<DatosRegistroPaciente>
     * Serializa DatosRegistroPaciente → JSON string para enviarlo
     * como body en los requests de POST /pacientes.
     */
    @Autowired
    private JacksonTester<DatosRegistroPaciente> datosRegistroPacienteJson;

    /*
     * JacksonTester<DatosDetallePaciente>
     * Serializa DatosDetallePaciente → JSON string para compararlo
     * con el body del response en el escenario 201.
     */
    @Autowired
    private JacksonTester<DatosDetallePaciente> datosDetallePacienteJson;

    /*
     * JacksonTester<DatosActualizacionPaciente>
     * Serializa DatosActualizacionPaciente → JSON string para enviarlo
     * como body en los requests de PUT /pacientes/{id}.
     */
    @Autowired
    private JacksonTester<DatosActualizacionPaciente> datosActualizacionPacienteJson;

    /*
     * @MockitoBean
     * Reemplaza el bean real de PacienteRepository en el contexto de
     * Spring con una versión falsa controlada por Mockito.
     *
     * ¿Por qué mockear el repositorio y no un servicio?
     * PacienteController llama directamente a PacienteRepository sin pasar
     * por una capa de servicio intermedia. Por eso el mock va sobre
     * el repositorio directamente.
     *
     * Comportamiento por defecto: devuelve null hasta que se programe
     * con when(...).thenReturn(...).
     *
     * Nota: reemplaza al deprecado @MockBean desde Spring Boot 3.4.
     */
    @MockitoBean
    private PacienteRepository pacienteRepository;

    // ══════════════════════════════════════════════════════════════
    // POST /pacientes
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin body + ADMIN → 400 BAD REQUEST
     * ──────────────────────────────────────────────────
     * Verifica que Bean Validation rechaza el request cuando no se
     * envía body. Los campos @NotNull de DatosRegistroPaciente están
     * ausentes. El controller nunca llega a ejecutarse.
     *
     * @WithMockUser(roles = "ADMIN") — necesario para pasar Security
     * y llegar a Bean Validation. Sin él obtendríamos 403.
     */
    @Test
    @DisplayName("Debe devolver 400 - Bad Request si no se envía un JSON válido")
    @WithMockUser(roles = "ADMIN")
    void registrarPacienteEscenario1() throws Exception {
        var response = mockMvc.perform(post("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /*
     * ESCENARIO 2 — Sin autenticación → 401 UNAUTHORIZED
     * ─────────────────────────────────────────────────────
     * Verifica que Spring Security bloquea el request cuando no hay
     * usuario autenticado. El authenticationEntryPoint devuelve 401
     * antes de llegar al controller.
     *
     * Sin @WithMockUser — no hay usuario autenticado.
     */
    @Test
    @DisplayName("Debe devolver 401 UNAUTHORIZED si el usuario no está autenticado")
    void registrarPacienteEscenario2() throws Exception {
        var response = mockMvc.perform(post("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 3 — Autenticado sin rol ADMIN → 403 FORBIDDEN
     * ──────────────────────────────────────────────────────────
     * Verifica que Spring Security bloquea el request cuando el usuario
     * está autenticado pero no tiene el rol requerido.
     *
     * @WithMockUser(roles = "USER") — autenticado pero sin ADMIN.
     * SecurityConfiguration: hasRole("ADMIN") para POST /pacientes.
     * El accessDeniedHandler devuelve 403.
     *
     * Diferencia con escenario 2:
     *   Escenario 2 → no autenticado          → 401
     *   Escenario 3 → autenticado sin permiso → 403
     */
    @Test
    @DisplayName("Debe devolver 403 FORBIDDEN si el usuario está autenticado pero no autorizado")
    @WithMockUser(roles = "USER")
    void registrarPacienteEscenario3() throws Exception {
        var response = mockMvc.perform(post("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /*
     * ESCENARIO 4 — Body válido + ADMIN → 201 CREATED
     * ──────────────────────────────────────────────────
     * Happy path. Verifica que el controller:
     *   1. Acepta el JSON válido
     *   2. Llama a pacienteRepository.save()
     *   3. Devuelve el JSON del médico creado con HTTP 201
     *
     * El mock de save() devuelve un Paciente construido desde datosRegistro,
     * simulando lo que haría la BD sin tocarla.
     * id = null porque no hay BD real que lo genere con AUTO_INCREMENT.
     *
     * new Direccion(datosRegistro.datosDireccion()) porque el response
     * devuelve la entidad Direccion, no el record DatosDireccion del request.
     */
    @Test
    @DisplayName("Debe devolver un 201 Created si el usuario está autenticado y autorizado y la validación pasó")
    @WithMockUser(roles = "ADMIN")
    void registrarPacienteEscenario4() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.save(any())).thenReturn(new Paciente(datosRegistro));

        var response = mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroPacienteJson.write(datosRegistro).getJson())
                )
                .andReturn()
                .getResponse();

        var datosDetalle = new DatosDetallePaciente(
                null,
                datosRegistro.nombre(),
                datosRegistro.email(),
                datosRegistro.documento(),
                datosRegistro.telefono(),
                new Direccion(datosRegistro.datosDireccion())
        );

        var jsonEsperado = datosDetallePacienteJson.write(datosDetalle).getJson();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonEsperado);
    }

    /*
     * ESCENARIO 5 — Email duplicado → 409 CONFLICT
     * ──────────────────────────────────────────────
     * Verifica que validarExistente() lanza DuplicadoException cuando
     * findByEmailIgnoreCase() encuentra un médico con ese email.
     *
     * El mock devuelve Optional.of(paciente) — caja con médico adentro —
     * simulando que el email ya existe en BD.
     * DuplicadoException es capturada por @ExceptionHandler → 409.
     *
     * La excepción se lanza ANTES de save() — el médico nunca se guarda.
     */
    @Test
    @DisplayName("Debe lanzar 409 cuando el email ya está registrado")
    @WithMockUser(roles = "ADMIN")
    void registrarPacienteEscenario5() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.findByEmailIgnoreCase(datosRegistro.email()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroPacienteJson.write(datosRegistro).getJson())
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    /*
     * ESCENARIO 6 — Documento duplicado → 409 CONFLICT
     * ───────────────────────────────────────────────────
     * Verifica que validarExistente() lanza DuplicadoException cuando
     * findByDocumento() encuentra un médico con ese documento.
     *
     * findByEmailIgnoreCase → Optional.empty() — email libre, no lanza excepción.
     * findByDocumento → Optional.of(paciente) — documento duplicado → lanza excepción.
     *
     * El email debe estar libre para que el controller llegue a validar
     * el documento — si el email estuviera duplicado, la excepción se
     * lanzaría antes y nunca llegaría a validar el documento.
     */
    @Test
    @DisplayName("Debe lanzar 409 cuando el documento ya está registrado")
    @WithMockUser(roles = "ADMIN")
    void registrarPacienteEscenario6() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.findByEmailIgnoreCase(datosRegistro.email()))
                .thenReturn(Optional.empty());
        when(pacienteRepository.findByDocumento(datosRegistro.documento()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroPacienteJson.write(datosRegistro).getJson())
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Debe lanzar 409 cuando el teléfono ya está registrado")
    @WithMockUser(roles = "ADMIN")
    void registrarPacienteEscenario7() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );


        when(pacienteRepository.findByEmailIgnoreCase(datosRegistro.email()))
                .thenReturn(Optional.empty());
        when(pacienteRepository.findByDocumento(datosRegistro.documento()))
                .thenReturn(Optional.empty());
        when(pacienteRepository.findByTelefono(datosRegistro.telefono()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(post("/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(datosRegistroPacienteJson.write(datosRegistro).getJson())
        )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    // ══════════════════════════════════════════════════════════════
    // GET /pacientes
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin autenticación → 401 UNAUTHORIZED
     * ─────────────────────────────────────────────────────
     * Spring Security bloquea con 401 antes de llegar al controller.
     */
    @Test
    @DisplayName("Debe devolver 401 UNAUTHORIZED cuando se intente un get /pacientes")
    void consultarPacienteEscenario1() throws Exception {
        var response = mockMvc.perform(get("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 2 — Autenticado sin ADMIN → 403 FORBIDDEN
     * ──────────────────────────────────────────────────────
     * Usuario autenticado con rol USER — sin ADMIN.
     * SecurityConfiguration: hasRole("ADMIN") para GET /pacientes → 403.
     */
    @Test
    @DisplayName("Debe devolver 403 FORBIDDEN cuando se intente un get /pacientes")
    @WithMockUser
    void consultarPacienteEscenario3() throws Exception {
        var response = mockMvc.perform(get("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /*
     * ESCENARIO 3 — ADMIN + página vacía → 200 OK
     * ─────────────────────────────────────────────
     * Happy path. Verifica que el endpoint responde 200 con una
     * página vacía. No se verifican médicos específicos — solo que
     * el endpoint funciona correctamente con ADMIN autenticado.
     *
     * PageImpl(List.of()) — implementación concreta de Page con
     * lista vacía. No necesitamos médicos reales para verificar el 200.
     */
    @Test
    @DisplayName("Debe devolver 200 OK cuando se intente un get /pacientes")
    @WithMockUser(roles = "ADMIN")
    void consultarPacienteEscenario5() throws Exception {
        when(pacienteRepository.findAllByActivoTrue(any()))
                .thenReturn(new PageImpl<>(List.of()));

        var response = mockMvc.perform(get("/pacientes"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // ══════════════════════════════════════════════════════════════
    // GET /pacientes/{id}
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin autenticación → 401 UNAUTHORIZED
     */
    @Test
    @DisplayName("Debe devolver 401 UNAUTHORIZED cuando se intente un get /pacientes/{id}")
    void consultarPacienteEscenario2() throws Exception {
        var response = mockMvc.perform(get("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 2 — Autenticado sin ADMIN → 403 FORBIDDEN
     */
    @Test
    @DisplayName("Debe devolver 403 FORBIDDEN cuando se intente un get /pacientes/{id}")
    @WithMockUser
    void consultarPacienteEscenario4() throws Exception {
        var response = mockMvc.perform(get("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /*
     * ESCENARIO 3 — ADMIN + médico activo → 200 OK
     * ───────────────────────────────────────────────
     * Happy path. findById() devuelve Optional.of(paciente) — médico
     * activo encontrado. No se verifica el body porque incluye links
     * HATEOAS dinámicos que harían la comparación frágil.
     */
    @Test
    @DisplayName("Debe devolver 200 OK cuando se intente un get /pacientes/{id}")
    @WithMockUser(roles = "ADMIN")
    void consultarPacienteEscenario6() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(get("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /*
     * ESCENARIO 4 — Médico no existe → 404 NOT FOUND
     * ─────────────────────────────────────────────────
     * findById() devuelve Optional.empty() — caja vacía.
     * orElseThrow() lanza EntityNotFoundException → @ExceptionHandler → 404.
     *
     * Se usa Optional.empty() y no thenThrow() porque el controller
     * usa orElseThrow — el flujo real pasa por el Optional.
     */
    @Test
    @DisplayName("Debe devolver un 404 NOT FOUND cuando el médico específico no exista")
    @WithMockUser(roles = "ADMIN")
    void consultarPacienteEscenario7() throws Exception {
        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.empty());

        var response = mockMvc.perform(get("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    /*
     * ESCENARIO 5 — Médico inactivo → 404 NOT FOUND
     * ────────────────────────────────────────────────
     * findById() devuelve Optional.of(paciente inactivo).
     * El controller verifica !paciente.getActivo() → true →
     * return ResponseEntity.notFound().build() → 404.
     *
     * paciente.eliminar() desactiva el médico — activo = false.
     * Se devuelve 404 (y no 409) porque el controller trata los
     * médicos inactivos como si no existieran para el cliente.
     */
    @Test
    @DisplayName("Debe devolver un 404 NOT FOUND cuando el médico específico está inactivo")
    @WithMockUser(roles = "ADMIN")
    void consultarPacienteEscenario8() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        var paciente = new Paciente(datosRegistro);
        paciente.eliminar();

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(paciente));

        var response = mockMvc.perform(get("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // ══════════════════════════════════════════════════════════════
    // PUT /pacientes/{id}
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin autenticación → 401 UNAUTHORIZED
     * ─────────────────────────────────────────────────────
     * Corrección: la URL debe incluir {id} → /pacientes/{id}.
     * Sin id la URL no hace match con el @PutMapping("/{id}") y
     * Spring devuelve 405 METHOD NOT ALLOWED en lugar de 401.
     */
    @Test
    @DisplayName("Debe devolver 401 UNAUTHORIZED cuando se intente un put /pacientes/{id}")
    void actualizarPacienteEscenario1() throws Exception {
        var response = mockMvc.perform(put("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 2 — Autenticado sin ADMIN → 403 FORBIDDEN
     */
    @Test
    @DisplayName("Debe devolver 403 FORBIDDEN cuando se intente un put /pacientes/{id}")
    @WithMockUser
    void actualizarPacienteEscenario2() throws Exception {
        var response = mockMvc.perform(put("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /*
     * ESCENARIO 3 — Médico no existe → 404 NOT FOUND
     * ─────────────────────────────────────────────────
     * findById() devuelve Optional.empty() → orElseThrow lanza
     * EntityNotFoundException → @ExceptionHandler → 404.
     * Se envía body para pasar Bean Validation antes de llegar
     * al findById().
     */
    @Test
    @DisplayName("Debe devolver 404 NOT FOUND cuando se intente un put /pacientes/{id} y el mismo no exista")
    @WithMockUser(roles = "ADMIN")
    void actualizarPacienteEscenario3() throws Exception {
        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.empty());

        var response = mockMvc.perform(put("/pacientes/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosActualizacionPacienteJson
                                .write(new DatosActualizacionPaciente(
                                        "Test Testeando Testing",
                                        null,
                                        null
                                ))
                                .getJson()
                        )
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    /*
     * ESCENARIO 4 — Médico inactivo → 409 CONFLICT
     * ───────────────────────────────────────────────
     * findById() devuelve Optional.of(paciente inactivo).
     * El controller verifica !paciente.getActivo() → true →
     * throw RecursoInactivoException → @ExceptionHandler → 409.
     *
     * Diferencia con GET /pacientes/{id} inactivo:
     *   GET  → return ResponseEntity.notFound() → 404 (trata inactivo como inexistente)
     *   PUT  → throw RecursoInactivoException   → 409 (informa que existe pero está inactivo)
     *
     * Se envía body para pasar Bean Validation antes de llegar
     * a la verificación de activo.
     */
    @Test
    @DisplayName("Debe devolver 409 CONFLICT cuando el médico exista pero esté inactivo")
    @WithMockUser(roles = "ADMIN")
    void actualizarPacienteEscenario4() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        var paciente = new Paciente(datosRegistro);
        paciente.eliminar();

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(paciente));

        var response = mockMvc.perform(put("/pacientes/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosActualizacionPacienteJson
                                .write(new DatosActualizacionPaciente(
                                        "Testeando testing testeado",
                                        null,
                                        null
                                ))
                                .getJson()
                        )
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    /*
     * ESCENARIO 5 — Médico activo + datos válidos → 200 OK
     * ───────────────────────────────────────────────────────
     * Happy path. findById() devuelve Optional.of(paciente activo).
     * El controller actualiza el médico y devuelve 200 con el detalle.
     * No se verifica el body — solo el status, porque comparar el
     * JSON exacto de la actualización añade complejidad sin valor adicional.
     *
     * new Paciente(datosRegistro) garantiza activo = true — estado
     * inicial de cualquier médico recién creado.
     */
    @Test
    @DisplayName("Debe devolver 200 OK cuando el médico fuese actualizado exitosamente")
    @WithMockUser(roles = "ADMIN")
    void actualizarPacienteEscenario5() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(put("/pacientes/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosActualizacionPacienteJson
                                .write(new DatosActualizacionPaciente(
                                        "Testeando testing testeado",
                                        null,
                                        null
                                ))
                                .getJson()
                        )
                )
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /pacientes/{id}
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin autenticación → 401 UNAUTHORIZED
     */
    @Test
    @DisplayName("Debe devolver 401 UNAUTHORIZED cuando se intente un delete /pacientes/{id}")
    void eliminarPacienteEscenario1() throws Exception {
        var response = mockMvc.perform(delete("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 2 — Autenticado sin ADMIN → 403 FORBIDDEN
     */
    @Test
    @DisplayName("Debe devolver 403 FORBIDDEN cuando se intente un delete /pacientes/{id}")
    @WithMockUser
    void eliminarPacienteEscenario2() throws Exception {
        var response = mockMvc.perform(delete("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    /*
     * ESCENARIO 3 — Médico no existe → 404 NOT FOUND
     * ─────────────────────────────────────────────────
     * findById() devuelve Optional.empty() → orElseThrow lanza
     * EntityNotFoundException → @ExceptionHandler → 404.
     */
    @Test
    @DisplayName("Debe devolver 404 NOT FOUND cuando se intente un delete /pacientes/{id} y el mismo no exista")
    @WithMockUser(roles = "ADMIN")
    void eliminarPacienteEscenario3() throws Exception {
        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.empty());

        var response = mockMvc.perform(delete("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    /*
     * ESCENARIO 4 — Médico inactivo → 404 NOT FOUND
     * ────────────────────────────────────────────────
     * Tu controller en eliminarPaciente() no verifica si el médico
     * está inactivo — solo llama a paciente.eliminar() directamente.
     * Eliminar un médico ya inactivo no lanza excepción, simplemente
     * devuelve 204. Este escenario es opcional pero documentado
     * para completar la cobertura.
     */
    @Test
    @DisplayName("Debe devolver 204 NO CONTENT al intentar eliminar un médico ya inactivo")
    @WithMockUser(roles = "ADMIN")
    void eliminarPacienteEscenario4() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        var paciente = new Paciente(datosRegistro);
        paciente.eliminar();

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(paciente));

        var response = mockMvc.perform(delete("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    /*
     * ESCENARIO 5 — Médico activo → 204 NO CONTENT
     * ───────────────────────────────────────────────
     * Happy path. findById() devuelve Optional.of(paciente activo).
     * paciente.eliminar() desactiva el médico (eliminación lógica).
     * El controller devuelve ResponseEntity.noContent().build() → 204.
     *
     * new Paciente(datosRegistro) garantiza activo = true.
     */
    @Test
    @DisplayName("Debe devolver un 204 NO CONTENT cuando un delete de UN paciente sea exitoso")
    @WithMockUser(roles = "ADMIN")
    void eliminarPacienteEscenario5() throws Exception {
        var datosRegistro = new DatosRegistroPaciente(
                "Test Testeando Testing",
                "t3sting@voll.med",
                "123456789",
                "88990077",
                datosDireccion()
        );

        when(pacienteRepository.findById(any()))
                .thenReturn(Optional.of(new Paciente(datosRegistro)));

        var response = mockMvc.perform(delete("/pacientes/{id}", 1))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    /*
     * ─────────────────────────────────────────────────────────────
     * MÉTODO AUXILIAR — Test Fixture
     * ─────────────────────────────────────────────────────────────
     * Construye un DatosDireccion con datos inventados válidos
     * para no repetir su construcción en cada test que lo necesite.
     * Los valores no tienen significado de negocio — solo deben ser
     * válidos estructuralmente.
     */
    private DatosDireccion datosDireccion() {
        return new DatosDireccion(
                "Av. Central",
                "00",
                null,
                "Test",
                "Test",
                "12345",
                "Test"
        );
    }
}