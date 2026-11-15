package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.MovimientoCredito;

import java.time.LocalDateTime;

public record MovimientoDto(LocalDateTime fecha, String tipo, long monto, String concepto) {

    public static MovimientoDto desde(MovimientoCredito m) {
        return new MovimientoDto(m.getFecha(), m.getTipo().name(), m.getMonto().getCantidad(), m.getConcepto());
    }
}
