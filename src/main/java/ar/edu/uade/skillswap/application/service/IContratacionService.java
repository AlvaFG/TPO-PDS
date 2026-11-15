package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;

/* Puerto de entrada: casos de uso de Contratacion. */
public interface IContratacionService {

    /* Devuelve el id de la contratacion creada en PENDIENTE. Con franjaId null usa la primera franja libre. */
    Long solicitar(String userId, SolicitarCommand cmd);

    void aceptar(String userId, Long contratacionId);

    void rechazar(String userId, Long contratacionId, String motivo);

    void pagar(String userId, Long contratacionId);

    void proponerReprogramacion(String userId, Long contratacionId, Long franjaId);

    void aceptarReprogramacion(String userId, Long contratacionId);

    void iniciar(String userId, Long contratacionId);

    void completar(String userId, Long contratacionId);

    /* Devuelve el monto reintegrado al cliente, en SkillCoins. */
    long cancelar(String userId, Long contratacionId, String motivo);

    Pagina<ContratacionDto> listar(String userId, int pagina, int tamanio);

    ContratacionDto verComprobante(String userId, Long contratacionId);
}
