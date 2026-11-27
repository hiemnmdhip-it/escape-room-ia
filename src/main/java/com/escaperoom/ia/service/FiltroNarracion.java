package com.escaperoom.ia.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class FiltroNarracion {
    private static final int MAX_FRASES = 3;
    private static final int MAX_CARACTERES = 360;
    private static final Pattern FIN_FRASE = Pattern.compile("[.!?…]+(?=\\s|$)");
    private static final Pattern OPCION_CIFRADA = Pattern.compile(
            "\\p{N}+|\\b(?:uno|dos|tres|one|two|three|primero|primera|segundo|segunda|tercero|tercera|first|second|third)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern OPCION_LITERAL = Pattern.compile(
            "\\b[BC]\\b|(?<=\\p{L}\\s)A\\b|\\bA(?=$|[).,:;!?])|(?iu)^[abc][.!?]*$"
                    + "|(?iu)\\b(?:opci[oó]n|alternativa|letra|respuesta|option|choice|letter|answer)\\s+[abc]\\b"
                    + "|(?iu)\\b(?:es|ser[ií]a|is)\\s+[abc]\\b(?=$|[).,:;!?])");
    private static final Pattern DESTRIPE = Pattern.compile(
            "\\b(?:(?:respuesta|opcion|alternativa|cifra|letra|numero|orden|secuencia) "
                    + "(?:correct[ao]|ganador[ae]?|valid[ao]|exact[ao])|"
                    + "(?:correct|winning|right|exact) (?:answer|option|choice|letter|number|order|sequence)|"
                    + "(?:la |tu |your |the )?(?:solucion|solution) (?:es|seria|is|would be)|"
                    + "elige|escoge|selecciona|marca|responde con|introduce|debes pulsar|"
                    + "pick|select|choose|enter|type in|press)\\b");
    private static final Pattern RELLENO = Pattern.compile("[¡!]|\\b(?:a+h+a+h+a+|jajaja|haha)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JERGA_ORGANO = Pattern.compile(
            "\\b(?:tokens?|system|user|assistant|chat\\s*memory|messagewindowchatmemory|spring\\s+ai)\\b",
            Pattern.CASE_INSENSITIVE);

    private final Consumer<String> emitir;
    private final boolean enOrgano;
    private final Set<String> frasesVistas = new HashSet<>();
    private final StringBuilder pendiente = new StringBuilder();
    private final StringBuilder texto = new StringBuilder();
    private int frases;

    FiltroNarracion(Consumer<String> emitir, List<String> respuestasAnteriores, boolean enOrgano) {
        this.emitir = emitir;
        this.enOrgano = enOrgano;
        for (String respuesta : respuestasAnteriores) {
            for (String frase : FIN_FRASE.split(respuesta)) {
                frasesVistas.add(normalizar(frase));
            }
        }
    }

    static String ocultarOpciones(String texto) {
        return OPCION_LITERAL.matcher(OPCION_CIFRADA.matcher(texto).replaceAll("[alternativa]"))
                .replaceAll("[alternativa]");
    }

    void aceptar(String fragmento) {
        if (frases == MAX_FRASES) {
            return;
        }
        pendiente.append(fragmento);
        var limites = FIN_FRASE.matcher(pendiente);
        int consumido = 0;
        while (frases < MAX_FRASES && limites.find()) {
            publicar(pendiente.substring(consumido, limites.end()));
            consumido = limites.end();
        }
        pendiente.delete(0, consumido);
    }

    void completar() {
        if (frases < MAX_FRASES) {
            publicar(pendiente.toString());
        }
    }

    String texto() {
        return texto.toString();
    }

    private void publicar(String fragmento) {
        String frase = fragmento.strip().replaceAll("\\s+", " ");
        String normalizada = normalizar(frase);
        if (normalizada.isBlank() || frase.length() > MAX_CARACTERES
                || OPCION_CIFRADA.matcher(frase).find() || OPCION_LITERAL.matcher(frase).find()
                || DESTRIPE.matcher(normalizada).find() || RELLENO.matcher(frase).find()
                || (enOrgano && JERGA_ORGANO.matcher(frase).find())
                || !frasesVistas.add(normalizada)) {
            return;
        }
        String salida = texto.isEmpty() ? frase : " " + frase;
        texto.append(salida);
        frases++;
        emitir.accept(salida);
    }

    private static String normalizar(String frase) {
        return Normalizer.normalize(frase, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
                .replaceAll("[.!?…]+$", "").strip().toLowerCase(Locale.ROOT);
    }
}
