package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;

/* Puerto de entrada: recomendaciones para el usuario. */
public interface IRecomendacionService {

    Pagina<PublicacionDto> recomendar(String userId, int pagina, int tamanio);
}
