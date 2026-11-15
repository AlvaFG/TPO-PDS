package ar.edu.uade.skillswap.application.recomendacion;

import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Strategy concreta: puntua cada publicacion por la cantidad de habilidades que comparte
 * con los intereses y habilidades del perfil. Descarta las que no comparten ninguna.
 */
public class AfinidadPorHabilidades implements IEstrategiaRecomendacion {

    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas) {
        Set<Habilidad> afines = new HashSet<>(perfil.getIntereses());
        afines.addAll(perfil.getHabilidades());
        return candidatas.stream()
                .filter(p -> puntaje(p, afines) > 0)
                .sorted(Comparator.comparingLong((Publicacion p) -> puntaje(p, afines)).reversed())
                .toList();
    }

    private long puntaje(Publicacion publicacion, Set<Habilidad> afines) {
        return publicacion.getHabilidades().stream().filter(afines::contains).count();
    }
}
