package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.recomendacion.IEstrategiaRecomendacion;
import ar.edu.uade.skillswap.application.recomendacion.PopularidadReciente;
import ar.edu.uade.skillswap.application.service.RecomendacionService;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* Caso de aceptacion 7: cambiar la estrategia sin modificar el caso de uso consumidor. */
class RecomendacionServiceTest {

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Publicaciones publicaciones = new Repositorios.Publicaciones();
    private final Repositorios.Contrataciones contrataciones = new Repositorios.Contrataciones();

    @BeforeEach
    void preparar() {
        Habilidad java = new Habilidad("Java");
        Perfil prov = perfiles.guardar(Datos.profesional("prov"));
        Perfil cli = perfiles.guardar(Datos.profesional("cli"));
        cli.definirHabilidades(List.of(), List.of(java));
        perfiles.guardar(Datos.profesional("cli2"));

        publicaciones.guardar(publicada(prov, "Java avanzado", List.of(java), 2));
        Publicacion popular = publicaciones.guardar(publicada(prov, "Fotografia", List.of(), 2));
        publicaciones.guardar(publicada(cli, "Propia del cliente", List.of(java), 1));

        for (String cliente : List.of("cli", "cli2")) {
            contrataciones.guardar(new Contratacion(popular, perfiles.buscarPorUserId(cliente).orElseThrow(),
                    popular.getFranjas().get(cliente.equals("cli") ? 0 : 1), "m", "o", Datos.HOY));
        }
    }

    private Publicacion publicada(Perfil autor, String titulo, List<Habilidad> habilidades, int franjas) {
        Publicacion p = Datos.borrador(autor, new Categoria("Cat"), 100, 2, null, habilidades,
                franjas == 1 ? new java.time.LocalDateTime[]{INICIO} : new java.time.LocalDateTime[]{INICIO, INICIO.plusDays(1)});
        p.editar(titulo, "d", p.getPrecioBase());
        p.publicar(autor.getUserId(), INICIO.minusDays(1));
        return p;
    }

    private List<String> recomendar(IEstrategiaRecomendacion estrategia) {
        RecomendacionService service = new RecomendacionService(estrategia, perfiles, publicaciones);
        return service.recomendar("cli", 0, 10).contenido().stream().map(PublicacionDto::titulo).toList();
    }

    @Test
    void afinidadPorHabilidades() {
        assertEquals(List.of("Java avanzado"), recomendar(new AfinidadPorHabilidades()));
    }

    @Test
    void popularidadReciente() {
        assertEquals(List.of("Fotografia", "Java avanzado"),
                recomendar(new PopularidadReciente(contrataciones, Clock.systemDefaultZone())));
    }

    @Test
    void popularidadFueraDeLaVentanaNoCuenta() {
        Clock futuro = Clock.offset(Clock.systemDefaultZone(), java.time.Duration.ofDays(60));
        assertEquals(List.of("Java avanzado", "Fotografia"),
                recomendar(new PopularidadReciente(7, contrataciones, futuro)));
    }

    @Test
    void cualquierEstrategiaSinTocarElServicio() {
        IEstrategiaRecomendacion alReves = (perfil, candidatas) -> {
            List<Publicacion> copia = new ArrayList<>(candidatas);
            java.util.Collections.reverse(copia);
            return copia;
        };
        assertEquals(List.of("Fotografia", "Java avanzado"), recomendar(alReves));
    }

    @Test
    void paginaLasRecomendacionesYExcluyeLasPropias() {
        RecomendacionService service = new RecomendacionService(new PopularidadReciente(contrataciones,
                Clock.systemDefaultZone()), perfiles, publicaciones);
        Pagina<PublicacionDto> segunda = service.recomendar("cli", 1, 1);
        assertEquals(2, segunda.totalElementos());
        assertEquals("Java avanzado", segunda.contenido().get(0).titulo());
        assertThrows(AccesoDenegadoException.class, () -> service.recomendar("desconocido", 0, 10));
    }
}
