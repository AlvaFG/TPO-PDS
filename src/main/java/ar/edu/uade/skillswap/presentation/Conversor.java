package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.dto.BilleteraDto;
import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.FranjaDto;
import ar.edu.uade.skillswap.application.dto.MovimientoDto;
import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.dto.PerfilPublicoDto;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.model.Models.Contratacion;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.Franja;
import ar.edu.uade.skillswap.model.Models.Propuesta;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Movimiento;
import ar.edu.uade.skillswap.model.Models.Notificacion;
import ar.edu.uade.skillswap.model.Models.Perfil;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.util.ArrayList;
import java.util.List;

/* Mapeo simple entre los DTO del nucleo y los record de la UI, y entre ids String y Long. */
final class Conversor {

    private Conversor() {
    }

    static Long id(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new ErrorDeAplicacion("Identificador invalido: " + id);
        }
    }

    static int entero(long valor) {
        return Math.toIntExact(valor);
    }

    static Publicacion publicacion(PublicacionDto d) {
        return new Publicacion(String.valueOf(d.id()), d.titulo(), d.descripcion(), d.categoria(),
                d.modalidad() == null ? null : Modalidad.valueOf(d.modalidad()), d.duracionMinutos(),
                entero(d.precio()), d.reputacionAutor(), d.autor(), d.etiquetas(),
                EstadoPublicacion.valueOf(d.estado()), false, String.valueOf(d.autorId()));
    }

    static Contratacion contratacion(ContratacionDto d) {
        return new Contratacion(String.valueOf(d.id()), d.publicacion(), d.contraparte(), d.inicio(),
                entero(d.precio()), EstadoContratacion.valueOf(d.estado()), d.rol(),
                d.propuestaFecha() == null ? null : new Propuesta(d.propuestaFecha(), d.puedeAceptarPropuesta()));
    }

    static Franja franja(FranjaDto d) {
        return new Franja(String.valueOf(d.id()), d.inicio());
    }

    static Notificacion notificacion(NotificacionDto d) {
        String texto = d.tipo().replace('_', ' ').toLowerCase();
        String titulo = Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        return new Notificacion(String.valueOf(d.id()), titulo, d.mensaje(), d.fecha(), d.leida(), d.tipo());
    }

    /* Los movimientos vienen del mas antiguo al mas nuevo; la UI los muestra del mas nuevo al mas antiguo. */
    static List<Movimiento> movimientos(BilleteraDto billetera) {
        List<Movimiento> resultado = new ArrayList<>();
        long saldo = billetera.saldo();
        for (int i = billetera.movimientos().size() - 1; i >= 0; i--) {
            MovimientoDto m = billetera.movimientos().get(i);
            long importe = TipoMovimiento.DEBITO.name().equals(m.tipo()) ? -m.monto() : m.monto();
            resultado.add(new Movimiento(m.concepto(), m.fecha(), entero(importe), entero(saldo)));
            saldo -= importe;
        }
        return resultado;
    }

    /* Los proyectos completados salen del historial resumido del perfil ("titulo - ESTADO"). */
    static Perfil perfil(String userId, PerfilPublicoDto d) {
        long completados = d.historialResumido().stream().filter(h -> h.endsWith(" - COMPLETADA")).count();
        String tipo = d.tipo().charAt(0) + d.tipo().substring(1).toLowerCase();
        return new Perfil(userId, d.nombreVisible(), tipo, d.biografia(), d.habilidades(), d.reputacion(),
                d.cantidadCalificaciones(), entero(completados));
    }
}
