package com.escaperoom.ia.dto;

import com.escaperoom.ia.model.Idioma;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EstadoPartida(String sessionId, Idioma idioma, String resultado,
        int turnosEstancados, int sellosResueltos, Map<String, String> escena, List<String> inventario,
        String reliquiaSeleccionada, Calibracion calibracion, String camara, Set<String> visitadas,
        Map<String, Integer> fasesRunas, List<String> reliquiasPendientes) {
    public record Calibracion(int minimo, int maximo, int lecturasRestantes) {}
}
