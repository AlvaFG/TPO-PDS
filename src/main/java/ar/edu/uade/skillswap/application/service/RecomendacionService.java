package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.application.recomendacion.IEstrategiaRecomendacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.util.List;

/*
 * Strategy Pattern - Contexto: delega el orden en la estrategia inyectada.
 * Cambiar de estrategia no modifica este caso de uso.
 */
public class RecomendacionService implements IRecomendacionService {

    private final IEstrategiaRecomendacion estrategia;
    private final IPerfilRepository perfilRepository;
    private final IPublicacionRepository publicacionRepository;

    public RecomendacionService(IEstrategiaRecomendacion estrategia, IPerfilRepository perfilRepository,
                                IPublicacionRepository publicacionRepository) {
        this.estrategia = estrategia;
        this.perfilRepository = perfilRepository;
        this.publicacionRepository = publicacionRepository;
    }

    /* Candidatas: publicaciones PUBLICADAS que no son del propio usuario. */
    @Override
    public Pagina<PublicacionDto> recomendar(String userId, int pagina, int tamanio) {
        Perfil perfil = perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new AccesoDenegadoException("El usuario no tiene perfil"));
        List<Publicacion> candidatas = publicacionRepository.listarPublicadas().stream()
                .filter(p -> !p.esAutor(userId))
                .toList();
        return Pagina.de(estrategia.recomendar(perfil, candidatas), pagina, tamanio).map(PublicacionDto::desde);
    }
}
