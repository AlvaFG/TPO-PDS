package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Notificacion;

import java.time.LocalDateTime;

public record NotificacionDto(Long id, String tipo, String mensaje, LocalDateTime fecha, boolean leida) {

    public static NotificacionDto desde(Notificacion n) {
        return new NotificacionDto(n.getId(), n.getTipoEvento().name(), n.getMensaje(), n.getFecha(), n.isLeida());
    }
}
