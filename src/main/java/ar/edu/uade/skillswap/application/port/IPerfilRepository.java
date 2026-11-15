package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.model.Perfil;

import java.util.Optional;

/* Puerto de salida de persistencia (Protected Variations: cambiar JPA no toca los casos de uso). */
public interface IPerfilRepository {

    Perfil guardar(Perfil perfil);

    Optional<Perfil> buscarPorId(Long id);

    Optional<Perfil> buscarPorUserId(String userId);
}
