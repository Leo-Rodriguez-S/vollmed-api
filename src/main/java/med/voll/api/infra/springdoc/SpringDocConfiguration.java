package med.voll.api.infra.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfiguration {

    //Este metodo informa a Swagger que nuestra API utiliza autenticación y le explica cómo funciona
    @Bean
    public OpenAPI customOpenAPI() {
        //OpenAPI es la representación en Java de toda la documentación.
        //Spring lo registra como bean y SpringDoc lo usa automáticamente para generar el JSON de /v3/api-docs
        return new OpenAPI()

                .info(new Info()
                        //Titulo y nombre de la aplicación
                        .title("Voll.med API")
                        .description("API Rest de la aplicación Voll.med, que contiene las funcionalidades CRUD de médicos y de pacientes, además de reserva y cancelamiento de consultas.")

                        //Información de contacto
                        .contact(new Contact()
                                .name("Equipo de desarrollo BackEnd de Voll.med")
                                .email("backend@voll.med"))

                        //Licencias
                        .license(new License()
                                .name("Licencia Voll.med")
                                .url("http://voll.med/api/licencia")))

                //Servidores
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Desarrollo local"),
                        new Server().url("https://voll.med/api/produccion").description("Producción")
                ))

                //Components es una sección donde se guardan cosas reutilizables —
                //Esquemas de seguridad, ejemplos, parámetros, etc. —
                //Es como una bodega de definiciones que otras partes de la documentación pueden referenciar.
                .components(new Components()
                        //Registro de un esquema de seguridad con el nombre "bearerAuth".
                        //Este nombre es la llave y debe ser exactamente igual en @SecurityRequirement
                        .addSecuritySchemes("bearer-key",
                                //El tipo HTTP le dice a Swagger que la autenticación va en el header Authorization de la petición HTTP.
                                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                        //Dentro del tipo HTTP, el esquema bearer significa que el valor del header será Bearer <token>
                                        .scheme("bearer")
                                        //Esto es puramente documentativo — no afecta ningún comportamiento. Solo le dice al lector humano que el token es un JWT
                                        .bearerFormat("JWT")));
    }

}
