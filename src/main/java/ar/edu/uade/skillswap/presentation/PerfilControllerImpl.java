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
}
