package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.util.List;
import java.util.Optional;

public interface IPoliticaCancelacionRepository {

    Optional<PoliticaCancelacion> buscarPorId(Long id);

    List<PoliticaCancelacion> listar();
}
