package ar.edu.uade.skillswap.application.recomendacion;

import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.util.List;

/*
 * Strategy Pattern - Estrategia de recomendacion.
 * Devuelve un subconjunto de las candidatas, ordenado de la mas a la menos recomendada.
 * Se elige en el composition root: el caso de uso no cambia.
 */
public interface IEstrategiaRecomendacion {

    List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas);
}
