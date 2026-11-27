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
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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

    @PostMapping("/{sessionId}/reiniciar")
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoPartida reiniciar(@PathVariable String sessionId, @RequestParam(required = false) Idioma idioma) {
        var anterior = partidas.obtener(sessionId).estado();
        return partidas.crear(idioma == null ? anterior.idioma() : idioma).estado();
    }

    @GetMapping({"/{sessionId}", "/{sessionId}/estado"})
    public EstadoPartida estado(@PathVariable String sessionId) {
        return partidas.obtener(sessionId).estado();
    }

    @GetMapping("/{sessionId}/historial")
    public List<MensajeHistorial> historial(@PathVariable String sessionId) {
        return partidas.obtener(sessionId).historial();
    }

    @GetMapping("/{sessionId}/diagnostico")
    public InformeDiagnostico diagnostico(@PathVariable String sessionId,
            @RequestParam(defaultValue = "false") boolean extenso) {
        String informe = partidas.obtener(sessionId).diagnostico(extenso);
        if (informe == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El informe se abre al terminar");
        }
        return new InformeDiagnostico(informe);
    }

    @GetMapping(path = "/{sessionId}/bienvenida", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter bienvenida(@PathVariable String sessionId) {
        var partida = partidas.obtener(sessionId);
        return transmitir((emitir, alEjecutar) -> narrador.emitirBienvenida(partida, emitir));
    }

    @GetMapping(path = "/{sessionId}/diagnostico/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnosticoStream(@PathVariable String sessionId,
            @RequestParam(defaultValue = "false") boolean extenso) {
        var partida = partidas.obtener(sessionId);
        var resultado = partida.enTurno(() ->
                new ResultadoAccion(diagnostico(sessionId, extenso).informe(), partida.estado(), null, null));
        return transmitir((emitir, alEjecutar) -> narrador.emitirResultado(resultado, emitir));
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
        validarEscena(accion);
        return escena.ejecutar(partidas.obtener(sessionId), accion);
    }

    private static void validarEscena(AccionEscena accion) {
        boolean incompleta = switch (accion.tipo()) {
            case INSPECCIONAR -> vacio(accion.elemento());
            case RECOGER, SELECCIONAR -> vacio(accion.reliquia());
            case USAR -> vacio(accion.reliquia()) || vacio(accion.elemento());
            case RESOLVER -> accion.sello() == null || vacio(accion.intento());
            case PISTA -> false;
        };
        if (incompleta) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos para ejecutar la acción");
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    @PostMapping(path = "/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable String sessionId, @Valid @RequestBody MensajeChat mensaje) {
        mensaje.validar();
        if (mensaje.escena() != null) {
            validarEscena(mensaje.escena());
        }
        var partida = partidas.obtener(sessionId);
        return transmitir((emitir, alEjecutar) -> mensaje.escena() == null
                ? narrador.conversar(partida, mensaje.texto(), Boolean.TRUE.equals(mensaje.accion()), emitir, alEjecutar)
                : escena.ejecutar(partida, mensaje.escena(), verificado -> {
                    alEjecutar.accept(verificado);
                    return narrador.emitirResultado(verificado, emitir);
                }));
    }

    private SseEmitter transmitir(BiFunction<Consumer<String>, Consumer<ResultadoAccion>, ResultadoAccion> turno) {
        var emisor = new SseEmitter(120_000L);
        var conectado = new AtomicBoolean(true);
        emisor.onCompletion(() -> conectado.set(false));
        emisor.onError(error -> conectado.set(false));
        emisor.onTimeout(() -> {
            conectado.set(false);
            emisor.complete();
        });
        Thread.startVirtualThread(() -> {
            try {
                var texto = narrador.transmitir(
                        emitir -> turno.apply(emitir, verificado -> enviar(emisor, conectado, "escena", verificado)),
                        resultado -> enviar(emisor, conectado, "estado", resultado));
                texto.doOnNext(fragmento -> enviar(emisor, conectado, "texto", fragmento)).blockLast();
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
                var mensaje = SseEmitter.event().name(evento);
                if (datos instanceof String texto) {
                    // Los bytes preservan UTF-8 incluso si el conversor de cadenas usa otra codificación por defecto.
                    String lineas = texto.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\ndata: ");
                    mensaje.data((" " + lineas).getBytes(StandardCharsets.UTF_8));
                } else {
                    mensaje.data(datos);
                }
                emisor.send(mensaje);
            } catch (IOException | IllegalStateException e) {
                conectado.set(false);
            }
        }
    }

    public record MensajeChat(@Size(max = 2000) String texto, Boolean accion, @Valid AccionEscena escena) {
        void validar() {
            if (escena == null ? vacio(texto) : texto != null || accion != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía texto o una acción de escena");
            }
        }
    }

    public record InformeDiagnostico(String informe) {}

}
