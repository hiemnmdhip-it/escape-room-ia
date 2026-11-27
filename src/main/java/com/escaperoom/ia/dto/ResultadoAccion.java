package com.escaperoom.ia.dto;

public record ResultadoAccion(String mensaje, EstadoPartida estado, String pista, String diagnostico,
        String runaFallida) {
    public ResultadoAccion(String mensaje, EstadoPartida estado, String pista, String diagnostico) {
        this(mensaje, estado, pista, diagnostico, null);
    }
}
