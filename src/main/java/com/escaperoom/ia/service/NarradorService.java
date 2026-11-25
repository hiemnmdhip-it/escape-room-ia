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
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class NarradorService {
    private static final Logger LOG = LoggerFactory.getLogger(NarradorService.class);
    private static final Pattern CONSULTA = Pattern.compile(
            "^(?:qué|que significa|cuál|cual|cómo|como|dónde|donde|a qué|a que|por qué|por que|"
            + "explica|explícame|explicame|define|dime qué|dime que|me puedes explicar|puedes explicar|"
            + "what|which|where|how|why|can you explain|could you explain|tell me|explain)\\b");
    private final ChatClient cliente;
    private final HistorialService historial;
    private final String prompt;

    public NarradorService(ObjectProvider<ChatClient.Builder> cliente, HistorialService historial,
            @Value("classpath:cripta-system-prompt.txt") Resource prompt) {
        this.cliente = cliente.stream().findFirst().map(ChatClient.Builder::build).orElse(null);
        this.historial = historial;
        try {
            this.prompt = prompt.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ResultadoAccion conversar(EscapeSession partida, String entrada, boolean accion, Consumer<String> fragmentos) {
        return partida.enTurno(() -> narrarTurno(partida, entrada.strip(), accion, fragmentos));
    }

    private ResultadoAccion narrarTurno(EscapeSession partida, String entrada, boolean accion, Consumer<String> fragmentos) {
        var estadoInicial = partida.estado();
        var ventana = historial.ventana(partida);
        historial.registrar(partida, Autor.JUGADOR, entrada);
        Idioma idioma = estadoInicial.idioma();
        if (entrada.equalsIgnoreCase("/diagnostico")) {
            String informe = partida.diagnostico(true);
            String mensaje = informe == null
                    ? idioma.texto("El informe se abre al terminar.", "The report opens when the game ends.") : informe;
            historial.registrar(partida, Autor.NARRADOR, mensaje);
            fragmentos.accept(mensaje);
            return new ResultadoAccion(mensaje, estadoInicial, null, null);
        }

        var herramientas = new HerramientasCripta(partida);
        StringBuilder texto = new StringBuilder();
        if (cliente != null) {
            var peticion = cliente.prompt().system(prompt + "\nIdioma: " + idioma
                    + "\nEstado actual validado: " + estadoInicial
                    + "\nContexto: altar con balanza de sangre; órgano de runas; candelabro de compuertas; "
                    + "cofre de obsidiana; pergamino rúnico, estaca de plata y cáliz de ceniza.")
                    .messages(ventana).user(entrada);
            if (permiteAccion(entrada, accion) && estadoInicial.resultado().equals("PLAYING")) {
                peticion.tools(herramientas);
            }
            try {
                peticion.stream().content().timeout(Duration.ofSeconds(30)).toIterable().forEach(fragmento -> {
                    texto.append(fragmento);
                    fragmentos.accept(fragmento);
                });
            } catch (RuntimeException e) {
                LOG.warn("El narrador no completó el turno", e);
            }
        }
        ResultadoAccion verificado = herramientas.ultimaAccion();
        if (texto.isEmpty()) {
            agregar(texto, fragmentos, verificado == null
                    ? idioma.texto("El narrador no responde ahora.", "The narrator is unavailable right now.")
                    : verificado.mensaje());
        }
        String pista = verificado == null ? null : verificado.pista();
        String diagnostico = verificado == null ? null : verificado.diagnostico();
        if (pista != null && texto.indexOf(pista) < 0) {
            agregar(texto, fragmentos, pista);
        }
        agregar(texto, fragmentos, diagnostico);
        historial.registrar(partida, Autor.NARRADOR, texto.toString());
        return new ResultadoAccion(texto.toString(), partida.estado(), pista, diagnostico);
    }

    static boolean permiteAccion(String entrada, boolean accion) {
        return accion && entrada.indexOf('?') < 0 && entrada.indexOf('¿') < 0
                && !CONSULTA.matcher(entrada.toLowerCase(Locale.ROOT)).find();
    }

    private void agregar(StringBuilder texto, Consumer<String> fragmentos, String añadido) {
        if (añadido != null) {
            String fragmento = texto.isEmpty() ? añadido : "\n\n" + añadido;
            texto.append(fragmento);
            fragmentos.accept(fragmento);
        }
    }
}
