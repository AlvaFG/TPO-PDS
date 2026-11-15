package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.model.Billetera;

import java.util.Optional;

public interface IBilleteraRepository {

    Billetera guardar(Billetera billetera);

    Optional<Billetera> buscarPorUserId(String userId);
}
