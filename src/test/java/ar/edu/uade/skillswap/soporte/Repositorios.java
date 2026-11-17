package ar.edu.uade.skillswap.soporte;

import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPoliticaCancelacionRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.ElementoCatalogo;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPausada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicada;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Calificacion;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Puertos de salida simulados en memoria para las pruebas de casos de uso.
 */
public final class Repositorios {

    private Repositorios() {
    }

    /* Ejecuta la operacion y cuenta cuantas transacciones se abrieron. */
    public static class GestorTransacciones implements IGestorTransacciones {
        public int transacciones;

        @Override
        public <T> T ejecutar(Supplier<T> operacion) {
            transacciones++;
            return operacion.get();
        }
    }

    public static class Perfiles implements IPerfilRepository {
        private final EnMemoria<Perfil> datos = new EnMemoria<>();

        @Override
        public Perfil guardar(Perfil perfil) {
            return datos.guardar(perfil);
        }

        @Override
        public Optional<Perfil> buscarPorId(Long id) {
            return datos.buscar(id);
        }

        @Override
        public Optional<Perfil> buscarPorUserId(String userId) {
            return datos.todos().stream().filter(p -> p.getUserId().equals(userId)).findFirst();
        }
    }

    public static class Publicaciones implements IPublicacionRepository {
        private final EnMemoria<Publicacion> datos = new EnMemoria<>();
        private final EnMemoria<FranjaHoraria> franjas = new EnMemoria<>();

        @Override
        public Publicacion guardar(Publicacion publicacion) {
            publicacion.getFranjas().forEach(franjas::guardar);
            return datos.guardar(publicacion);
        }

        @Override
        public Optional<Publicacion> buscarPorId(Long id) {
            return datos.buscar(id);
        }

        @Override
        public Pagina<Publicacion> buscar(FiltroBusqueda f, List<Long> categoriaIds) {
            Stream<Publicacion> resultado = listarPublicadas().stream()
                    .filter(p -> f.texto() == null || p.getTitulo().toLowerCase().contains(f.texto().toLowerCase()))
                    .filter(p -> categoriaIds.isEmpty() || categoriaIds.contains(p.getCategoria().getId()))
                    .filter(p -> f.modalidad() == null || p.getModalidad() == f.modalidad())
                    .filter(p -> f.precioMinimo() == null || p.getPrecioBase().getCantidad() >= f.precioMinimo())
                    .filter(p -> f.precioMaximo() == null || p.getPrecioBase().getCantidad() <= f.precioMaximo())
                    .filter(p -> f.habilidadId() == null
                            || p.getHabilidades().stream().map(ElementoCatalogo::getId).anyMatch(f.habilidadId()::equals))
                    .filter(p -> f.reputacionMinima() == null || p.getAutor().getReputacion() >= f.reputacionMinima());
            if (f.orden() != null) {
                resultado = resultado.sorted(switch (f.orden()) {
                    case MENOR_PRECIO -> Comparator.comparingLong((Publicacion p) -> p.getPrecioBase().getCantidad());
                    case MEJOR_REPUTACION -> Comparator.comparingDouble((Publicacion p) -> p.getAutor().getReputacion()).reversed();
                    case MAS_RECIENTE -> Comparator.comparing(Publicacion::getFechaPublicacion).reversed();
                    case RELEVANCIA -> Comparator.comparingLong(Publicacion::getId);
                });
            }
            return Pagina.de(resultado.toList(), f.pagina(), f.tamanio());
        }

        @Override
        public List<Publicacion> listarActivasDe(Long autorId) {
            return datos.todos().stream()
                    .filter(p -> Objects.equals(p.getAutor().getId(), autorId))
                    .filter(p -> p.getNombreEstado().equals(EstadoPublicada.NOMBRE)
                            || p.getNombreEstado().equals(EstadoPausada.NOMBRE))
                    .toList();
        }

        @Override
        public List<Publicacion> listarDeAutor(Long autorId) {
            return datos.todos().stream().filter(p -> Objects.equals(p.getAutor().getId(), autorId)).toList();
        }

