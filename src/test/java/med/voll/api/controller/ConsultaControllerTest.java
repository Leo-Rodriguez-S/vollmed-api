package med.voll.api.controller;
/*
 * =====================================================================
 * CLASE: ConsultaControllerTest
 * =====================================================================
 * Integration test de la capa web para ConsultaController.
 *
 * Estrategia: se simula HTTP con MockMvc sin levantar un servidor
 * real, y se aísla el controller mockeando ReservaDeConsulta para que
 * el test no toque la base de datos ni ejecute reglas de negocio.
 *
 * Pirámide de testing: Integration Test.
 *
 * Cobertura:
 *   POST   /consultas → escenarios 1-5
 *   DELETE /consultas → escenarios 1-5
 * =====================================================================
 */

import jakarta.persistence.EntityNotFoundException;
import med.voll.api.domain.consulta.*;
import med.voll.api.domain.medico.Especialidad;
import med.voll.api.infra.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
class ConsultaControllerTest {

    /*
     * Datos compartidos entre tests.
     * Se declaran como atributos de clase para evitar repetición.
     * Son final porque no deben modificarse entre tests.
     *
     * fecha: siempre 2 horas en el futuro para cumplir la regla
     * de anticipación mínima de 30 minutos.
     */
    private final LocalDateTime fecha       = LocalDateTime.now().plusHours(2);
    private final Especialidad especialidad = Especialidad.CARDIOLOGIA;

    /*
     * MockMvc: el simulador de HTTP.
     * Equivale a Insomnia pero dentro del código.
     * Spring lo inyecta automáticamente gracias a @AutoConfigureMockMvc.
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * JacksonTester<DatosRegistroConsulta>
     * Serializa DatosRegistroConsulta → JSON string para enviarlo
     * como body en los requests de POST /consultas.
     *
     * Uso: datosRegistroConsultaJson.write(objeto).getJson()
     *   .write()    → JsonContent (objeto intermedio con el JSON)
     *   .getJson()  → String JSON puro
     */
    @Autowired
    JacksonTester<DatosRegistroConsulta> datosRegistroConsultaJson;

    /*
     * JacksonTester<DatosDetalleConsulta>
     * Serializa DatosDetalleConsulta → JSON string para compararlo
     * con el body del response recibido en el escenario 200.
     */
    @Autowired
    JacksonTester<DatosDetalleConsulta> datosDetalleConsultaJson;

    /*
     * JacksonTester<DatosCancelamientoConsulta>
     * Serializa DatosCancelamientoConsulta → JSON string para enviarlo
     * como body en los requests de DELETE /consultas.
     */
    @Autowired
    JacksonTester<DatosCancelamientoConsulta> datosCancelamientoConsultaJson;

    /*
     * @MockitoBean
     * Reemplaza el bean real de ReservaDeConsulta en el contexto de
     * Spring con una versión falsa controlada por Mockito.
     *
     * ¿Por qué mockearlo?
     * ReservaDeConsulta llama a repositorios que tocan la BD.
     * El objetivo de este test es probar ÚNICAMENTE el controller —
     * no las reglas de negocio ni la persistencia, que se prueban
     * en sus propias clases de test.
     *
     * Comportamiento por defecto del mock:
     *   - Métodos con retorno  → devuelven null
     *   - Métodos void         → no hacen nada (doNothing implícito)
     * Se programa explícitamente con when(...).thenReturn(...)
     * o doThrow(...).when(...).metodo() según el escenario.
     *
     * Nota: reemplaza al deprecado @MockBean desde Spring Boot 3.4.
     */
    @MockitoBean
    ReservaDeConsulta reservaDeConsulta;

