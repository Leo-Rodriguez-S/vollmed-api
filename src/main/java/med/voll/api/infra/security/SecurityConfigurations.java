package med.voll.api.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import med.voll.api.infra.exceptions.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.List;

/*
@Configuration:
- Marca clase como fuente de beans
- Spring detecta métodos @Bean
*/
@Slf4j
@Configuration

/*
@EnableWebSecurity:
- Activa Spring Security
- Importa configuración default
- Habilita @Bean SecurityFilterChain
- Registra filtros en cadena de Servlet
*/
@EnableWebSecurity
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        //Desabilitamos la protección CSRF porque nuestra API REST es stateless,
        //funciona con tokens JWT en header Authorization y no por sesión / cookies (donde protección CSRF tiene sentido)
        return http
                //Activa soporte CORS en Spring Security usando la configuración
                //CORS definida en la aplicación.
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                //Nuestra política de sesión es stateless:
                //No creamos HttpSession -> el servidor no guarda estado de la sesión,
                //Cada request es independiente
                //Se autentica con JWT
                //Escalable horizontalmente -> multiples servidores
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //Autorización de requests:
                .authorizeHttpRequests(req -> {
                    //Cualquiera puede acceder sin necesidad de autenticarse
                    req.requestMatchers("/login", "/usuarios").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/**").permitAll();

                    //Requiere autenticación y autorización con usuario ADMIN. Usuario sin ADMIN -> 403 FORBIDDEN
                    //hasRole("") agrega prefijo ROLE_, sólo se coloca el nombre puntual del rol -> hasRole("ADMIN")
                    req.requestMatchers(HttpMethod.POST, "/medicos", "/pacientes").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.GET, "/medicos", "/pacientes", "/medicos/{id}", "/pacientes/{id}").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.DELETE, "/medicos/{id}", "/pacientes/{id}").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PUT, "/medicos/{id}", "/pacientes/{id}").hasRole("ADMIN");

                    //Cualquier request no matcheado antes requiere autenticación (cualquier rol)
                    req.anyRequest().authenticated();

                    /*
                    Orden IMPORTA:
                    Reglas evaluadas de arriba hacia abajo, por ende la primera que coincide gana
                    Siempre colocar específicas arriba y generales abajo
                    */
                })

                //Manejo de excepciones
                .exceptionHandling(ex -> ex
                        /*Filtro para manejar NO autenticado -> tokens inválidos / expirados -> 401 UNAUTHORIZED
                        - Lanza AuthenticationException
                        - Casos:
                          ├─ Sin token (header Authorization vacío)
                          ├─ Token inválido (formato incorrecto)
                          ├─ Token expirado
                          └─ Token con firma incorrecta
                        */
                        .authenticationEntryPoint((request, response, authException) -> {

                            var error = new ErrorResponse(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                    "Token inválido o expirado",
                                    LocalDateTime.now(),
                                    List.of()
                            );

                            //Log del error para el logcat
                            log.error(error.message());

                            //Se trata la excepción con un HTTP 401 UNAUTHORIZED
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            //Se pasa el error a formato json mediante el objectMapper.writeValueAsString(),
                            //En esta capa Spring Web no puede serializar / deserializar automáticamente
                            var json = objectMapper.writeValueAsString(error);
                            response.getWriter().write(json);

                        })

                        //Filtro para manejar no autorizados -> 403 FORBIDDEN
                        //Usuario ya logueado
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            var error = new ErrorResponse(
                                    HttpStatus.FORBIDDEN.value(),
                                    HttpStatus.FORBIDDEN.getReasonPhrase(),
                                    "Sin permisos para realizar esta solicitud.",
                                    LocalDateTime.now(),
                                    List.of()
                            );

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            var json = objectMapper.writeValueAsString(error);
                            response.getWriter().write(json);
                        })
                )

                /*
                ¿Por qué ANTES?
                SecurityFilter valida JWT y setea autenticación
                → Filtros posteriores ven usuario autenticado
                → FilterSecurityInterceptor verifica autorización
                */
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /*
    AuthenticationManager:
    - Componente core de Spring Security
    - Autentica usuarios
    - Metodo principal: authenticate(Authentication)

    ¡Usado en AuthenticationController!
    */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //Metodo con algoritmo de hashing para guardar y comparar contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}