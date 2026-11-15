package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.domain.model.Notificacion;

import java.util.Optional;

public interface INotificacionRepository {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(Long id);

    /* Bandeja del usuario, de la mas reciente a la mas antigua. */
    Pagina<Notificacion> listarPorDestinatario(String userId, int pagina, int tamanio);
}
