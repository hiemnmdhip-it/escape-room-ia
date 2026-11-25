package com.escaperoom.ia.service;

import static org.junit.jupiter.api.Assertions.*;

import com.escaperoom.ia.exception.PartidaNoEncontradaException;
import com.escaperoom.ia.model.Idioma;
import com.escaperoom.ia.model.MensajeHistorial.Autor;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;

class HistorialServiceTest {
    @Test
    void laVentanaConservaLaColaDelHistorialYSeparaPartidas() throws Exception {
        var repositorio = new InMemoryChatMemoryRepository();
        var servicio = new HistorialService(MessageWindowChatMemory.builder()
                .chatMemoryRepository(repositorio).maxMessages(3).build());
        var partidas = new PartidasService();
        var una = partidas.crear(Idioma.ES);
        var otra = partidas.crear(Idioma.EN);
        assertDoesNotThrow(() -> UUID.fromString(una.id()));
        assertNotEquals(una.id(), otra.id());
        assertSame(una, partidas.obtener(una.id()));
        assertThrows(PartidaNoEncontradaException.class, () -> partidas.obtener("desconocida"));

        try (var executor = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 30; i++) {
                String texto = "Mensaje " + i;
                executor.execute(() -> servicio.registrar(una, Autor.JUGADOR, texto));
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        servicio.registrar(otra, Autor.NARRADOR, "Welcome");
        var bitacora = una.historial();
        assertEquals(30, bitacora.size());
        assertEquals(bitacora.subList(27, 30).stream().map(mensaje -> mensaje.texto()).toList(),
                repositorio.findByConversationId(una.id()).stream().map(Message::getText).toList());
        assertEquals("Welcome", servicio.ventana(otra).getFirst().getText());
        assertThrows(UnsupportedOperationException.class, bitacora::clear);
    }
}