    // ══════════════════════════════════════════════════════════════
    // POST /consultas
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin body → 400 BAD REQUEST
     * ─────────────────────────────────────────
     * Verifica que Bean Validation rechaza automáticamente un request
     * sin body porque los campos @NotNull de DatosRegistroConsulta
     * están ausentes. El controller nunca llega a ejecutarse.
     *
     * @WithMockUser
     * Simula un usuario autenticado para que Spring Security no
     * intercepte el request con 401 antes de llegar a Bean Validation.
     */
    @Test
    @DisplayName("Debe devolver un 400 cuando la request no tenga datos")
    @WithMockUser
    void crearConsulta_escenario1() throws Exception {

        // When — POST sin body
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/consultas"))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /*
     * ESCENARIO 2 — Body válido → 200 OK
     * ────────────────────────────────────
     * Verifica que el controller:
     *   1. Acepta un JSON válido
     *   2. Llama a reservaDeConsulta.reservarConsulta()
     *   3. Devuelve el JSON del detalle de consulta con HTTP 200
     *
     * El mock devuelve un DatosDetalleConsulta controlado para que
     * el resultado sea predecible y no dependa de la BD.
     * El id es null porque no hay BD real que lo genere.
     */
    @Test
    @DisplayName("Debe devolver un 200 cuando la request tenga datos validos")
    @WithMockUser
    void crearConsulta_escenario2() throws Exception {

        // Given
        var datosDetalle = new DatosDetalleConsulta(null, 2L, 5L, fecha, especialidad);

        /*
         * Programa el mock: cuando reservarConsulta() sea llamado con
         * cualquier argumento (any()), devolvé datosDetalle.
         * any() hace el test robusto ante cambios en el objeto enviado.
         */
        when(reservaDeConsulta.reservarConsulta(any())).thenReturn(datosDetalle);

        // When — POST con body JSON válido
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/consultas")
                                /*
                                 * contentType: indica que el body es JSON.
                                 * Equivale al header Content-Type: application/json
                                 * que Insomnia agrega automáticamente.
                                 */
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(datosRegistroConsultaJson
                                        .write(new DatosRegistroConsulta(2L, 5L, fecha, especialidad))
                                        .getJson()
                                )
                )
                .andReturn()
                .getResponse();

        // Then — verificar status y body
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.OK.value());

