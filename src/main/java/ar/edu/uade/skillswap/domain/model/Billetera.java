package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * Billetera de SkillCoins (Information Expert): no permite saldo negativo
 * ni un segundo debito para la misma contratacion.
 * En la base, UNIQUE(billetera_id, contratacion_id, tipo) en movimiento_credito respalda el doble cobro.
 */
@Entity
public class Billetera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titular_id")
    private Perfil titular;
    @Embedded
    @AttributeOverride(name = "cantidad", column = @Column(name = "saldo", nullable = false))
    private SkillCoins saldo;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "billetera_id", nullable = false)
    @OrderBy("id")
    private List<MovimientoCredito> movimientos = new ArrayList<>();
    @Version
    private long version;

    protected Billetera() {
    }

    public Billetera(Perfil titular, SkillCoins saldoInicial) {
        if (titular == null || saldoInicial == null) {
            throw new ReglaNegocioException("La billetera necesita titular y saldo inicial");
        }
        this.titular = titular;
        this.saldo = saldoInicial;
    }

    public void debitar(SkillCoins monto, String concepto, Long contratacionId) {
        if (contratacionId != null && yaPago(contratacionId)) {
            throw new ReglaNegocioException("La contratacion ya fue pagada: no se permite doble cobro");
        }
        if (!tieneSaldo(monto)) {
            throw new ReglaNegocioException("Saldo insuficiente");
        }
        saldo = saldo.restar(monto);
        movimientos.add(new MovimientoCredito(LocalDateTime.now(), TipoMovimiento.DEBITO, monto, concepto, contratacionId));
    }

    public void acreditar(SkillCoins monto, TipoMovimiento tipo, String concepto, Long contratacionId) {
        saldo = saldo.sumar(monto);
        movimientos.add(new MovimientoCredito(LocalDateTime.now(), tipo, monto, concepto, contratacionId));
    }

    public boolean tieneSaldo(SkillCoins monto) {
        return !saldo.esMenorQue(monto);
    }

    public boolean yaPago(Long contratacionId) {
        return movimientos.stream()
                .anyMatch(m -> m.getTipo() == TipoMovimiento.DEBITO && Objects.equals(m.getContratacionId(), contratacionId));
    }

    public Long getId() {
        return id;
    }

    public Perfil getTitular() {
        return titular;
    }

    public SkillCoins getSaldo() {
        return saldo;
    }

    public List<MovimientoCredito> getMovimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    public long getVersion() {
        return version;
    }
}
