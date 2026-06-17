package med.voll.api.domain.medico;

import com.fasterxml.jackson.annotation.JsonCreator;
import med.voll.api.infra.exceptions.ENUMInvalidoException;

import java.util.Arrays;
import java.util.stream.Collectors;

//ENUM de especialidades válidas
public enum Especialidad {
    ORTOPEDIA,
    CARDIOLOGIA,
    GINECOLOGIA,
    DERMATOLOGIA;

    //JsonCreator indica a Jackson cómo deserializar ENUM con metodo 'fromString'
    @JsonCreator
    public static Especialidad fromString(String text){
        for(Especialidad especialidad : Especialidad.values()){
            if (especialidad.name().equalsIgnoreCase(text)){
                return especialidad;
            }
        }

        //Se lanza excepción personalizada si no se encontró alguna especialidad válida y se indica cuáles son los valores permitidos.
        throw new ENUMInvalidoException("Especialidad inválida: '" + text +
                "'. Intente nuevamente con una especialidad válida: " + especialidadesValidas()
        );


    }

    //Metodo que une las especialidades válidas
    private static String especialidadesValidas() {
        return Arrays.stream(Especialidad.values())
                .map(Especialidad::name)
                .collect(Collectors.
                        joining(", ")
                );
    }
}
