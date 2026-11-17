package ar.edu.uade.skillswap.application.dto;

import java.time.LocalDateTime;

/* Franja libre de una publicacion, para elegirla al solicitar o reprogramar. */
public record FranjaDto(Long id, LocalDateTime inicio) {
}
