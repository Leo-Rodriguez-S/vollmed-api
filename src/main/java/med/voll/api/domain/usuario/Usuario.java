package med.voll.api.domain.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario implements UserDetails {
/*
UserDetails = Interfaz de Spring Security

Spring Security necesita saber:
- ¿Quién es el usuario? (username)
- ¿Cuál es su contraseña? (password)
- ¿Qué permisos tiene? (authorities)
- ¿Está activo? (enabled)
- ¿Está bloqueado? (accountNonLocked)
- ¿Está expirado? (isAccountNonExpired & isCredentialNonExpired)

UserDetails proporciona CONTRATO estándar:
public interface UserDetails {
    Collection<? extends GrantedAuthority> getAuthorities();
    String getPassword();
    String getUsername();
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}

El usuario implementa UserDetails:
└─ Spring Security sabe cómo autenticar
*/

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String login;
    private String contrasena;
    private String rol;

    //Constructor para registro
    public Usuario(DatosRegistroUsuario datos, String contrasenaHasheada) {
        this.login = datos.login();
        this.contrasena = contrasenaHasheada;
        this.rol = "ROLE_USER";
    }

    public Usuario(String login, String contrasena, String rol) {
        this.login = login;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    //Metodo que retorna roles/permisos del usuario
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    /*
     getPassword(): Retorna contraseña hasheada

     Spring Security la usa para:
     Login:
   - Usuario envía "123456" (texto plano)
   - getPassword() retorna "$2a$10$..." (hash BCrypt)
   - BCrypt.matches("123456", "$2a$10$...") → true/false

     Importante: NUNCA retorna contraseña en texto plano
   - Siempre hash BCrypt
   - Irreversible
    */
    @Override
    public String getPassword() {
        return contrasena;
    }

    /*
    getUsername(): Retorna identificador único del usuario

    En tu caso: email (login)

    Spring Security usa el username para:
    1. Autenticación:
       loadUserByUsername("jessica@voll.med") --> Metodo de UserDetailsService en AuthenticationService
       ↓
       Busca usuario por username

    2. Token JWT:
       token.getSubject() → username

    Confusión común:
    - getUsername() NO siempre es un "nombre de usuario"
    - Puede ser email, DNI, código, etc.
    - Es el IDENTIFICADOR ÚNICO para login

    En este proyecto:
    username = email (login)
    */
    @Override
    public String getUsername() {
        return login;
    }

    /*
    isAccountNonExpired(): ¿Cuenta expirada?

    true = NO expirada (activa)
    false = SÍ expirada (bloqueada)

    Casos de uso:
    - Cuentas de prueba con expiración
    - Suscripciones temporales
    - Acceso limitado por tiempo

    En este proyecto:
    return true → NUNCA expiran
    ↓
    Por implementar:
    private LocalDate expirationDate;

    @Override
    public boolean isAccountNonExpired() {
        return expirationDate == null ||
               LocalDate.now().isBefore(expirationDate);
    }
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /*
    isAccountNonLocked(): ¿Cuenta bloqueada?

    true = NO bloqueada (puede acceder)
    false = SÍ bloqueada (no puede acceder)

    Casos de uso:
    - Bloquear después de X intentos fallidos
    - Bloqueo manual por admin
    - Suspensión temporal

    En este proyecto:
    return true → NUNCA bloqueadas
    ↓
    Para implementar:
    private Boolean locked;
    private Integer loginAttempts;

    @Override
    public boolean isAccountNonLocked() {
        return !locked && loginAttempts < 5;
    }
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /*
    isCredentialsNonExpired(): ¿Contraseña expirada?

    true = NO expirada (válida)
    false = SÍ expirada (debe cambiarla)

    Casos de uso:
    - Forzar cambio de contraseña cada X meses
    - Contraseña temporal de primer acceso
    - Políticas de seguridad corporativas

    En este proyecto:
    return true → Contraseñas NUNCA expiran
    ↓
    Para implementar:
    private LocalDate passwordChangedDate;

    @Override
    public boolean isCredentialsNonExpired() {
        if (passwordChangedDate == null) return false;

        LocalDate expirationDate = passwordChangedDate.plusMonths(3);
        return LocalDate.now().isBefore(expirationDate);
    }
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /*
    isEnabled(): ¿Usuario habilitado?

    true = Habilitado (puede acceder)
    false = Deshabilitado (no puede acceder)

    Diferencia vs isAccountNonLocked:
    - isEnabled: Estado general (activo/inactivo)
    - isAccountNonLocked: Bloqueo específico (temporal)

    Casos de uso:
    - Usuarios pendientes de verificación email
    - Desactivación por admin
    - Soft delete de usuarios

    En este proyecto:
    return true → SIEMPRE habilitados
    ↓
    Para implementar:
    private Boolean activo;

    @Override
    public boolean isEnabled() {
        return activo;
    }
    */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
