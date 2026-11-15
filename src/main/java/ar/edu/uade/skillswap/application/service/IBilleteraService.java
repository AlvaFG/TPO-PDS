package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.BilleteraDto;

/* Puerto de entrada: saldo y movimientos de la billetera. */
public interface IBilleteraService {

    BilleteraDto consultar(String userId);

    /* Acreditacion de SkillCoins ficticios para la demo. */
    BilleteraDto acreditarDemo(String userId, long importe);
}
