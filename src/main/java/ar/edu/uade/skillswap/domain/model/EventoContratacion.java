package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Evento de dominio de una contratacion.
 * Sabe a quien le interesa cada tipo de evento (Information Expert).
 */
public class EventoContratacion {

    private final TipoEvento tipo;
    private final Long contratacionId;
    private final String clienteId;
    private final String proveedorId;
    private final LocalDateTime fecha;

    public EventoContratacion(TipoEvento tipo, Long contratacionId, String clienteId,
                              String proveedorId, LocalDateTime fecha) {
        this.tipo = tipo;
        this.contratacionId = contratacionId;
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.fecha = fecha;
    }

    public static EventoContratacion de(Contratacion contratacion, TipoEvento tipo) {
        return new EventoContratacion(tipo, contratacion.getId(), contratacion.getCliente().getUserId(),
                contratacion.getProveedor().getUserId(), LocalDateTime.now());
    }

    /* La solicitud le interesa al proveedor y la aceptacion al cliente; el resto, a las dos partes. */
    public List<String> destinatarios() {
        return switch (tipo) {
            case SOLICITUD_RECIBIDA -> List.of(proveedorId);
            case SOLICITUD_ACEPTADA -> List.of(clienteId);
            case PAGO_CONFIRMADO, REPROGRAMACION, CANCELACION, SESION_COMPLETADA -> List.of(clienteId, proveedorId);
            case CALIFICACION_REGISTRADA -> List.of();
        };
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
