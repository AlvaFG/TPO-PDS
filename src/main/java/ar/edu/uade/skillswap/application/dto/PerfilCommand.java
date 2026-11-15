package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.RangoHorario;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;

import java.util.List;
import java.util.Set;

/*
 * Datos para crear o actualizar un perfil. zonaHoraria es un id de java.time.ZoneId
 * (por ejemplo "America/Argentina/Buenos_Aires"). El tipo solo se usa al crear.
 */
public record PerfilCommand(TipoPerfil tipo, String nombreVisible, String biografia, String zonaHoraria,
                            List<Long> habilidadIds, List<Long> interesIds,
                            List<RangoHorario> disponibilidad, Set<CanalNotificacion> canales) {
}
