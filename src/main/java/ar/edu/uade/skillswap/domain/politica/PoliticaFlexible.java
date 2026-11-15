package ar.edu.uade.skillswap.domain.politica;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

/*
 * Strategy concreta: 100 % con anticipacion >= horasReintegroTotal,
 * 50 % con >= horasReintegroParcial y 0 % si no.
 */
@Entity
@DiscriminatorValue("FLEXIBLE")
public class PoliticaFlexible extends PoliticaCancelacion {

    private int horasReintegroTotal = 24;
    private int horasReintegroParcial = 2;

    public PoliticaFlexible() {
        super("Flexible");
    }

    public PoliticaFlexible(int horasReintegroTotal, int horasReintegroParcial) {
        super("Flexible");
        if (horasReintegroParcial < 0 || horasReintegroTotal < horasReintegroParcial) {
            throw new IllegalArgumentException("Umbrales de reintegro invalidos");
        }
        this.horasReintegroTotal = horasReintegroTotal;
        this.horasReintegroParcial = horasReintegroParcial;
    }

    @Override
    public SkillCoins calcularReintegro(SkillCoins pagado, LocalDateTime inicio, LocalDateTime cancelacion) {
        long horas = horasDeAnticipacion(inicio, cancelacion);
        if (horas >= horasReintegroTotal) {
            return pagado;
        }
        if (horas >= horasReintegroParcial) {
            return pagado.porcentaje(50);
        }
        return SkillCoins.CERO;
    }
}
