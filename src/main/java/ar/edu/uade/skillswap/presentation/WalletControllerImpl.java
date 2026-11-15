package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.service.IBilleteraService;
import ar.edu.uade.skillswap.controllers.UiControllers.WalletController;
import ar.edu.uade.skillswap.model.Models.Movimiento;

import java.util.List;

public class WalletControllerImpl implements WalletController {

    private final IBilleteraService billetera;
    private final EjecutorDeAcciones ejecutor;

    public WalletControllerImpl(IBilleteraService billetera, EjecutorDeAcciones ejecutor) {
        this.billetera = billetera;
        this.ejecutor = ejecutor;
    }

    @Override
    public int consultarSaldo(String userId) {
        return ejecutor.ejecutar(userId, () -> Conversor.entero(billetera.consultar(userId).saldo()));
    }

    @Override
    public List<Movimiento> listarMovimientos(String userId) {
        return ejecutor.ejecutar(userId, () -> Conversor.movimientos(billetera.consultar(userId)));
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return ejecutor.ejecutar(userId, () -> Conversor.entero(billetera.acreditarDemo(userId, importe).saldo()));
    }
}
