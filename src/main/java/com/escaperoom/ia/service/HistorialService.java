package com.escaperoom.ia.service;

import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.MensajeHistorial;
import com.escaperoom.ia.model.MensajeHistorial.Autor;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class HistorialService {
    private final ChatMemory memoria;

    public HistorialService(ChatMemory memoria) {
        this.memoria = memoria;
    }

    public void registrar(EscapeSession partida, Autor autor, String texto) {
        synchronized (partida) {
            Message mensaje = autor == Autor.JUGADOR ? new UserMessage(texto) : new AssistantMessage(texto);
            memoria.add(partida.id(), mensaje);
            partida.anotar(new MensajeHistorial(Instant.now(), autor, texto));
        }
    }

    public List<Message> ventana(EscapeSession partida) {
        synchronized (partida) {
            return List.copyOf(memoria.get(partida.id()));
        }
    }
}
