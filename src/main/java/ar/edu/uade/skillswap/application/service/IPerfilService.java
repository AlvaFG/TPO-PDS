package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.PerfilCommand;
import ar.edu.uade.skillswap.application.dto.PerfilPublicoDto;

/* Puerto de entrada: casos de uso de Perfil. */
public interface IPerfilService {

    PerfilPublicoDto crearPerfil(String userId, PerfilCommand cmd);

    void actualizarPerfil(String userId, PerfilCommand cmd);

    PerfilPublicoDto verPerfil(Long perfilId);

    /* Perfil del propio usuario (la UI conoce el userId, no el id del perfil). */
    PerfilPublicoDto verPerfilDeUsuario(String userId);

    void darDeBaja(String userId);
}
