package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.model.Models;

/* La UI conoce una modalidad HIBRIDA que el dominio no tiene (REMOTA y PRESENCIAL). */
final class ModalidadUi {

    private ModalidadUi() {
    }

    static ar.edu.uade.skillswap.domain.model.Modalidad aDominio(Models.Modalidad modalidad) {
        if (modalidad == Models.Modalidad.HIBRIDA) {
            throw new ErrorDeAplicacion("La modalidad hibrida no esta disponible");
        }
        return ar.edu.uade.skillswap.domain.model.Modalidad.valueOf(modalidad.name());
    }
}
