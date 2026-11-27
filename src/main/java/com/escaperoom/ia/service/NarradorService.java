package com.escaperoom.ia.service;

import com.escaperoom.ia.dto.ResultadoAccion;
import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import com.escaperoom.ia.model.MensajeHistorial.Autor;
import com.escaperoom.ia.tools.HerramientasCripta;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class NarradorService {
    private static final Logger LOG = LoggerFactory.getLogger(NarradorService.class);
    private static final Duration ESPERA_MODELO = Duration.ofSeconds(90);
    private static final Duration PAUSA_TEXTO_LOCAL = Duration.ofMillis(45);
    private static final Pattern BLOQUE_TEXTO = Pattern.compile("\\s*\\S+(?:\\h+\\S+)?\\s*|\\s+");
    private static final Pattern CONSULTA = Pattern.compile(
            "^(?:qué|que significa|cuál|cual|cómo|como|dónde|donde|a qué|a que|por qué|por que|"
            + "explica|explícame|explicame|define|dime qué|dime que|me puedes explicar|puedes explicar|"
            + "what|which|where|how|why|can you explain|could you explain|tell me|explain)\\b");
    private static final Pattern CONSULTA_SIGNIFICADO = Pattern.compile(
            "\\b(?:qu[eé] significa|(?:a )?qu[eé] te refieres|what .+ means|what .*mean|meaning of)\\b");
    private static final Pattern ORDEN = Pattern.compile(
            "^(?:inspecciona|examino|examinar|examina|inspect|examine|abre|abro|abrir|open|recoge|recojo|"
            + "collect|usa|uso|usar|use|calibra|calibro|calibrar|calibrate|apaga|apago|apagar|extinguish|"
            + "cruza|cruzo|cruzar|go to)\\b");
    private static final Pattern OBJETIVO_ORDEN = Pattern.compile(
            "\\b(?:altar|[oó]rgano|organ|candelabro|chandelier|cofre|chest|c[aá]liz|chalice|pergamino|scroll|"
            + "estaca|stake|cripta|crypt|estancia|room|sala|runa|rune|c[aá]mara|chamber)\\b");
    private static final Pattern ELECCION = Pattern.compile(
            "^(?:(?:elijo|opci[oó]n|choose|option)\\s+)?([A-Z]|[0-9]+)[.!]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACLARACION = Pattern.compile(
            "^[¿¡]?(?:explica|expl[ií]came|c[oó]mo as[ií]|no entiendo|a qu[eé] te refieres|qu[eé] significa (?:eso|esto)|"
            + "explain|what do you mean|what does (?:it|that|this) mean|i (?:don't|do not) understand)[.!?]*$");
    private static final Pattern PEDIR_SOLUCION = Pattern.compile(
            "\\b(?:soluci[oó]n|spoiler|(?:opci[oó]n|respuesta|n[uú]mero|orden) correct[ao]|"
            + "solution|correct (?:answer|option|number|order)|which (?:one|option) (?:is right|should i pick))\\b");
    private final ChatClient cliente;
    private final HistorialService historial;
    private final String prompt;

    public NarradorService(ObjectProvider<ChatClient.Builder> cliente, HistorialService historial,
            @Value("classpath:cripta-system-prompt.txt") Resource prompt) {
        this.cliente = cliente.orderedStream().findFirst()
                .map(ChatClient.Builder::build).orElse(null);
        this.historial = historial;
        try {
            this.prompt = prompt.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ResultadoAccion conversar(EscapeSession partida, String entrada, boolean accion, Consumer<String> fragmentos) {
        return conversar(partida, entrada, accion, fragmentos, verificado -> {});
    }

    public ResultadoAccion conversar(EscapeSession partida, String entrada, boolean accion, Consumer<String> fragmentos,
            Consumer<ResultadoAccion> alEjecutar) {
        return partida.enTurno(() -> {
            String mensaje = entrada.strip();
            ResultadoAccion resultado = narrarTurno(partida, mensaje, accion, fragmentos, alEjecutar);
            historial.registrar(partida, Autor.JUGADOR, mensaje);
            historial.registrar(partida, Autor.NARRADOR, resultado.mensaje());
            return resultado;
        });
    }

    public Flux<String> transmitir(Function<Consumer<String>, ResultadoAccion> turno,
            Consumer<ResultadoAccion> alTerminar) {
        return Flux.create(salida -> {
            ResultadoAccion resultado = turno.apply(salida::next);
            alTerminar.accept(resultado);
            salida.complete();
        });
    }

    public String bienvenida(Idioma idioma) {
        return idioma.texto(
                "Has despertado en una cripta que no figura en ningún mapa. Mala señal. Al menos la arquitectura tiene buen gusto."
                        + "\n\nTres sellos cierran la salida. Examina el altar para empezar. Puedes explorar la sala y pedirme pistas.",
                "You woke inside a crypt absent from every map. Bad sign. At least the architecture has taste."
                        + "\n\nThree seals guard the exit. Inspect the altar to begin. Explore the room and ask me for hints.");
    }

    public ResultadoAccion emitirBienvenida(EscapeSession partida, Consumer<String> fragmentos) {
        return partida.enTurno(() -> {
            var estado = partida.estado();
            String mensaje = bienvenida(estado.idioma());
            if (partida.historial().stream().noneMatch(entrada -> entrada.autor() == Autor.NARRADOR
                    && entrada.texto().equals(mensaje))) {
                historial.registrar(partida, Autor.NARRADOR, mensaje);
            }
            return emitirResultado(new ResultadoAccion(mensaje, estado, null, null), fragmentos);
        });
    }

    public ResultadoAccion emitirResultado(ResultadoAccion resultado, Consumer<String> fragmentos) {
        StringBuilder texto = new StringBuilder();
        agregar(texto, fragmentos, unirOrientacion(resultado.mensaje(), resultado.pista()));
        if (resultado.diagnostico() != null) {
            agregar(texto, fragmentos, resultado.diagnostico());
        }
        return new ResultadoAccion(texto.toString(), resultado.estado(), resultado.pista(), resultado.diagnostico(),
                resultado.runaFallida());
    }

    private ResultadoAccion narrarTurno(EscapeSession partida, String entrada, boolean accion, Consumer<String> fragmentos,
            Consumer<ResultadoAccion> alEjecutar) {
        var estadoInicial = partida.estado();
        Idioma idioma = estadoInicial.idioma();
        if (entrada.equalsIgnoreCase("/diagnostico")) {
            String informe = partida.diagnostico(true);
            String mensaje = informe == null
                    ? idioma.texto("El informe se abre al terminar.", "The report opens when the game ends.") : informe;
            return emitirResultado(new ResultadoAccion(mensaje, estadoInicial, null, null), fragmentos);
        }

        var herramientas = new HerramientasCripta(partida, alEjecutar);
        boolean jugando = estadoInicial.resultado().equals("PLAYING");
        var eleccion = ELECCION.matcher(entrada);
        if (jugando && estadoInicial.sellosResueltos() < 3 && accion && eleccion.matches()) {
            herramientas.resolverSello(estadoInicial.sellosResueltos() + 1, eleccion.group(1));
            return emitirResultado(herramientas.ultimaAccion(), fragmentos);
        }
        boolean orden = jugando && permiteAccion(entrada, accion);
        if (orden && cliente != null) {
            var peticion = cliente.prompt().system(prompt + "\nEjecuta solo la acción de escena que el jugador pidió.")
                    .user(FiltroNarracion.ocultarOpciones(entrada))
                    .tools(Arrays.stream(ToolCallbacks.from(herramientas))
                            .filter(tool -> List.of("inspeccionarElemento", "recogerReliquia", "usarReliquia")
                                    .contains(tool.getToolDefinition().name()))
                            .toList());
            emitirModelo(peticion, fragmento -> {});
            if (herramientas.ultimaAccion() != null) {
                return emitirResultado(herramientas.ultimaAccion(), fragmentos);
            }
        }
        String respuesta = conversarConModelo(partida, entrada, fragmentos);
        return new ResultadoAccion(respuesta, estadoInicial, null, null);
    }

    static boolean permiteAccion(String entrada, boolean accion) {
        String normalizada = entrada.toLowerCase(Locale.ROOT);
        return accion && entrada.indexOf('?') < 0 && entrada.indexOf('¿') < 0
                && ORDEN.matcher(normalizada).find()
                && OBJETIVO_ORDEN.matcher(normalizada).find()
                && !CONSULTA.matcher(normalizada).find() && !CONSULTA_SIGNIFICADO.matcher(normalizada).find()
                && !PEDIR_SOLUCION.matcher(normalizada).find();
    }

    private String conversarConModelo(EscapeSession partida, String entrada, Consumer<String> fragmentos) {
        if (cliente == null) {
            throw new IllegalStateException("Ollama no está disponible para este turno");
        }
        var ventana = historial.ventana(partida);
        List<String> respuestasAnteriores = ventana.stream()
                .filter(mensaje -> mensaje.getMessageType() == MessageType.ASSISTANT)
                .map(Message::getText)
                .toList();
        var estado = partida.estado();
        boolean enOrgano = "READY".equals(estado.escena().get("organ"));
        String sistema = prompt + "\nIdioma: " + estado.idioma()
                + "\nContexto de la prueba: " + partida.contextoConversacional();
        String peticion = FiltroNarracion.ocultarOpciones(entrada);
        if (ACLARACION.matcher(entrada.toLowerCase(Locale.ROOT)).matches()) {
            sistema += "\nEs una petición de aclaración. Empieza con un ejemplo cotidiano concreto y relaciónalo "
                    + "en la frase siguiente con el criterio de la prueba. Evita saludos y ambientación de relleno.";
            String temaAnterior = temaDeAclaracion(ventana);
            if (temaAnterior != null) {
                peticion += "\nPregunta anterior del jugador: " + FiltroNarracion.ocultarOpciones(temaAnterior);
            }
        }
        for (String instruccion : List.of("", "\nEnsaya una analogía nueva. Usa palabras, sin cifras, etiquetas ni respuestas del juego.")) {
            var filtro = new FiltroNarracion(fragmentos, respuestasAnteriores, enOrgano);
            boolean completo = emitirModelo(cliente.prompt().system(sistema + instruccion).user(peticion),
                    filtro::aceptar);
            if (completo) {
                filtro.completar();
            }
            if (!filtro.texto().isEmpty()) {
                return filtro.texto();
            }
            if (!completo) {
                break;
            }
        }
        throw new IllegalStateException("Ollama no devolvió una respuesta publicable");
    }

    private static String temaDeAclaracion(List<Message> ventana) {
        return ventana.reversed().stream()
                .filter(mensaje -> mensaje.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .filter(texto -> !ELECCION.matcher(texto).matches() && !permiteAccion(texto, true)
                        && !ACLARACION.matcher(texto.toLowerCase(Locale.ROOT)).matches()
                        && !PEDIR_SOLUCION.matcher(texto.toLowerCase(Locale.ROOT)).find())
                .findFirst().orElse(null);
    }

    private static String unirOrientacion(String texto, String orientacion) {
        return orientacion == null || texto.contains(orientacion) ? texto : texto + " " + orientacion;
    }

    private boolean emitirModelo(ChatClient.ChatClientRequestSpec peticion, Consumer<String> fragmentos) {
        try {
            Flux<String> tokens = peticion.stream().content();
            tokens.timeout(ESPERA_MODELO).publishOn(Schedulers.boundedElastic(), 1).doOnNext(fragmentos).blockLast();
            return true;
        } catch (RuntimeException e) {
            LOG.warn("El narrador no completó el turno", e);
            return false;
        }
    }

    private void agregar(StringBuilder texto, Consumer<String> fragmentos, String añadido) {
        if (añadido != null) {
            boolean pausar = !texto.isEmpty();
            String bloque = pausar ? "\n\n" + añadido : añadido;
            texto.append(bloque);
            var fragmentosLocales = BLOQUE_TEXTO.matcher(bloque);
            while (fragmentosLocales.find()) {
                if (pausar) {
                    try {
                        Thread.sleep(PAUSA_TEXTO_LOCAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                fragmentos.accept(fragmentosLocales.group());
                pausar = true;
            }
        }
    }
}
