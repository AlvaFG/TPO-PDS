package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;

public record CategoriaDto(Long id, String nombre) {

    public static CategoriaDto desde(Categoria c) {
        return new CategoriaDto(c.getId(), c.getNombre());
    }
}
