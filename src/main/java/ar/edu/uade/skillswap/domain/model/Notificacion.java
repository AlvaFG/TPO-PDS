package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String destinatarioId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacion canal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipoEvento;
    @Column(nullable = false)
    private String mensaje;
    @Column(nullable = false)
    private LocalDateTime fecha;
    private boolean leida;

    protected Notificacion() {
    }

    public Notificacion(String destinatarioId, CanalNotificacion canal, TipoEvento tipoEvento,
                        String mensaje, LocalDateTime fecha) {
        this.destinatarioId = destinatarioId;
        this.canal = canal;
        this.tipoEvento = tipoEvento;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public void marcarLeida(String userId) {
        if (!destinatarioId.equals(userId)) {
            throw new ReglaNegocioException("Solo el destinatario puede marcar la notificacion como leida");
        }
        leida = true;
    }

    public Long getId() {
        return id;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }
}
