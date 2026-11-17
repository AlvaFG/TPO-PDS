package ar.edu.uade.skillswap;

import ar.edu.uade.skillswap.application.evento.NotificacionObserver;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.evento.ReputacionObserver;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.application.port.ICanalNotificacionFactory;
import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPoliticaCancelacionRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.application.port.IServicioIdentidad;
import ar.edu.uade.skillswap.application.recomendacion.IEstrategiaRecomendacion;
import ar.edu.uade.skillswap.application.recomendacion.PopularidadReciente;
import ar.edu.uade.skillswap.application.service.BilleteraService;
import ar.edu.uade.skillswap.application.service.BusquedaService;
import ar.edu.uade.skillswap.application.service.CalificacionService;
import ar.edu.uade.skillswap.application.service.ContratacionService;
import ar.edu.uade.skillswap.application.service.NotificacionService;
import ar.edu.uade.skillswap.application.service.PerfilService;
import ar.edu.uade.skillswap.application.service.PublicacionService;
import ar.edu.uade.skillswap.application.service.RecomendacionService;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.presentation.BusquedaControllerImpl;
import ar.edu.uade.skillswap.presentation.ContratacionControllerImpl;
import ar.edu.uade.skillswap.presentation.EjecutorDeAcciones;
import ar.edu.uade.skillswap.presentation.NotificacionControllerImpl;
import ar.edu.uade.skillswap.presentation.PerfilControllerImpl;
import ar.edu.uade.skillswap.presentation.PublicacionControllerImpl;
import ar.edu.uade.skillswap.presentation.SkillSwapControllers;
import ar.edu.uade.skillswap.presentation.WalletControllerImpl;

import java.time.Clock;

/*
 * Composition Root (sin Spring): unico lugar que conoce las clases concretas de servicios, observers
 * y controllers. Recibe los puertos de salida ya construidos (JPA en produccion, memoria en pruebas).
 */
public final class ConfiguracionApp {

    private ConfiguracionApp() {
    }

    /* Puertos de salida que la aplicacion necesita. */
    public record Puertos(IPerfilRepository perfiles, IPublicacionRepository publicaciones,
                          IContratacionRepository contrataciones, IBilleteraRepository billeteras,
                          INotificacionRepository notificaciones, ICalificacionRepository calificaciones,
                          ICatalogoRepository catalogo, IPoliticaCancelacionRepository politicas,
                          IGestorTransacciones transacciones, IServicioIdentidad identidad,
                          ICanalNotificacionFactory canales, Clock reloj) {
    }

    /* Estrategia de recomendacion por defecto: popularidad reciente. */
    public static UiControllers crear(Puertos p) {
        return crear(p, new PopularidadReciente(p.contrataciones(), p.reloj()));
    }

    public static UiControllers crear(Puertos p, IEstrategiaRecomendacion estrategia) {
        PublicadorEventos eventos = new PublicadorEventos();
        eventos.suscribir(new NotificacionObserver(p.perfiles(), p.canales()));
        eventos.suscribir(new ReputacionObserver(p.perfiles(), p.calificaciones()));

        var publicacion = new PublicacionService(p.publicaciones(), p.perfiles(), p.catalogo(), p.politicas(),
                p.transacciones(), p.reloj());
        var contratacion = new ContratacionService(p.contrataciones(), p.publicaciones(), p.perfiles(),
                p.billeteras(), p.transacciones(), eventos, p.reloj());
        var busqueda = new BusquedaService(p.publicaciones(), p.catalogo());
        var recomendacion = new RecomendacionService(estrategia, p.perfiles(), p.publicaciones());
        var billetera = new BilleteraService(p.billeteras());
        var notificacion = new NotificacionService(p.notificaciones());
        var perfil = new PerfilService(p.perfiles(), p.publicaciones(), p.contrataciones(), p.catalogo(),
                p.billeteras(), p.transacciones());

        var ejecutor = new EjecutorDeAcciones(p.identidad());
        return new SkillSwapControllers(
                new BusquedaControllerImpl(busqueda, recomendacion, ejecutor),
                new PublicacionControllerImpl(publicacion, busqueda, ejecutor, p.reloj()),
                new ContratacionControllerImpl(contratacion,
                        new CalificacionService(p.contrataciones(), p.calificaciones(), p.transacciones(), eventos),
                        publicacion, ejecutor),
                new WalletControllerImpl(billetera, ejecutor),
                new NotificacionControllerImpl(notificacion, ejecutor),
                new PerfilControllerImpl(perfil, ejecutor));
    }
}