        @Override
        public List<Publicacion> listarPublicadas() {
            return datos.todos().stream().filter(Publicacion::admiteSolicitudes).toList();
        }
    }

    public static class Contrataciones implements IContratacionRepository {
        private final EnMemoria<Contratacion> datos = new EnMemoria<>();

        @Override
        public Contratacion guardar(Contratacion contratacion) {
            return datos.guardar(contratacion);
        }

        @Override
        public Optional<Contratacion> buscarPorId(Long id) {
            return datos.buscar(id);
        }

        @Override
        public Pagina<Contratacion> listarPorParticipante(String userId, int pagina, int tamanio) {
            return Pagina.de(datos.todos().stream().filter(c -> c.esParticipante(userId)).toList(), pagina, tamanio);
        }

        /* La fecha de la solicitud es la del primer cambio de estado del historial. */
        @Override
        public Map<Long, Long> contarRecientesPorPublicacion(LocalDateTime desde) {
            return datos.todos().stream()
                    .filter(c -> !c.getHistorial().get(0).getFecha().isBefore(desde))
                    .collect(Collectors.groupingBy(c -> c.getPublicacion().getId(), Collectors.counting()));
        }
    }

    public static class Billeteras implements IBilleteraRepository {
        private final EnMemoria<Billetera> datos = new EnMemoria<>();

        @Override
        public Billetera guardar(Billetera billetera) {
            return datos.guardar(billetera);
        }

        @Override
        public Optional<Billetera> buscarPorUserId(String userId) {
            return datos.todos().stream().filter(b -> b.getTitular().getUserId().equals(userId)).findFirst();
        }
    }

    public static class Calificaciones implements ICalificacionRepository {
        private final EnMemoria<Calificacion> datos = new EnMemoria<>();

        @Override
        public Calificacion guardar(Calificacion calificacion) {
            return datos.guardar(calificacion);
        }

        @Override
        public List<Calificacion> listarRecibidas(Long perfilId) {
            return datos.todos().stream().filter(c -> Objects.equals(c.getCalificado().getId(), perfilId)).toList();
        }

        public List<Calificacion> todas() {
            return datos.todos();
        }
    }

    public static class Notificaciones implements INotificacionRepository {
        private final EnMemoria<Notificacion> datos = new EnMemoria<>();

        @Override
        public Notificacion guardar(Notificacion notificacion) {
            return datos.guardar(notificacion);
        }

        @Override
        public Optional<Notificacion> buscarPorId(Long id) {
            return datos.buscar(id);
        }

        @Override
        public Pagina<Notificacion> listarPorDestinatario(String userId, int pagina, int tamanio) {
            return Pagina.de(datos.todos().stream().filter(n -> n.getDestinatarioId().equals(userId)).toList(),
                    pagina, tamanio);
        }
    }

    public static class Catalogo implements ICatalogoRepository {
        private final EnMemoria<ElementoCatalogo> datos = new EnMemoria<>();

        public <T extends ElementoCatalogo> T guardar(T elemento) {
            datos.guardar(elemento);
            return elemento;
        }

        @Override
        public Optional<Categoria> buscarCategoria(Long id) {
            return datos.buscar(id).filter(Categoria.class::isInstance).map(Categoria.class::cast);
        }

        @Override
        public List<Habilidad> buscarHabilidades(List<Long> ids) {
            return ids.stream().map(datos::buscar).flatMap(Optional::stream)
                    .filter(Habilidad.class::isInstance).map(Habilidad.class::cast).toList();
        }

        @Override
        public List<Categoria> listarCategorias() {
            return datos.todos().stream().filter(Categoria.class::isInstance).map(Categoria.class::cast).toList();
        }
    }

    public static class Politicas implements IPoliticaCancelacionRepository {
        private final EnMemoria<PoliticaCancelacion> datos = new EnMemoria<>();

        public PoliticaCancelacion guardar(PoliticaCancelacion politica) {
            return datos.guardar(politica);
        }

        @Override
        public Optional<PoliticaCancelacion> buscarPorId(Long id) {
            return datos.buscar(id);
        }

        @Override
        public List<PoliticaCancelacion> listar() {
            return datos.todos();
        }
    }
}
