package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.BilleteraDto;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;

public class BilleteraService implements IBilleteraService {

    private final IBilleteraRepository billeteraRepository;

    public BilleteraService(IBilleteraRepository billeteraRepository) {
        this.billeteraRepository = billeteraRepository;
    }

    @Override
    public BilleteraDto consultar(String userId) {
        return billeteraRepository.buscarPorUserId(userId)
                .map(BilleteraDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene billetera"));
    }

    @Override
    public BilleteraDto acreditarDemo(String userId, long importe) {
        var billetera = billeteraRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene billetera"));
        billetera.acreditar(new SkillCoins(importe), TipoMovimiento.CREDITO, "Acreditacion de demostracion", null);
        return BilleteraDto.desde(billeteraRepository.guardar(billetera));
    }
}
