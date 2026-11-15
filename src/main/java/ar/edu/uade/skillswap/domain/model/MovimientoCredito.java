package ar.edu.uade.skillswap.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/*
 * Movimiento de la billetera. contratacionId permite detectar el doble cobro.
 */
@Entity
public class MovimientoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;
    @Embedded
    @AttributeOverride(name = "cantidad", column = @Column(name = "monto", nullable = false))
    private SkillCoins monto;
    private String concepto;
    private Long contratacionId;

    protected MovimientoCredito() {
    }

    public MovimientoCredito(LocalDateTime fecha, TipoMovimiento tipo, SkillCoins monto,
                             String concepto, Long contratacionId) {
        this.fecha = fecha;
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.contratacionId = contratacionId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public SkillCoins getMonto() {
        return monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public Long getContratacionId() {
        return contratacionId;
    }
}
