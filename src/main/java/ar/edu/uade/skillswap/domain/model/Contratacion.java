package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacion;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacionFactory;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoPendiente;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.precio.ICalculoPrecio;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Solicitud y contratacion de una publicacion.
 * State Pattern - Contexto: cada accion delega en el estado actual.
 * Guarda el precio acordado como foto: no cambia si despues cambian las promociones.
 * El estado se persiste como nombreEstado y se reconstruye con EstadoContratacionFactory (@PostLoad).
 */
@Entity
public class Contratacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Perfil cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Perfil proveedor;
    private String mensaje;
    private String objetivo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "franja_id")
    private FranjaHoraria franja;
    @Embedded
    @AttributeOverride(name = "cantidad", column = @Column(name = "precio_acordado", nullable = false))
    private SkillCoins precioAcordado;
    private String detallePrecio;
    @Transient
    private EstadoContratacion estado = new EstadoPendiente();
    @Column(nullable = false)
    private String nombreEstado = estado.getNombre();
    @Embedded
    private PropuestaReprogramacion propuesta;
    /* Historial inmutable: con @OrderColumn cada cambio nuevo es solo un INSERT. */
    @ElementCollection
    @CollectionTable(name = "contratacion_historial", joinColumns = @JoinColumn(name = "contratacion_id"))
    @OrderColumn(name = "orden")
    private List<CambioEstado> historial = new ArrayList<>();
    private boolean calificadaPorCliente;
    private boolean calificadaPorProveedor;
    @Version
    private long version;

    protected Contratacion() {
    }

    /* Simple Factory: reconstruye el objeto estado desde el nombre persistido. */
    @PostLoad
    private void reconstruirEstado() {
        this.estado = EstadoContratacionFactory.desdeNombre(nombreEstado);
    }

    /* Crea la solicitud en PENDIENTE con el precio cotizado en la fecha indicada. */
    public Contratacion(Publicacion publicacion, Perfil cliente, FranjaHoraria franja,
                        String mensaje, String objetivo, LocalDate fecha) {
        if (!publicacion.admiteSolicitudes()) {
            throw new ReglaNegocioException("La publicacion no admite solicitudes");
        }
        if (publicacion.esAutor(cliente.getUserId())) {
            throw new ReglaNegocioException("El autor no puede solicitar su propia publicacion");
        }
        validarFranjaDisponible(publicacion, franja);
        ICalculoPrecio precio = publicacion.cotizar(fecha);
        this.publicacion = publicacion;
        this.cliente = cliente;
        this.proveedor = publicacion.getAutor();
        this.franja = franja;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
        this.precioAcordado = precio.calcularPrecio();
        this.detallePrecio = precio.getDescripcion();
        historial.add(new CambioEstado(LocalDateTime.now(), cliente.getUserId(), null, nombreEstado, "Solicitud"));
    }

    /* ---- State: cada accion delega en el estado actual ---- */

    public void aceptar(String actorId) {
        estado.aceptar(this, actorId);
    }

    public void rechazar(String actorId, String motivo) {
        estado.rechazar(this, actorId, motivo);
    }

    public void pagar(String actorId, Billetera cliente, Billetera proveedor) {
        estado.pagar(this, actorId, cliente, proveedor);
    }

    public void proponerReprogramacion(String actorId, FranjaHoraria nuevaFranja) {
        estado.proponerReprogramacion(this, actorId, nuevaFranja);
    }

    public void aceptarReprogramacion(String actorId) {
        estado.aceptarReprogramacion(this, actorId);
    }

    public void iniciar(String actorId) {
        estado.iniciar(this, actorId);
    }

    public void completar(String actorId) {
        estado.completar(this, actorId);
    }

    /* Devuelve el monto reintegrado al cliente. */
    public SkillCoins cancelar(String actorId, String motivo, LocalDateTime ahora, Billetera cliente, Billetera proveedor) {
        return estado.cancelar(this, actorId, motivo, ahora, cliente, proveedor);
    }

    /* Solo participantes de una contratacion COMPLETADA, una vez cada uno y nunca a si mismos. */
    public Calificacion calificar(String autorId, int puntaje, String comentario) {
        if (!estado.permiteCalificar()) {
            throw new ReglaNegocioException("Solo se puede calificar una contratacion COMPLETADA");
        }
        Perfil calificado = contraparteDe(autorId);
        Perfil autor = esCliente(autorId) ? cliente : proveedor;
        boolean yaCalifico = esCliente(autorId) ? calificadaPorCliente : calificadaPorProveedor;
        if (yaCalifico) {
            throw new ReglaNegocioException("El participante ya califico esta contratacion");
        }
        Calificacion calificacion = new Calificacion(this, autor, calificado, puntaje, comentario, LocalDateTime.now());
        if (esCliente(autorId)) {
            calificadaPorCliente = true;
        } else {
            calificadaPorProveedor = true;
        }
        return calificacion;
    }

    /* Unico punto que cambia el estado: deja la entrada en el historial inmutable. */
    public void cambiarEstado(EstadoContratacion nuevo, String actorId, String motivo) {
        historial.add(new CambioEstado(LocalDateTime.now(), actorId, estado.getNombre(), nuevo.getNombre(), motivo));
        this.estado = nuevo;
        this.nombreEstado = nuevo.getNombre();
    }

    public boolean esParticipante(String userId) {
        return esCliente(userId) || esProveedor(userId);
    }

    public Perfil contraparteDe(String userId) {
        if (esCliente(userId)) {
            return proveedor;
        }
        if (esProveedor(userId)) {
            return cliente;
        }
        throw new ReglaNegocioException("El usuario no participa de la contratacion");
    }

    public boolean esCliente(String userId) {
        return cliente.getUserId().equals(userId);
    }

    public boolean esProveedor(String userId) {
        return proveedor.getUserId().equals(userId);
    }

    /* Lo usan los estados que admiten reprogramar (ACEPTADA y CONFIRMADA). */
    public void registrarPropuesta(String actorId, FranjaHoraria nuevaFranja) {
        validarFranjaDisponible(publicacion, nuevaFranja);
        this.propuesta = new PropuestaReprogramacion(nuevaFranja, actorId, LocalDateTime.now());
    }

    /* Mueve la reserva a la franja propuesta: libera la anterior y ocupa la nueva. */
    public void aplicarPropuesta(String actorId) {
        if (propuesta == null) {
            throw new ReglaNegocioException("No hay una reprogramacion pendiente");
        }
        if (!propuesta.puedeAceptarla(actorId)) {
            throw new ReglaNegocioException("La reprogramacion la acepta la contraparte de quien la propuso");
        }
        FranjaHoraria nueva = propuesta.getNuevaFranja();
        nueva.ocupar();
        franja.liberar();
        franja = nueva;
        propuesta = null;
    }

    private static void validarFranjaDisponible(Publicacion publicacion, FranjaHoraria franja) {
        if (franja == null || !publicacion.getFranjas().contains(franja)) {
            throw new ReglaNegocioException("La franja no pertenece a la publicacion");
        }
        if (!franja.estaLibre()) {
            throw new ReglaNegocioException("La franja elegida esta ocupada");
        }
    }

    public Long getId() {
        return id;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Perfil getCliente() {
        return cliente;
    }

    public Perfil getProveedor() {
        return proveedor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public FranjaHoraria getFranja() {
        return franja;
    }

    public SkillCoins getPrecioAcordado() {
        return precioAcordado;
    }

    public String getDetallePrecio() {
        return detallePrecio;
    }

    public EstadoContratacion getEstado() {
        return estado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public PropuestaReprogramacion getPropuesta() {
        return propuesta;
    }

    public List<CambioEstado> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public boolean isCalificadaPorCliente() {
        return calificadaPorCliente;
    }

    public boolean isCalificadaPorProveedor() {
        return calificadaPorProveedor;
    }

    public long getVersion() {
        return version;
    }
}
