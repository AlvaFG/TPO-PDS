package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/*
 * Perfil profesional. Referencia el userId del servicio de identidad externo.
 * La reputacion no tiene setter: solo se recalcula desde calificaciones verificadas.
 * La baja es logica (activo = false).
 * Persistencia: tabla perfil, UNIQUE(user_id) y @Version (optimistic locking).
 */
@Entity
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPerfil tipo;
    @Column(nullable = false)
    private String nombreVisible;
    private String biografia;
    private ZoneId zonaHoraria;
    @ManyToMany
    @JoinTable(name = "perfil_habilidad", joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> habilidades = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "perfil_interes", joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> intereses = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "perfil_disponibilidad", joinColumns = @JoinColumn(name = "perfil_id"))
    @OrderColumn(name = "orden")
    private List<RangoHorario> disponibilidad = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "perfil_canal", joinColumns = @JoinColumn(name = "perfil_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false)
    private Set<CanalNotificacion> canales = EnumSet.of(CanalNotificacion.INTERNA);
    private double reputacion;
    private int cantidadCalificaciones;
    private boolean activo = true;
    @Version
    private long version;

    protected Perfil() {
    }

    public Perfil(String userId, TipoPerfil tipo, String nombreVisible) {
        if (userId == null || userId.isBlank()) {
            throw new ReglaNegocioException("El perfil debe referenciar un userId");
        }
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de perfil es obligatorio");
        }
        this.userId = userId;
        this.tipo = tipo;
        actualizarDatos(nombreVisible, null, null);
    }

    public void actualizarDatos(String nombreVisible, String biografia, ZoneId zonaHoraria) {
        if (nombreVisible == null || nombreVisible.isBlank()) {
            throw new ReglaNegocioException("El nombre visible es obligatorio");
        }
        this.nombreVisible = nombreVisible;
        this.biografia = biografia;
        this.zonaHoraria = zonaHoraria;
    }

    public void definirHabilidades(List<Habilidad> habilidades, List<Habilidad> intereses) {
        this.habilidades = new ArrayList<>(habilidades);
        this.intereses = new ArrayList<>(intereses);
    }

    public void definirDisponibilidad(List<RangoHorario> rangos) {
        this.disponibilidad = new ArrayList<>(rangos);
    }

    public void elegirCanales(Set<CanalNotificacion> canales) {
        if (canales == null || canales.isEmpty()) {
            throw new ReglaNegocioException("Hay que elegir al menos un canal de notificacion");
        }
        this.canales = EnumSet.copyOf(canales);
    }

    /* Promedio de las calificaciones recibidas: es la unica forma de cambiar la reputacion. */
    public void recalcularReputacion(List<Calificacion> calificaciones) {
        boolean ajenas = calificaciones.stream()
                .anyMatch(c -> !c.getCalificado().getUserId().equals(userId));
        if (ajenas) {
            throw new ReglaNegocioException("Solo cuentan las calificaciones recibidas por el perfil");
        }
        this.cantidadCalificaciones = calificaciones.size();
        this.reputacion = calificaciones.stream().mapToInt(Calificacion::getPuntaje).average().orElse(0);
    }

    /* Los desafios los publica una organizacion; las mentorias y servicios, un profesional. */
    public boolean puedePublicar(TipoPublicacion tipoPublicacion) {
        if (!activo) {
            return false;
        }
        if (tipoPublicacion == TipoPublicacion.DESAFIO) {
            return tipo == TipoPerfil.ORGANIZACION;
        }
        return tipo == TipoPerfil.PROFESIONAL;
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public TipoPerfil getTipo() {
        return tipo;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public List<Habilidad> getHabilidades() {
        return Collections.unmodifiableList(habilidades);
    }

    public List<Habilidad> getIntereses() {
        return Collections.unmodifiableList(intereses);
    }

    public List<RangoHorario> getDisponibilidad() {
        return Collections.unmodifiableList(disponibilidad);
    }

    public Set<CanalNotificacion> getCanales() {
        return Collections.unmodifiableSet(canales);
    }

    public double getReputacion() {
        return reputacion;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public long getVersion() {
        return version;
    }
}
