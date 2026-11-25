package com.escaperoom.ia.model;

import java.time.Instant;

public record MensajeHistorial(Instant fecha, Autor autor, String texto) {
    public enum Autor { JUGADOR, NARRADOR }
}