        /*
         * Compara el JSON recibido con el JSON esperado.
         * Ambos son Strings — JacksonTester garantiza el mismo
         * formato que usa la app real (fechas, orden de campos).
         *
         * response.getContentAsString()              → JSON del response
         * datosDetalleConsultaJson.write(...).getJson() → JSON esperado
         */
        assertThat(response.getContentAsString())
                .isEqualTo(datosDetalleConsultaJson.write(datosDetalle).getJson());
    }

    /*
     * ESCENARIO 3 — Sin autenticación → 401 UNAUTHORIZED
     * ─────────────────────────────────────────────────────
     * Verifica que Spring Security bloquea el request cuando no hay
     * usuario autenticado y devuelve 401.
     *
     * Sin @WithMockUser — no hay usuario autenticado.
     * El authenticationEntryPoint de SecurityConfiguration intercepta
     * el request y devuelve 401 antes de llegar al controller.
     */
    @Test
    @DisplayName("Debe devolver un 401 Unauthorized -- Usuario no auténticado")
    void crearConsulta_escenario3() throws Exception {

        // When — POST sin usuario autenticado
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroConsultaJson
                                .write(new DatosRegistroConsulta(2L, 5L, fecha, especialidad))
                                .getJson()
                        )
                )
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 4 — EntityNotFoundException → 404 NOT FOUND
     * ────────────────────────────────────────────────────────
     * Verifica que el @ExceptionHandler captura EntityNotFoundException
     * y devuelve 404. Se lanza cuando el médico o paciente no existe.
     *
     * No se toca la BD — el mock simula directamente la excepción.
     * Lo que se prueba aquí es el manejo de la excepción por el
     * @ExceptionHandler, no la lógica de existencia en BD.
     */
    @Test
    @DisplayName("Debe devolver un 404 Not Found cuando no se encuentra el objeto")
    @WithMockUser
    void crearConsulta_escenario4() throws Exception {

        // Given — mock lanza EntityNotFoundException
        when(reservaDeConsulta.reservarConsulta(any()))
                .thenThrow(new EntityNotFoundException());

        // When
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroConsultaJson
                                .write(new DatosRegistroConsulta(2L, 5L, fecha, especialidad))
                                .getJson()
                        ))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    /*
     * ESCENARIO 5 — BusinessException → 409 CONFLICT
     * ─────────────────────────────────────────────────
     * Verifica que el @ExceptionHandler captura BusinessException
     * y devuelve 409. Se lanza cuando se viola una regla de negocio
     * (slot inválido, fuera de horario, médico ocupado, etc.).
     *
     * No se prueba la regla de negocio específica — eso corresponde
     * a los unit tests de cada validador. Aquí solo se verifica que
     * el @ExceptionHandler maneja correctamente BusinessException.
     */
    @Test
    @DisplayName("Debe de lanzar un 409 cuando se lance una excepción de reglas del negocio (Business Exception)")
    @WithMockUser
    void crearConsulta_escenario5() throws Exception {

        // Given — mock lanza BusinessException
        when(reservaDeConsulta.reservarConsulta(any()))
                .thenThrow(new BusinessException("Business Exception", HttpStatus.CONFLICT));

        // When
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosRegistroConsultaJson
                                .write(new DatosRegistroConsulta(2L, 5L, fecha, especialidad))
                                .getJson()
                        ))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /consultas
    // ══════════════════════════════════════════════════════════════

    /*
     * ESCENARIO 1 — Sin autenticación → 401 UNAUTHORIZED
     * ─────────────────────────────────────────────────────
     * Mismo principio que crearConsulta_escenario3 pero para
     * el endpoint DELETE /consultas.
     * Spring Security bloquea con 401 antes de llegar al controller.
     */
    @Test
    @DisplayName("Debe devolver un 401 Unauthorized -- Usuario no auténticado")
    void eliminarConsulta_escenario1() throws Exception {

        // When — DELETE sin usuario autenticado
        var response = mockMvc.perform(MockMvcRequestBuilders.delete("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosCancelamientoConsultaJson
                                .write(new DatosCancelamientoConsulta(2L, MotivoCancelacion.MEDICO_CANCELO))
                                .getJson()
                        )
                )
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /*
     * ESCENARIO 2 — Sin body → 400 BAD REQUEST
     * ──────────────────────────────────────────
     * Verifica que Bean Validation rechaza el request cuando no se
     * envía body. Los campos @NotNull de DatosCancelamientoConsulta
     * (idConsulta, motivoCancelacion) están ausentes.
     */
    @Test
    @DisplayName("Debe devolver un 400 Bad Request cuando el JSON sea nulo o inválido")
    @WithMockUser
    void eliminarConsulta_escenario2() throws Exception {

        // When — DELETE sin body
        var response = mockMvc.perform(MockMvcRequestBuilders.delete("/consultas"))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /*
     * ESCENARIO 3 — EntityNotFoundException → 404 NOT FOUND
     * ────────────────────────────────────────────────────────
     * Verifica que el @ExceptionHandler captura EntityNotFoundException
     * y devuelve 404 cuando la consulta a cancelar no existe.
     *
     * cancelarConsulta() devuelve void → se usa doThrow en lugar de
     * when().thenThrow() porque Java no puede pasar void como argumento.
     *
     * Sintaxis void:    doThrow(...).when(mock).metodo()
     * Sintaxis retorno: when(mock.metodo()).thenThrow(...)
     */
    @Test
    @DisplayName("Debe devolver un 404 Not Found cuando no se encuentra el objeto")
    @WithMockUser
    void eliminarConsulta_escenario3() throws Exception {

        // Given — mock lanza EntityNotFoundException en metodo void
        doThrow(new EntityNotFoundException())
                .when(reservaDeConsulta).cancelarConsulta(any());

        // When
        var response = mockMvc.perform(MockMvcRequestBuilders.delete("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosCancelamientoConsultaJson
                                .write(new DatosCancelamientoConsulta(2L, MotivoCancelacion.PACIENTE_DESISTIO))
                                .getJson()
                        ))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    /*
     * ESCENARIO 4 — BusinessException → 409 CONFLICT
     * ─────────────────────────────────────────────────
     * Verifica que el @ExceptionHandler captura BusinessException
     * y devuelve 409 cuando se viola una regla de negocio al cancelar
     * (por ejemplo, cancelar con menos de 24h de anticipación).
     *
     * Usa doThrow por la misma razón que el escenario 3 —
     * cancelarConsulta() devuelve void.
     */
    @Test
    @DisplayName("Debe de lanzar un 409 cuando se lance una excepción de reglas del negocio (Business Exception)")
    @WithMockUser
    void eliminarConsulta_escenario4() throws Exception {

        // Given — mock lanza BusinessException en metodo void
        doThrow(new BusinessException("Business Exception", HttpStatus.CONFLICT))
                .when(reservaDeConsulta).cancelarConsulta(any());

        // When
        var response = mockMvc.perform(MockMvcRequestBuilders.delete("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosCancelamientoConsultaJson
                                .write(new DatosCancelamientoConsulta(2L, MotivoCancelacion.PACIENTE_DESISTIO))
                                .getJson()
                        ))
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    /*
     * ESCENARIO 5 — Cancelación exitosa → 204 NO CONTENT
     * ─────────────────────────────────────────────────────
     * Verifica el happy path del DELETE: cuando cancelarConsulta()
     * termina sin excepciones, el controller devuelve 204 NO CONTENT.
     *
     * doNothing() es el comportamiento por defecto de Mockito en
     * métodos void, pero se deja comentado para documentar la intención.
     * El mock simplemente no hace nada → el controller llega a:
     *   return ResponseEntity.noContent().build() → HTTP 204
     */
    @Test
    @DisplayName("Debe devolver un 204 No Content al cancelar una consulta exitosamente")
    @WithMockUser
    void eliminarConsulta_escenario5() throws Exception {

        // Given — doNothing implícito, cancelarConsulta() termina sin error
        // doNothing().when(reservaDeConsulta).cancelarConsulta(any());

        // When
        var response = mockMvc.perform(MockMvcRequestBuilders.delete("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datosCancelamientoConsultaJson
                                .write(new DatosCancelamientoConsulta(2L, MotivoCancelacion.PACIENTE_DESISTIO))
                                .getJson())
                )
                .andReturn()
                .getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }
}