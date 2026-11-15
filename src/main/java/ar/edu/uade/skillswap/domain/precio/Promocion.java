package ar.edu.uade.skillswap.domain.precio;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import java.time.LocalDate;

/*
 * Definicion persistida de una promocion, con vigencia.
 * Cada subclase crea su decorador porque tiene los datos para inicializarlo (GRASP Creator).
 * Persistencia: SINGLE_TABLE (promocion). Los decoradores (Descuento*) se arman en memoria y no se guardan.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigo;
    @Column(nullable = false)
    private LocalDate vigenteDesde;
    @Column(nullable = false)
    private LocalDate vigenteHasta;

    protected Promocion() {
    }

    protected Promocion(String codigo, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        if (vigenteDesde == null || vigenteHasta == null || vigenteHasta.isBefore(vigenteDesde)) {
            throw new IllegalArgumentException("Vigencia de promocion invalida");
        }
        this.codigo = codigo;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde) && !fecha.isAfter(vigenteHasta);
    }

    public abstract ICalculoPrecio decorar(ICalculoPrecio precio);

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }
}
