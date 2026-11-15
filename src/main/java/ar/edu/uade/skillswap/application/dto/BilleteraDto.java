package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Billetera;

import java.util.List;

public record BilleteraDto(long saldo, List<MovimientoDto> movimientos) {

    public static BilleteraDto desde(Billetera b) {
        return new BilleteraDto(b.getSaldo().getCantidad(),
                b.getMovimientos().stream().map(MovimientoDto::desde).toList());
    }
}
