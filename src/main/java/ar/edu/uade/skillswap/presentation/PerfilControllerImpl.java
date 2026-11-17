package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.service.IPerfilService;
import ar.edu.uade.skillswap.controllers.UiControllers.PerfilController;
import ar.edu.uade.skillswap.model.Models.Perfil;

public class PerfilControllerImpl implements PerfilController {

    private final IPerfilService perfiles;
    private final EjecutorDeAcciones ejecutor;

    public PerfilControllerImpl(IPerfilService perfiles, EjecutorDeAcciones ejecutor) {
        this.perfiles = perfiles;
        this.ejecutor = ejecutor;
    }

    @Override
    public Perfil verPerfil(String userId) {
        return ejecutor.ejecutar(userId, () -> Conversor.perfil(userId, perfiles.verPerfilDeUsuario(userId)));
    }

    @Override
    public Perfil verPerfilDe(String perfilId) {
        return Conversor.perfil(perfilId, perfiles.verPerfil(Conversor.id(perfilId)));
    }

    @Override
    public Perfil actualizar(String userId, String nombre, String biografia) {
        return ejecutor.ejecutar(userId, () -> {
            perfiles.actualizarBasico(userId, nombre, biografia);
            return Conversor.perfil(userId, perfiles.verPerfilDeUsuario(userId));
        });
    }
}
