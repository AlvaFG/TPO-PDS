package ar.edu.uade.skillswap.application.dto;

public record SolicitarCommand(Long publicacionId, Long franjaId, String mensaje, String objetivo) {
}
