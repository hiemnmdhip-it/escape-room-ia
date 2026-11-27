package com.escaperoom.ia.service;

import com.escaperoom.ia.exception.PartidaNoEncontradaException;
import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PartidasService {
    private final ConcurrentHashMap<String, EscapeSession> partidas = new ConcurrentHashMap<>();

    public EscapeSession crear(Idioma idioma) {
        String id = UUID.randomUUID().toString();
        return partidas.computeIfAbsent(id, clave -> new EscapeSession(clave, idioma));
    }

    public EscapeSession obtener(String id) {
        EscapeSession partida = partidas.get(id);
        if (partida == null) {
            throw new PartidaNoEncontradaException();
        }
        return partida;
    }
}
