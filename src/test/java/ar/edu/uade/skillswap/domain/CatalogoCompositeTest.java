package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.soporte.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/* Composite: Tecnologia -> Arquitectura -> Microservicios, con habilidades en cada nivel. */
class CatalogoCompositeTest {

    private Categoria tecnologia;
    private Categoria arquitectura;
    private Categoria microservicios;
    private Habilidad java;
    private Habilidad kafka;

    @BeforeEach
    void armarCatalogo() {
        tecnologia = Ids.asignar(new Categoria("Tecnologia"), 1L);
        arquitectura = Ids.asignar(new Categoria("Arquitectura"), 2L);
        microservicios = Ids.asignar(new Categoria("Microservicios"), 3L);
        java = Ids.asignar(new Habilidad("Java"), 10L);
        kafka = Ids.asignar(new Habilidad("Kafka"), 11L);
        tecnologia.agregar(java);
        tecnologia.agregar(arquitectura);
        arquitectura.agregar(microservicios);
        microservicios.agregar(kafka);
    }

    @Test
    void contieneRecorreTodosLosNiveles() {
        assertTrue(tecnologia.contiene(kafka));
        assertTrue(tecnologia.contiene(microservicios));
        assertFalse(arquitectura.contiene(java));
        assertTrue(kafka.contiene(kafka));
        assertFalse(kafka.contiene(java));
    }

    @Test
    void listaLasHabilidadesDelSubarbol() {
        assertEquals(List.of(java, kafka), tecnologia.listarHabilidades());
        assertEquals(List.of(kafka), arquitectura.listarHabilidades());
        assertEquals(List.of(kafka), kafka.listarHabilidades());
    }

    @Test
    void listaLosIdsDeLaCategoriaYSusSubcategorias() {
        assertEquals(List.of(1L, 2L, 3L), tecnologia.listarSubcategoriaIds());
        assertEquals(List.of(3L), microservicios.listarSubcategoriaIds());
    }

    @Test
    void noPermiteCiclos() {
        assertThrows(IllegalArgumentException.class, () -> microservicios.agregar(tecnologia));
        assertThrows(IllegalArgumentException.class, () -> tecnologia.agregar(tecnologia));
        assertThrows(IllegalArgumentException.class, () -> tecnologia.agregar(null));
    }

    @Test
    void quitarUnHijo() {
        tecnologia.quitar(java);
        assertFalse(tecnologia.contiene(java));
        assertEquals(1, tecnologia.getHijos().size());
    }

    @Test
    void igualdadPorIdPersistido() {
        Habilidad mismaJava = Ids.asignar(new Habilidad("Java"), 10L);
        assertEquals(java, mismaJava);
        assertEquals(java.hashCode(), mismaJava.hashCode());
        assertNotEquals(new Habilidad("Sin id"), new Habilidad("Sin id"));
        assertNotEquals(java, Ids.asignar(new Categoria("Otra"), 10L));
        assertEquals("Java", java.getNombre());
        assertThrows(IllegalArgumentException.class, () -> new Habilidad(" "));
    }
}
