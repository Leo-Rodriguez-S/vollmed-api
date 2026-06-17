# Voll.med API REST

API REST para gestión de una clínica médica ficticia. Permite administrar médicos, pacientes y consultas médicas con autenticación segura mediante JWT.

## 🛠️ Tecnologías

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL
- Flyway (migraciones de base de datos)
- SpringDoc OpenAPI (Swagger)
- Maven

## ✅ Funcionalidades

- Registro y autenticación de usuarios con JWT
- CRUD de médicos y pacientes
- Agendamiento y cancelación de consultas
- Validaciones de reglas de negocio
- Migraciones de base de datos con Flyway
- Documentación interactiva con Swagger UI
- Perfiles de entorno separados (dev, prod, test)

## ⚙️ Configuración

### Variables de entorno requeridas

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | URL de conexión a MySQL |
| `DB_DRIVER` | Driver de base de datos |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT |

### Ejecución local

```bash
# Clonar el repositorio
git clone https://github.com/Leo-Rodriguez-S/vollmed-api.git

# Configurar las variables de entorno
# Ejecutar con Maven
./mvnw spring-boot:run
```

## 📄 Documentación de la API

Con la aplicación corriendo, accede a la documentación interactiva en:
http://localhost:8080/swagger-ui.html

## 📁 Estructura del proyecto
src/main/java/med/voll/api/

├── controller/      # Controladores REST

├── domain/          # Entidades, DTOs, repositorios y servicios

│   ├── consulta/

│   ├── medico/

│   ├── paciente/

│   └── usuario/

└── infra/           # Seguridad, excepciones y configuración

├── security/

├── exceptions/

└── springdoc/