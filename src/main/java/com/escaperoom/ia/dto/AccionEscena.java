package com.escaperoom.ia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccionEscena(@NotNull Tipo tipo, @Size(max = 40) String elemento, @Size(max = 40) String reliquia,
        Integer sello, @Size(max = 40) String intento) {
    public enum Tipo { INSPECCIONAR, RECOGER, USAR, PISTA, RESOLVER, SELECCIONAR }
}
