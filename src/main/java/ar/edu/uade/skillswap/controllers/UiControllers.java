package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.*;
import java.util.List;

public interface UiControllers {
    interface BusquedaController {
        List<Publicacion> buscar(FiltrosBusqueda filtros);
        List<Publicacion> recomendar(String userId);
        List<String> categorias();
    }

    interface PublicacionController {
        List<Publicacion> listarPropias(String userId);
        Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                  Modalidad modalidad, int duracion, int precio, String politicaId);
        default Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                          String categoria, Modalidad modalidad, int duracion, int precio) {
            return crearBorrador(userId, titulo, descripcion, categoria, modalidad, duracion, precio, null);
        }
        Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado);
        /* Solo en BORRADOR. */
        Publicacion editar(String userId, String publicacionId, String titulo, String descripcion, int precio);
        /* tipo: PORCENTUAL o MONTO_FIJO. */
        void agregarPromocion(String userId, String publicacionId, String tipo, int valor, int diasVigencia);
        List<Politica> listarPoliticas();
    }

    interface ContratacionController {
        List<Contratacion> listarContrataciones(String userId);
        Contratacion solicitar(String userId, String publicacionId, String objetivo, String franjaId);
        default Contratacion solicitar(String userId, String publicacionId, String objetivo) {
            return solicitar(userId, publicacionId, objetivo, null);
        }
        List<Franja> franjasDePublicacion(String publicacionId);
        Contratacion rechazar(String userId, String contratacionId, String motivo);
        List<Franja> franjasParaReprogramar(String userId, String contratacionId);
        Contratacion proponerReprogramacion(String userId, String contratacionId, String franjaId);
        Contratacion aceptarReprogramacion(String userId, String contratacionId);
        /* Detalle del precio acordado (promociones aplicadas). */
        String comprobante(String userId, String contratacionId);
        Contratacion avanzar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String contratacionId);
        void calificar(String userId, String contratacionId, int puntaje, String comentario);
    }

    interface WalletController {
        int consultarSaldo(String userId);
        List<Movimiento> listarMovimientos(String userId);
        int acreditarDemo(String userId, int importe);
    }

    interface NotificacionController {
        List<Notificacion> listarNotificaciones(String userId);
        void marcarLeida(String userId, String notificacionId);
        void marcarTodasLeidas(String userId);
    }

    interface PerfilController {
        Perfil verPerfil(String userId);
        Perfil verPerfilDe(String perfilId);
        Perfil actualizar(String userId, String nombre, String biografia);
    }

    BusquedaController busqueda();
    PublicacionController publicaciones();
    ContratacionController contrataciones();
    WalletController wallet();
    NotificacionController notificaciones();
    PerfilController perfiles();
}
