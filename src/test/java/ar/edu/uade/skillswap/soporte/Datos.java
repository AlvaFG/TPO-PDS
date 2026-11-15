package ar.edu.uade.skillswap.soporte;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/*
 * Datos de prueba reutilizables.
 */
public final class Datos {

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 10, 0);
    public static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    public static final int DURACION = 60;

    private Datos() {
    }

    public static Perfil profesional(String userId) {
        return new Perfil(userId, TipoPerfil.PROFESIONAL, "Perfil " + userId);
    }

    public static Perfil organizacion(String userId) {
        return new Perfil(userId, TipoPerfil.ORGANIZACION, "Organizacion " + userId);
    }

    /* Borrador de mentoria completo, con una franja por cada inicio. */
    public static Publicacion borrador(Perfil autor, Categoria categoria, long precio, int cupo,
                                       PoliticaCancelacion politica, List<Habilidad> habilidades,
                                       LocalDateTime... inicios) {
        Publicacion p = new Publicacion(autor, TipoPublicacion.MENTORIA, "Mentoria de entrevistas", "Practica",
                categoria, habilidades, Modalidad.REMOTA, DURACION, cupo, new SkillCoins(precio),
                Set.of("entrevista"), politica);
        for (LocalDateTime inicio : inicios) {
            p.agregarFranja(new FranjaHoraria(inicio, inicio.plusMinutes(DURACION)));
        }
        return p;
    }

    public static Publicacion publicada(Perfil autor, long precio, int cupo, PoliticaCancelacion politica,
                                        LocalDateTime... inicios) {
        Publicacion p = borrador(autor, new Categoria("Tecnologia"), precio, cupo, politica, List.of(), inicios);
        p.publicar(autor.getUserId(), INICIO.minusDays(10));
        return p;
    }
}
