package med.voll.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}

/*
@SpringBootApplication es una MEGA-ANOTACIÓN que combina 3:

1. @SpringBootConfiguration
   └─ Indica que esta es una clase de configuración Spring
      Equivalente a @Configuration

2. @EnableAutoConfiguration
   └─ Configura automáticamente basándose en:
      - Dependencies en classpath
      - Properties en application.properties

3. @ComponentScan
   └─ Escanea paquete actual y sub-paquetes
      Busca: @Component, @Service, @Repository, @Controller
      Los registra como beans en ApplicationContext

En este proyecto:
package med.voll.api;

@SpringBootApplication escanea:
├─ med.voll.api
├─ med.voll.api.controller
├─ med.voll.api.domain
├─ med.voll.api.infra
└─ Todos los sub-paquetes

Flujo Completo de Inicio:
┌────────────────────────────────────────────────────────┐
│  PASO 1: main() se ejecuta                             │
│  public static void main(String[] args) {              │
│      SpringApplication.run(ApiApplication.class, args);│
│  }                                                     │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 2: SpringApplication se inicializa               │
│  - Lee @SpringBootApplication                          │
│  - Determina tipo de aplicación: WEB                   │
│  - Carga configuración inicial                         │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 3: ApplicationContext se crea                    │
│  ApplicationContext = Contenedor de Beans              │
│  - Gestiona ciclo de vida de objetos                   │
│  - Inyección de dependencias                           │
│  - Configuración centralizada                          │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 4: @EnableAutoConfiguration actúa                │
│                                                        │
│  Detecta en classpath:                                 │
│  spring-boot-starter-data-jpa                          │
│     → Auto-configura Hibernate                         │
│     → Auto-configura DataSource                        │
│     → Auto-configura TransactionManager                │
│                                                        │
│  spring-boot-starter-web                               │
│     → Auto-configura Tomcat embebido                   │
│     → Auto-configura DispatcherServlet                 │
│     → Auto-configura Jackson (JSON)                    │
│                                                        │
│  spring-boot-starter-security                          │
│     → Auto-configura SecurityFilterChain               │
│     → Auto-configura AuthenticationManager             │
│                                                        │
│  flyway-core                                           │
│     → Auto-configura Flyway                            │
│     → Ejecuta migrations                               │
│                                                        │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 5: application.properties se lee                 │
│  spring.datasource.url=jdbc:mysql://localhost/...      │
│  spring.datasource.username=root                       │
│  spring.datasource.password=@LR1505msMYSQL@            │
│  → Configuración inyectada en beans                    │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 6: DataSource se crea                            │
│  HikariCP (Connection Pool):                           │
│  - Conecta a MySQL                                     │
│  - Crea pool de 10 conexiones                          │
│  - Valida conexión                                     │
│                                                        │
│  Logs:                                                 │
│  HikariPool-1 - Starting...                            │
│  HikariPool-1 - Start completed.                       │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 7: Flyway ejecuta                                │
│  1. Conecta a vollmed_api                              │
│  2. Verifica flyway_schema_history                     │
│  3. Compara versions aplicadas vs disponibles          │
│  4. Ejecuta migrations pendientes                      │
│                                                        │
│  Logs:                                                 │
│  Flyway Community Edition 9.x.x                        │
│  Database: jdbc:mysql://localhost/vollmed_api          │
│  Successfully validated 7 migrations                   │
│  Current version of schema `vollmed_api`: 7            │
│  Schema `vollmed_api` is up to date. No migration...   │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 8: @ComponentScan escanea paquetes              │
│  Encuentra y registra beans:                           │
│                                                        │
│  @RestController:                                      │
│  ├─ AuthenticationController                           │
│  ├─ MedicoController                                   │
│  ├─ PacienteController                                 │
│  └─ UsuarioController                                  │
│                                                        │
│  @Service:                                             │
│  ├─ TokenService                                       │
│  └─ AuthenticationService                              │
│                                                        │
│  @Repository:                                          │
│  ├─ MedicoRepository (proxy JPA)                       │
│  ├─ PacienteRepository (proxy JPA)                     │
│  └─ UsuarioRepository (proxy JPA)                      │
│                                                        │
│  @Component:                                           │
│  ├─ SecurityFilter                                     │
│  ├─ MedicoDTOModelAssembler                            │
│  └─ PacienteDTOModelAssembler                          │
│                                                        │
│  @Configuration:                                       │
│  └─ SecurityConfigurations                             │
│                                                        │
│  @RestControllerAdvice:                                │
│  └─ GestorDeErrores                                    │
│                                                        │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 9: Hibernate se inicializa                       │
│  - Escanea @Entity (Medico, Paciente, Usuario)         │
│  - Mapea anotaciones JPA a schema BD                   │
│  - Valida que schema coincide con entidades            │
│  - Crea SessionFactory                                 │
│                                                        │
│  Logs:                                                 │
│  Hibernate: 5.6.x                                      │
│  HHH000204: Processing PersistenceUnitInfo...          │
│  HHH000412: Hibernate ORM core version...              │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 10: Security Filters se configuran               │
│  SecurityConfigurations.securityFilterChain():         │
│  - CSRF disabled                                       │
│  - SessionCreationPolicy.STATELESS                     │
│  - authorizeHttpRequests configurado                   │
│  - SecurityFilter agregado BEFORE                      │
│    UsernamePasswordAuthenticationFilter                │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 11: Tomcat embebido inicia                       │
│  - Configura puerto 8080 (default)                     │
│  - Registra DispatcherServlet                          │
│  - Registra Security Filters                           │
│  - Mapea @RequestMapping a URLs                        │
│                                                        │
│  Logs:                                                 │
│  Tomcat initialized with port 8080 (http)              │
│  Starting service [Tomcat]                             │
│  Starting Servlet engine: [Apache Tomcat/10.1.x]       │
└────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────┐
│  PASO 12: Aplicación LISTA                             │
│                                                        │
│  Logs:                                                 │
│  Started ApiApplication in 3.456 seconds (JVM...)      │
│                                                        │
│  Estado:                                               │
│  ✅ Tomcat escuchando en http://localhost:8080         │
│  ✅ Base de datos conectada                            │
│  ✅ Migrations aplicadas                               │
│  ✅ Beans registrados                                  │
│  ✅ Security configurado                               │
│  ✅ Listo para recibir requests                        │
│                                                        │
└────────────────────────────────────────────────────────┘
*/
