package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.time.LocalDateTime;

/*
 * Transicion pedida sobre una publicacion. Cada literal llama al metodo de dominio
 * que le corresponde, asi el caso de uso no necesita un switch.
 */
public enum AccionPublicacion {

    PUBLICAR {
        @Override
        public void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora) {
            publicacion.publicar(userId, ahora);
        }
    },
    PAUSAR {
        @Override
        public void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora) {
            publicacion.pausar(userId);
        }
    },
    REANUDAR {
        @Override
        public void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora) {
            publicacion.reanudar(userId);
        }
    },
    CERRAR {
        @Override
        public void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora) {
            publicacion.cerrar(userId);
        }
    },
    CANCELAR {
        @Override
        public void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora) {
            publicacion.cancelar(userId, motivo);
        }
    };

    public abstract void ejecutar(Publicacion publicacion, String userId, String motivo, LocalDateTime ahora);
}
