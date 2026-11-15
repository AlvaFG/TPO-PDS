package ar.edu.uade.skillswap.domain.politica;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

/*
 * Strategy concreta: nunca devuelve el 100 %.
 * 50 % con anticipacion >= horasReintegroParcial y 0 % si no.
 */
@Entity
@DiscriminatorValue("ESTRICTA")
public class PoliticaEstricta extends PoliticaCancelacion {

    private int horasReintegroParcial = 72;

    public PoliticaEstricta() {
        super("Estricta");
    }

    public PoliticaEstricta(int horasReintegroParcial) {
        super("Estricta");
        if (horasReintegroParcial < 0) {
            throw new IllegalArgumentException("Umbral de reintegro invalido");
        }
        this.horasReintegroParcial = horasReintegroParcial;
    }

    @Override
    public SkillCoins calcularReintegro(SkillCoins pagado, LocalDateTime inicio, LocalDateTime cancelacion) {
        if (horasDeAnticipacion(inicio, cancelacion) >= horasReintegroParcial) {
            return pagado.porcentaje(50);
        }
        return SkillCoins.CERO;
    }
}
