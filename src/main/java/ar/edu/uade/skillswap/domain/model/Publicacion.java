package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoBorrador;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicacion;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicacionFactory;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;
import ar.edu.uade.skillswap.domain.precio.ICalculoPrecio;
import ar.edu.uade.skillswap.domain.precio.Promocion;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Oferta de mentoria o servicio, o desafio de una organizacion.
 * State Pattern - Contexto: delega cada transicion en su estado actual.
 * Decorator Pattern - Componente concreto: su precio base es el punto de partida de las promociones.
 * El estado se persiste como nombreEstado y se reconstruye con EstadoPublicacionFactory (@PostLoad).
 * @Version evita la sobreventa de cupos: dos aceptaciones concurrentes no pueden confirmar las dos.
 */
@Entity
public class Publicacion implements ICalculoPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Perfil autor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPublicacion tipo;
    private String titulo;
    private String descripcion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    @ManyToMany
    @JoinTable(name = "publicacion_habilidad", joinColumns = @JoinColumn(name = "publicacion_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> habilidades = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    private int duracionMinutos;
    private int cupo;
    private int cuposOcupados;
    @Embedded
    @AttributeOverride(name = "cantidad", column = @Column(name = "precio_base"))
    private SkillCoins precioBase;
    @ElementCollection
    @CollectionTable(name = "publicacion_etiqueta", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta", nullable = false)
    private Set<String> etiquetas = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "publicacion_id", nullable = false)
    @OrderBy("id")
    private List<FranjaHoraria> franjas = new ArrayList<>();
    @Transient
    private EstadoPublicacion estado = new EstadoBorrador();
    @Column(nullable = false)
    private String nombreEstado = estado.getNombre();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "politica_cancelacion_id")
    private PoliticaCancelacion politicaCancelacion;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "publicacion_id", nullable = false)
    @OrderBy("id")
    private List<Promocion> promociones = new ArrayList<>();
    /* Historial inmutable: con @OrderColumn cada cambio nuevo es solo un INSERT. */
    @ElementCollection
    @CollectionTable(name = "publicacion_historial", joinColumns = @JoinColumn(name = "publicacion_id"))
    @OrderColumn(name = "orden")
    private List<CambioEstado> historial = new ArrayList<>();
    private LocalDateTime fechaPublicacion;
    @Version
    private long version;

    protected Publicacion() {
    }

    /* Simple Factory: reconstruye el objeto estado desde el nombre persistido. */
    @PostLoad
    private void reconstruirEstado() {
        this.estado = EstadoPublicacionFactory.desdeNombre(nombreEstado);
    }

    /* Crea la publicacion en BORRADOR: puede estar incompleta hasta que se publique. */
    public Publicacion(Perfil autor, TipoPublicacion tipo, String titulo, String descripcion,
                       Categoria categoria, List<Habilidad> habilidades, Modalidad modalidad,
                       int duracionMinutos, int cupo, SkillCoins precioBase, Set<String> etiquetas,
                       PoliticaCancelacion politicaCancelacion) {
        if (autor == null || tipo == null) {
            throw new ReglaNegocioException("La publicacion necesita autor y tipo");
        }
        if (!autor.puedePublicar(tipo)) {
            throw new ReglaNegocioException("El perfil no puede publicar un " + tipo);
        }
        if (duracionMinutos <= 0 || cupo <= 0) {
            throw new ReglaNegocioException("La duracion y el cupo deben ser positivos");
        }
        this.autor = autor;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.habilidades = new ArrayList<>(habilidades);
        this.modalidad = modalidad;
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
        this.precioBase = precioBase;
        this.etiquetas = new HashSet<>(etiquetas);
        this.politicaCancelacion = politicaCancelacion;
    }

    public void editar(String titulo, String descripcion, SkillCoins precioBase) {
        validarEditable();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precioBase = precioBase;
    }

    public void agregarFranja(FranjaHoraria franja) {
        validarEditable();
        if (franjas.stream().anyMatch(f -> f.seSuperponeCon(franja))) {
            throw new ReglaNegocioException("La franja se superpone con otra de la publicacion");
        }
        franjas.add(franja);
    }

    public void agregarPromocion(Promocion promocion) {
        promociones.add(promocion);
    }

    /* ---- State: cada accion delega en el estado actual ---- */

    public void publicar(String actorId, LocalDateTime ahora) {
        estado.publicar(this, actorId);
        this.fechaPublicacion = ahora;
    }

    public void pausar(String actorId) {
        estado.pausar(this, actorId);
    }

    public void reanudar(String actorId) {
        estado.reanudar(this, actorId);
    }

    public void cerrar(String actorId) {
        estado.cerrar(this, actorId);
    }

    public void cancelar(String actorId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Cancelar una publicacion exige un motivo");
        }
        estado.cancelar(this, actorId, motivo);
    }

    /* Unico punto que cambia el estado: deja la entrada en el historial inmutable. */
    public void cambiarEstado(EstadoPublicacion nuevo, String actorId, String motivo) {
        historial.add(new CambioEstado(LocalDateTime.now(), actorId, estado.getNombre(), nuevo.getNombre(), motivo));
        this.estado = nuevo;
        this.nombreEstado = nuevo.getNombre();
    }

    /* Publicar requiere titulo, categoria, precio no negativo y al menos una franja. */
    public void validarPublicable() {
        List<String> faltantes = new ArrayList<>();
        if (titulo == null || titulo.isBlank()) {
            faltantes.add("titulo");
        }
        if (categoria == null) {
            faltantes.add("categoria");
        }
        if (precioBase == null) {
            faltantes.add("precio");
        }
        if (franjas.isEmpty()) {
            faltantes.add("al menos una franja");
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Publicacion incompleta, falta: " + String.join(", ", faltantes));
        }
    }

    public boolean admiteSolicitudes() {
        return estado.admiteSolicitudes();
    }

    public boolean esVisiblePara(String userId) {
        return estado.esVisiblePara(this, userId);
    }

    public void reservarCupo(FranjaHoraria franja) {
        if (!franjas.contains(franja)) {
            throw new ReglaNegocioException("La franja no pertenece a la publicacion");
        }
        if (cuposOcupados >= cupo) {
            throw new ReglaNegocioException("Cupo agotado");
        }
        franja.ocupar();
        cuposOcupados++;
    }

    public void liberarCupo(FranjaHoraria franja) {
        franja.liberar();
        cuposOcupados = Math.max(0, cuposOcupados - 1);
    }

    /* Arma la cadena de decoradores con las promociones vigentes en la fecha. */
    public ICalculoPrecio cotizar(LocalDate fecha) {
        ICalculoPrecio precio = this;
        for (Promocion promocion : promociones) {
            if (promocion.estaVigente(fecha)) {
                precio = promocion.decorar(precio);
            }
        }
        return precio;
    }

    @Override
    public SkillCoins calcularPrecio() {
        return precioBase;
    }

    /* Descripcion del calculo de precio (ICalculoPrecio), no el texto de la publicacion. */
    @Override
    public String getDescripcion() {
        return "Precio base " + precioBase;
    }

    public boolean esAutor(String userId) {
        return autor.getUserId().equals(userId);
    }

    private void validarEditable() {
        if (!estado.esEditable()) {
            throw new ReglaNegocioException("Solo se puede editar una publicacion en BORRADOR");
        }
    }

    public Long getId() {
        return id;
    }

    public Perfil getAutor() {
        return autor;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    /* Texto de la publicacion (getDescripcion() es el del calculo de precio). */
    public String getTextoDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public List<Habilidad> getHabilidades() {
        return Collections.unmodifiableList(habilidades);
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCupo() {
        return cupo;
    }

    public int getCuposOcupados() {
        return cuposOcupados;
    }

    public SkillCoins getPrecioBase() {
        return precioBase;
    }

    public Set<String> getEtiquetas() {
        return Collections.unmodifiableSet(etiquetas);
    }

    public List<FranjaHoraria> getFranjas() {
        return Collections.unmodifiableList(franjas);
    }

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public PoliticaCancelacion getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public List<Promocion> getPromociones() {
        return Collections.unmodifiableList(promociones);
    }

    public List<CambioEstado> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public long getVersion() {
        return version;
    }
}
