package com.escaperoom.ia.controller;

import com.escaperoom.ia.dto.AccionEscena;
import com.escaperoom.ia.dto.EstadoPartida;
import com.escaperoom.ia.dto.ResultadoAccion;
import com.escaperoom.ia.model.Idioma;
import com.escaperoom.ia.model.MensajeHistorial;
import com.escaperoom.ia.service.EscenaService;
import com.escaperoom.ia.service.NarradorService;
import com.escaperoom.ia.service.PartidasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/partidas")
public class PartidaApiController {
    private final PartidasService partidas;
    private final EscenaService escena;
    private final NarradorService narrador;

    public PartidaApiController(PartidasService partidas, EscenaService escena, NarradorService narrador) {
        this.partidas = partidas;
        this.escena = escena;
        this.narrador = narrador;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoPartida crear(@RequestParam(defaultValue = "ES") Idioma idioma) {
        return partidas.crear(idioma).estado();
    }

    @GetMapping("/{sessionId}")
    public EstadoPartida estado(@PathVariable String sessionId) {
        return partidas.obtener(sessionId).estado();
    }

    @GetMapping("/{sessionId}/historial")
    public List<MensajeHistorial> historial(@PathVariable String sessionId) {
        return partidas.obtener(sessionId).historial();
    }

    @PatchMapping("/{sessionId}/idioma")
    public EstadoPartida idioma(@PathVariable String sessionId, @RequestParam Idioma idioma) {
        var partida = partidas.obtener(sessionId);
        return partida.enTurno(() -> {
            partida.cambiarIdioma(idioma);
            return partida.estado();
        });
    }

    @PostMapping("/{sessionId}/escena")
    public ResultadoAccion escena(@PathVariable String sessionId, @Valid @RequestBody AccionEscena accion) {
        if (accion.tipo() == AccionEscena.Tipo.RESOLVER && accion.sello() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el número de sello");
        }
        return escena.ejecutar(partidas.obtener(sessionId), accion);
    }

    @PostMapping(path = "/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable String sessionId, @Valid @RequestBody MensajeChat mensaje) {
        var partida = partidas.obtener(sessionId);
        var emisor = new SseEmitter(60_000L);
        var conectado = new AtomicBoolean(true);
        emisor.onCompletion(() -> conectado.set(false));
        emisor.onError(error -> conectado.set(false));
        emisor.onTimeout(() -> conectado.set(false));
        Thread.startVirtualThread(() -> {
            try {
                var resultado = narrador.conversar(partida, mensaje.texto(), Boolean.TRUE.equals(mensaje.accion()),
                        texto -> enviar(emisor, conectado, "texto", texto));
                enviar(emisor, conectado, "estado", resultado);
                if (conectado.get()) {
                    emisor.complete();
                }
            } catch (RuntimeException e) {
                if (conectado.get()) {
                    emisor.completeWithError(e);
                }
            }
        });
        return emisor;
    }

    private static void enviar(SseEmitter emisor, AtomicBoolean conectado, String evento, Object datos) {
        if (conectado.get()) {
            try {
                emisor.send(SseEmitter.event().name(evento).data(datos));
            } catch (IOException | IllegalStateException e) {
                conectado.set(false);
            }
        }
    }

    public record MensajeChat(@NotBlank @Size(max = 2000) String texto, Boolean accion) {}

}
