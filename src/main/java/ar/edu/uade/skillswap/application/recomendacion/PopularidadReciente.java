package ar.edu.uade.skillswap.application.recomendacion;

import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
 * Strategy concreta: ordena las candidatas por cantidad de contrataciones
 * en los ultimos diasVentana dias.
 */
public class PopularidadReciente implements IEstrategiaRecomendacion {

    private final int diasVentana;
    private final IContratacionRepository contratacionRepository;
    private final Clock reloj;

    public PopularidadReciente(IContratacionRepository contratacionRepository, Clock reloj) {
        this(30, contratacionRepository, reloj);
    }

    public PopularidadReciente(int diasVentana, IContratacionRepository contratacionRepository, Clock reloj) {
        this.diasVentana = diasVentana;
        this.contratacionRepository = contratacionRepository;
        this.reloj = reloj;
    }

    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas) {
        LocalDateTime desde = LocalDateTime.now(reloj).minusDays(diasVentana);
        Map<Long, Long> conteos = contratacionRepository.contarRecientesPorPublicacion(desde);
        return candidatas.stream()
                .sorted(Comparator.comparingLong((Publicacion p) -> conteos.getOrDefault(p.getId(), 0L)).reversed())
                .toList();
    }
}
