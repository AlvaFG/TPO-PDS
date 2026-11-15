package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.domain.model.Contratacion;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/* Puerto de salida de persistencia de contrataciones. */
public interface IContratacionRepository {

    Contratacion guardar(Contratacion contratacion);

    Optional<Contratacion> buscarPorId(Long id);

    /* Contrataciones donde el usuario es cliente o proveedor, de la mas reciente a la mas antigua. */
    Pagina<Contratacion> listarPorParticipante(String userId, int pagina, int tamanio);

    /* Cantidad de contrataciones creadas desde la fecha, agrupadas por id de publicacion. */
    Map<Long, Long> contarRecientesPorPublicacion(LocalDateTime desde);
}
