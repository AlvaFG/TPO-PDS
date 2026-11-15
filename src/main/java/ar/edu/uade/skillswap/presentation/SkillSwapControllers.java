package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.controllers.UiControllers;

/* Agrupa los controllers que la UI conoce. */
public class SkillSwapControllers implements UiControllers {

    private final BusquedaController busqueda;
    private final PublicacionController publicaciones;
    private final ContratacionController contrataciones;
    private final WalletController wallet;
    private final NotificacionController notificaciones;
    private final PerfilController perfiles;

    public SkillSwapControllers(BusquedaController busqueda, PublicacionController publicaciones,
                                ContratacionController contrataciones, WalletController wallet,
                                NotificacionController notificaciones, PerfilController perfiles) {
        this.busqueda = busqueda;
        this.publicaciones = publicaciones;
        this.contrataciones = contrataciones;
        this.wallet = wallet;
        this.notificaciones = notificaciones;
        this.perfiles = perfiles;
    }

    @Override
    public BusquedaController busqueda() {
        return busqueda;
    }

    @Override
    public PublicacionController publicaciones() {
        return publicaciones;
    }

    @Override
    public ContratacionController contrataciones() {
        return contrataciones;
    }

    @Override
    public WalletController wallet() {
        return wallet;
    }

    @Override
    public NotificacionController notificaciones() {
        return notificaciones;
    }

    @Override
    public PerfilController perfiles() {
        return perfiles;
    }
}
