package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados) {}

    /* autorId es el id del perfil del proveedor (null en el mock): permite ver su perfil. */
    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              int precio, double reputacion, String proveedor,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada, String autorId) {
        public Publicacion(String id, String titulo, String descripcion, String categoria,
                           Modalidad modalidad, int duracionMinutos, int precio, double reputacion,
                           String proveedor, List<String> etiquetas, EstadoPublicacion estado,
                           boolean destacada) {
            this(id, titulo, descripcion, categoria, modalidad, duracionMinutos, precio, reputacion,
                    proveedor, etiquetas, estado, destacada, null);
        }
    }

    /* rol es CLIENTE o PROVEEDOR (null en el mock); propuesta es una reprogramacion pendiente (o null). */
    public record Contratacion(String id, String titulo, String contraparte,
                               LocalDateTime fecha, int precio, EstadoContratacion estado,
                               String rol, Propuesta propuesta) {
        public Contratacion(String id, String titulo, String contraparte, LocalDateTime fecha,
                            int precio, EstadoContratacion estado) {
            this(id, titulo, contraparte, fecha, precio, estado, null, null);
        }
    }

    public record Propuesta(LocalDateTime nuevaFecha, boolean puedoAceptar) {}

    public record Franja(String id, LocalDateTime inicio) {}

    public record Politica(String id, String nombre) {}

    public record Movimiento(String concepto, LocalDateTime fecha, int importe, int saldo) {}

    public record Notificacion(String id, String titulo, String detalle,
                               LocalDateTime fecha, boolean leida, String tipo) {}

    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad,
                                  int precioMaximo, String orden) {}
}
