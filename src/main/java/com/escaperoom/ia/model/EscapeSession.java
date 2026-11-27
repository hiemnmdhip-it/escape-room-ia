package com.escaperoom.ia.model;

import com.escaperoom.ia.dto.EstadoPartida;
import com.escaperoom.ia.dto.ResultadoAccion;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class EscapeSession {
    private static final int UMBRAL_PISTA = 2;
    private static final List<PruebaCripta> PRUEBAS = List.of(
            PruebaCripta.FRAGMENTOS, PruebaCripta.MEMORIA, PruebaCripta.INVOCACIONES);
    private static final List<String> RELIQUIAS = List.of("scroll", "stake", "chalice");
    private static final Map<String, String> COMBINACIONES = Map.of(
            "scroll", "altar", "stake", "chandelier", "chalice", "organ");

    private final String id;
    private final CopyOnWriteArrayList<MensajeHistorial> historial = new CopyOnWriteArrayList<>();
    private final Object turno = new Object();
    private final Set<String> inventario = new LinkedHashSet<>(List.of("scroll", "stake"));
    private Idioma idioma;
    private int turnosEstancados;
    private int pistasSolicitadas;
    private int sellosResueltos;
    private int fase = 1;
    private boolean descubrimientoRevelado;
    private String seleccion;

    public EscapeSession(String id, Idioma idioma) {
        this.id = id;
        this.idioma = idioma;
    }

    public String id() {
        return id;
    }

    public <T> T enTurno(Supplier<T> operacion) {
        synchronized (turno) {
            return operacion.get();
        }
    }

    public synchronized EstadoPartida estado() {
        return new EstadoPartida(id, idioma, resultado(), turnosEstancados, sellosResueltos,
                Map.of("altar", estadoSello(1), "organ", estadoSello(2), "chandelier", estadoSello(3),
                        "chest", inventario.contains("chalice") ? "OPEN" : "CLOSED",
                        "scroll", "COLLECTED", "stake", "COLLECTED",
                        "chalice", inventario.contains("chalice") ? "COLLECTED" : "HIDDEN"),
                List.copyOf(inventario), seleccion, null, null, Set.of(),
                Map.of("altar", faseVisible(1), "organ", faseVisible(2), "chandelier", faseVisible(3)),
                RELIQUIAS.stream().filter(reliquia -> !inventario.contains(reliquia)).toList());
    }

    private int faseVisible(int sello) {
        return sellosResueltos >= sello ? 3 : sellosResueltos + 1 == sello ? fase : 1;
    }

    public synchronized void cambiarIdioma(Idioma nuevo) {
        idioma = nuevo;
    }

    public List<MensajeHistorial> historial() {
        return List.copyOf(historial);
    }

    public void anotar(MensajeHistorial mensaje) {
        historial.add(mensaje);
    }

    public synchronized ResultadoAccion inspeccionar(String elemento) {
        if (!terminada() && fase == 1
                && sellosResueltos < 2 && pruebaActual().elemento().equals(elemento)) {
            descubrimientoRevelado = true;
        }
        return observar(elemento);
    }

    public synchronized ResultadoAccion observar(String elemento) {
        String mensaje = switch (elemento) {
            case "room" -> texto("Las velas iluminan un altar con pergaminos, un órgano, un candelabro y un cofre. ",
                    "The candles illuminate an altar with scrolls, an organ, a chandelier and a chest. ")
                    + pasoBreve();
            case "altar" -> describir(PruebaCripta.FRAGMENTOS);
            case "organ" -> describir(PruebaCripta.MEMORIA);
            case "chandelier" -> describir(PruebaCripta.INVOCACIONES);
            case "chest" -> describirCofre();
            case "chalice" -> inventario.contains("chalice")
                    ? texto("La ceniza del cáliz vibra cerca del órgano. Allí puede revelar la partitura.",
                            "The ash in the chalice trembles by the organ. It can reveal the score there.")
                    : texto("Busca el cáliz en el cofre.", "Look for the chalice in the chest.");
            case "scroll" -> texto("El pergamino vibra junto a pasajes de significado parecido. Pruébalo en el altar.",
                    "The scroll hums near passages with similar meanings. Try it on the altar.");
            case "stake" -> texto("La plata vibra cerca del fuego. Úsala en el candelabro para revelar el cerrojo.",
                    "Silver hums near the fire. Use it on the chandelier to reveal the bolt.");
            default -> texto("No veo ese elemento.", "I cannot see that object.");
        };
        return new ResultadoAccion(mensaje, estado(), null, null);
    }

    public synchronized String guiaActual() {
        if (terminada()) {
            return cerrada().mensaje();
        }
        return sellosResueltos == PRUEBAS.size() ? faltaCaliz() : presentarFase(pruebaActual());
    }

    private PruebaCripta pruebaActual() {
        return PRUEBAS.get(sellosResueltos);
    }

    private String describir(PruebaCripta prueba) {
        int indice = PRUEBAS.indexOf(prueba);
        if (indice > sellosResueltos) {
            return bloqueado();
        }
        if (indice < sellosResueltos) {
            return texto("La runa ya está desbloqueada.", "The rune is already unlocked.");
        }
        return presentarFase(prueba);
    }

    private String presentarFase(PruebaCripta prueba) {
        return prueba.presentar(idioma, fase, fase > 1 || descubrimientoRevelado);
    }

    private String describirCofre() {
        return texto("La obsidiana está fría. En el cierre hay un cáliz grabado junto a unos tubos de órgano. ",
                "The obsidian is cold. A chalice and organ pipes are etched into the clasp. ")
                + (inventario.contains("chalice")
                        ? texto("La tapa está abierta. El cáliz está en el inventario.",
                                "The lid is open. The chalice is in your inventory.")
                        : texto("La tapa cede si la levantas. Dentro brilla un cáliz de ceniza.",
                                "The lid gives if you lift it. An ash chalice gleams inside."));
    }

    public synchronized ResultadoAccion recoger(String reliquia) {
        if (terminada()) {
            return cerrada();
        }
        if (inventario.contains(reliquia)) {
            return respuesta(texto("Ya la llevas contigo.", "You already have it."));
        }
        if (!"chalice".equals(reliquia)) {
            return respuesta(texto("Esa reliquia no está disponible.", "That relic is unavailable."));
        }
        inventario.add(reliquia);
        return terminada() ? desenlace(texto("Recuperas el cáliz y cruzas la salida. Has escapado.",
                "You recover the chalice and cross the threshold. You escaped.")) : respuesta(describirCofre());
    }

    public synchronized ResultadoAccion seleccionar(String reliquia) {
        if (terminada()) {
            return cerrada();
        }
        if (!inventario.contains(reliquia)) {
            return respuesta(texto("Esa reliquia no está en tu inventario.", "That relic is not in your inventory."));
        }
        seleccion = reliquia.equals(seleccion) ? null : reliquia;
        return respuesta(seleccion == null ? texto("Selección cancelada.", "Selection cleared.")
                : texto("Reliquia seleccionada.", "Relic selected."));
    }

    public synchronized ResultadoAccion usar(String reliquia, String elemento) {
        if (terminada()) {
            return cerrada();
        }
        int indice = PRUEBAS.stream().map(PruebaCripta::elemento).toList().indexOf(elemento);
        if (!inventario.contains(reliquia)) {
            return rechazarUso(indice,
                    texto("Esa reliquia no está en tu inventario.", "That relic is not in your inventory."));
        }
        seleccion = null;
        if (!elemento.equals(COMBINACIONES.get(reliquia))) {
            return rechazarUso(indice, texto("La reliquia no reacciona aquí.", "The relic does not react here."));
        }
        if (indice > sellosResueltos) {
            return rechazarUso(indice, bloqueado());
        }
        if (indice == sellosResueltos && fase == 1) {
            descubrimientoRevelado = true;
        }
        return respuesta(observar(elemento).mensaje());
    }

    private ResultadoAccion rechazarUso(int indice, String mensaje) {
        if (indice == sellosResueltos) {
            return fallar(mensaje);
        }
        return indice > sellosResueltos
                ? new ResultadoAccion(mensaje, estado(), null, null, PRUEBAS.get(indice).elemento())
                : respuesta(mensaje);
    }

    public synchronized ResultadoAccion resolver(int sello, String intento) {
        if (terminada()) {
            return cerrada();
        }
        if (sello < 1 || sello > PRUEBAS.size()) {
            return respuesta(texto("Ese sello no existe.", "That seal does not exist."));
        }
        if (sello != sellosResueltos + 1) {
            return sello <= sellosResueltos ? respuesta(texto("Ese sello ya está roto.", "That seal is already broken."))
                    : new ResultadoAccion(bloqueado(), estado(), null, null, PRUEBAS.get(sello - 1).elemento());
        }
        if (fase == 1 && !descubrimientoRevelado) {
            return fallar(guiaActual());
        }
        if (intento == null || !pruebaActual().acepta(fase, intento)) {
            return fallar(texto("La runa rechaza la elección. Revisa la inscripción.",
                    "The rune rejects that choice. Read the inscription again."));
        }
        if (fase < 3) {
            avanzarFase();
            return respuesta(guiaActual());
        }
        return romperSello(texto("El sello cede. La piedra tiene mejores modales que antes.",
                "The seal gives way. The stone has better manners now."));
    }

    public synchronized ResultadoAccion solicitarPista() {
        if (terminada()) {
            return cerrada();
        }
        if (sellosResueltos == PRUEBAS.size() || (fase == 1 && !descubrimientoRevelado)) {
            return respuesta(guiaActual());
        }
        String mensaje = pista(pistasSolicitadas);
        pistasSolicitadas = 1;
        return new ResultadoAccion(mensaje, estado(), null, null);
    }

    public synchronized String contextoConversacional() {
        if (terminada()) {
            return texto("La cripta está superada y el diagnóstico pedagógico está disponible.",
                    "The crypt has been cleared and the learning report is available.");
        }
        if (sellosResueltos == PRUEBAS.size()) {
            return texto("La puerta está abierta y queda el cáliz del cofre.",
                    "The gate is open and the chalice in the chest is still missing.");
        }
        String intentos = switch (turnosEstancados) {
            case 0 -> texto("Aún no ha fallado un intento en esta fase.", "No attempt has failed in this phase.");
            case 1 -> texto("Ya hubo un intento sin progreso en esta fase.", "An attempt did not advance this phase.");
            default -> texto("Ya hubo varios intentos sin progreso. Explica el criterio con otra analogía.",
                    "Several attempts did not advance the phase. Explain the criterion with a different analogy.");
        };
        return pruebaActual().contextoConversacional(idioma, fase) + " " + intentos;
    }

    public synchronized String siguientePaso() {
        if (terminada()) {
            return texto("Si te queda curiosidad, abre el diagnóstico de la expedición.",
                    "If you are still curious, open the expedition report.");
        }
        return sellosResueltos == PRUEBAS.size() || (fase == 1 && !descubrimientoRevelado) ? guiaActual() : pista(0);
    }

    public synchronized String pasoBreve() {
        String paso = siguientePaso();
        int fin = paso.indexOf(". ");
        return fin < 0 ? paso : paso.substring(0, fin + 1);
    }

    public synchronized String diagnostico(boolean extenso) {
        return terminada() ? DiagnosticoSpringAi.explicar(idioma, extenso) : null;
    }

    private ResultadoAccion romperSello(String mensaje) {
        sellosResueltos++;
        fase = 1;
        descubrimientoRevelado = false;
        progreso();
        return terminada() ? desenlace(mensaje + texto(" Has escapado.", " You escaped."))
                : respuesta(mensaje + " " + guiaActual());
    }

    private ResultadoAccion fallar(String mensaje) {
        turnosEstancados = Math.min(UMBRAL_PISTA + 1, turnosEstancados + 1);
        return new ResultadoAccion(mensaje, estado(), turnosEstancados == UMBRAL_PISTA
                ? siguientePaso() : null, null, pruebaActual().elemento());
    }

    private void avanzarFase() {
        fase++;
        progreso();
    }

    private void progreso() {
        turnosEstancados = 0;
        pistasSolicitadas = 0;
    }

    private String estadoSello(int sello) {
        if (sellosResueltos >= sello) {
            return "SOLVED";
        }
        return sellosResueltos + 1 == sello && !terminada() ? "READY" : "LOCKED";
    }

    private String resultado() {
        return terminada() ? "VICTORY" : "PLAYING";
    }

    private boolean terminada() {
        return sellosResueltos == PRUEBAS.size() && inventario.containsAll(RELIQUIAS);
    }

    private ResultadoAccion cerrada() {
        return respuesta(texto("La partida ha terminado.", "The game has ended."));
    }

    private String bloqueado() {
        return texto("Primero rompe el sello anterior.", "Break the previous seal first.");
    }

    private String faltaCaliz() {
        return texto("Recoge el cáliz de ceniza en el cofre. La puerta ya está abierta.",
                "Fetch the ash chalice from the chest. The gate is already open.");
    }

    private String texto(String es, String en) {
        return idioma.texto(es, en);
    }

    private String pista(int nivel) {
        String orientacion = pruebaActual().pista(idioma, fase);
        return nivel == 0 ? orientacion : orientacion + " "
                + texto("Descarta lo que contradiga la inscripción.", "Rule out what contradicts the inscription.");
    }

    private ResultadoAccion respuesta(String mensaje) {
        return new ResultadoAccion(mensaje, estado(), null, null);
    }

    private ResultadoAccion desenlace(String mensaje) {
        return new ResultadoAccion(mensaje, estado(), null, diagnostico(false));
    }

}
