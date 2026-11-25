package com.escaperoom.ia.model;

import com.escaperoom.ia.dto.EstadoPartida;
import com.escaperoom.ia.dto.ResultadoAccion;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class EscapeSession {
    private static final int PESO_MINIMO = 1;
    public static final int PESO_MAXIMO = 100;
    private static final int INTENTOS_CALIBRACION = Integer.SIZE - Integer.numberOfLeadingZeros(PESO_MAXIMO);
    private static final int UMBRAL_PISTA = 2;
    private static final List<String> ACTIVACION_RUNAS = List.of("A", "Z", "R", "A");
    private static final Map<String, String> COMBINACIONES = Map.of(
            "scroll", "altar", "stake", "chandelier", "chalice", "organ");

    private final String id;
    private final int pesoObjetivo;
    private final CopyOnWriteArrayList<MensajeHistorial> historial = new CopyOnWriteArrayList<>();
    private final Object turno = new Object();
    private final Set<String> inventario = new LinkedHashSet<>(List.of("scroll", "stake"));
    private final Deque<String> runas = new ArrayDeque<>();
    private final Set<Camara> visitadas = EnumSet.of(Camara.A);
    private Idioma idioma;
    private int vitalidad = 10;
    private int turnosEstancados;
    private int sellosResueltos;
    private int minimo = PESO_MINIMO;
    private int maximo = PESO_MAXIMO;
    private int calibraciones = INTENTOS_CALIBRACION;
    private String seleccion;
    private Camara camara = Camara.A;

    public EscapeSession(String id, Idioma idioma, int pesoObjetivo) {
        if (pesoObjetivo < PESO_MINIMO || pesoObjetivo > PESO_MAXIMO) {
            throw new IllegalArgumentException("Peso fuera del intervalo inicial");
        }
        this.id = id;
        this.idioma = idioma;
        this.pesoObjetivo = pesoObjetivo;
        ACTIVACION_RUNAS.forEach(runas::push);
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
        return new EstadoPartida(id, idioma, resultado(), vitalidad, turnosEstancados, sellosResueltos,
                Map.of("altar", estadoSello(1), "organ", estadoSello(2), "chandelier", estadoSello(3),
                        "chest", inventario.contains("chalice") ? "OPEN" : "CLOSED",
                        "scroll", "COLLECTED", "stake", "COLLECTED",
                        "chalice", inventario.contains("chalice") ? "COLLECTED" : "HIDDEN"),
                List.copyOf(inventario), seleccion, new EstadoPartida.Calibracion(minimo, maximo, calibraciones),
                sellosResueltos >= 2 ? camara.name() : null,
                sellosResueltos >= 2 ? visitadas.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()) : Set.of());
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
        String mensaje = switch (elemento) {
            case "altar" -> sellosResueltos >= 1 ? texto("La balanza duerme.", "The scales are quiet.")
                    : texto("Calibra un peso entre %d y %d. Quedan %d lecturas.",
                            "Calibrate a weight between %d and %d. %d readings left.").formatted(minimo, maximo, calibraciones);
            case "organ" -> sellosResueltos == 0 ? bloqueado() : sellosResueltos >= 2
                    ? texto("El órgano guarda silencio.", "The organ is silent.")
                    : texto("Las runas se encendieron así: A, Z, R, A. Apágalas una a una.",
                            "The runes lit up like this: A, Z, R, A. Put them out one by one.");
            case "chandelier" -> sellosResueltos < 2 ? bloqueado() : sellosResueltos == 3
                    ? texto("La ruta está abierta.", "The route is open.")
                    : texto("Estás en %s. Llega a F sin repetir cámara. Salidas desde aquí: %s.",
                            "You are in %s. Reach F without revisiting a chamber. Exits from here: %s.")
                            .formatted(camara, camara.salidas());
            case "chest", "chalice" -> inventario.contains("chalice")
                    ? texto("El cáliz está en el inventario.", "The chalice is in your inventory.")
                    : texto("El cofre contiene un cáliz de ceniza.", "The chest holds an ash chalice.");
            case "scroll" -> texto("El pergamino reacciona al altar.", "The scroll reacts to the altar.");
            case "stake" -> texto("La estaca vibra cerca del candelabro.", "The stake vibrates near the chandelier.");
            default -> texto("No veo ese elemento.", "I cannot see that object.");
        };
        return respuesta(mensaje);
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
        progreso();
        return respuesta(texto("Cofre abierto. Cáliz de ceniza recogido.", "Chest opened. Ash chalice collected."));
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
        if (!inventario.contains(reliquia)) {
            return respuesta(texto("Esa reliquia no está en tu inventario.", "That relic is not in your inventory."));
        }
        seleccion = null;
        if (!elemento.equals(COMBINACIONES.get(reliquia))) {
            return respuesta(texto("La reliquia no reacciona aquí.", "The relic does not react here."));
        }
        return inspeccionar(elemento);
    }

    public synchronized ResultadoAccion resolver(int sello, String intento) {
        if (terminada()) {
            return cerrada();
        }
        if (sello < 1 || sello > 3) {
            return respuesta(texto("Ese sello no existe.", "That seal does not exist."));
        }
        if (sello != sellosResueltos + 1) {
            return respuesta(sello <= sellosResueltos
                    ? texto("Ese sello ya está roto.", "That seal is already broken.") : bloqueado());
        }
        return switch (sello) {
            case 1 -> calibrar(intento);
            case 2 -> apagarRuna(intento);
            default -> cruzar(intento);
        };
    }

    public synchronized ResultadoAccion solicitarPista() {
        return terminada() ? cerrada() : respuesta(pista());
    }

    public synchronized String diagnostico(boolean extenso) {
        return terminada() ? DiagnosticoJava.explicar(idioma, extenso) : null;
    }

    private ResultadoAccion calibrar(String intento) {
        int peso;
        try {
            peso = Integer.parseInt(intento);
        } catch (NumberFormatException e) {
            return respuesta(texto("Indica un peso entero.", "Enter a whole-number weight."));
        }
        if (peso < PESO_MINIMO || peso > PESO_MAXIMO) {
            return respuesta(texto("El peso debe estar entre 1 y 100.", "The weight must be between 1 and 100."));
        }
        calibraciones--;
        if (peso == pesoObjetivo) {
            return romperSello(texto("La balanza se equilibra. El órgano despierta.", "The scales balance. The organ awakens."));
        }
        int anteriorMinimo = minimo;
        int anteriorMaximo = maximo;
        if (peso < pesoObjetivo) {
            minimo = Math.max(minimo, peso + 1);
        } else {
            maximo = Math.min(maximo, peso - 1);
        }
        if (minimo != anteriorMinimo || maximo != anteriorMaximo) {
            progreso();
        } else {
            turnosEstancados++;
        }
        vitalidad--;
        if (calibraciones == 0) {
            return desenlace(texto("La balanza ha agotado sus lecturas.", "The scales have run out of readings."));
        }
        return respuesta(peso < pesoObjetivo ? texto("Demasiado ligero.", "Too light.")
                : texto("Demasiado pesado.", "Too heavy."));
    }

    private ResultadoAccion apagarRuna(String intento) {
        if (!ACTIVACION_RUNAS.contains(intento)) {
            return respuesta(texto("Solo hay runas A, Z y R.", "The runes are A, Z and R."));
        }
        if (!intento.equals(runas.peek())) {
            return fallar(texto("Esa runa sigue atrapada bajo otra.", "That rune is still trapped under another."));
        }
        runas.pop();
        progreso();
        return runas.isEmpty() ? romperSello(texto("El órgano calla. El candelabro ilumina la ruta.",
                "The organ falls silent. The chandelier lights the way."))
                : respuesta(texto("Una runa se apaga.", "One rune goes out."));
    }

    private ResultadoAccion cruzar(String intento) {
        Camara destino;
        try {
            destino = Camara.valueOf(intento);
        } catch (IllegalArgumentException e) {
            return respuesta(texto("Indica una cámara de la A a la F.", "Enter a chamber from A to F."));
        }
        if (visitadas.contains(destino) || !camara.salidas().contains(destino)) {
            return fallar(texto("La cripta rechaza esa compuerta. Sigues aquí.",
                    "The crypt rejects that gate. You stay here."));
        }
        camara = destino;
        visitadas.add(destino);
        progreso();
        return destino == Camara.F ? romperSello(texto("La salida cede. Has escapado.", "The exit gives way. You escaped."))
                : inspeccionar("chandelier");
    }

    private ResultadoAccion romperSello(String mensaje) {
        sellosResueltos++;
        progreso();
        return terminada() ? desenlace(mensaje) : respuesta(mensaje);
    }

    private ResultadoAccion fallar(String mensaje) {
        vitalidad--;
        turnosEstancados++;
        return terminada() ? desenlace(mensaje) : respuesta(mensaje);
    }

    private void progreso() {
        turnosEstancados = 0;
    }

    private String estadoSello(int sello) {
        if (sellosResueltos >= sello) {
            return "SOLVED";
        }
        return sellosResueltos + 1 == sello && !terminada() ? "READY" : "LOCKED";
    }

    private String resultado() {
        if (sellosResueltos == 3) {
            return "VICTORY";
        }
        return vitalidad == 0 || (sellosResueltos == 0 && calibraciones == 0) ? "DEFEAT" : "PLAYING";
    }

    private boolean terminada() {
        return !resultado().equals("PLAYING");
    }

    private ResultadoAccion cerrada() {
        return respuesta(texto("La partida ha terminado.", "The game has ended."));
    }

    private String bloqueado() {
        return texto("Primero rompe el sello anterior.", "Break the previous seal first.");
    }

    private String texto(String es, String en) {
        return idioma.texto(es, en);
    }

    private String pista() {
        return switch (sellosResueltos) {
            case 0 -> texto("Prueba en el centro. La balanza odia que cuentes de uno en uno.",
                    "Try the middle. The scales hate counting one by one.");
            case 1 -> texto("Piensa en platos apilados. Retira el de arriba.", "Think of stacked plates. Take the top one.");
            default -> texto("Recuerda por dónde pasaste. Las compuertas no aprecian las segundas visitas.",
                    "Remember where you have been. Gates dislike second visits.");
        };
    }

    private ResultadoAccion respuesta(String mensaje) {
        return new ResultadoAccion(mensaje, estado(),
                !terminada() && turnosEstancados >= UMBRAL_PISTA ? pista() : null, null);
    }

    private ResultadoAccion desenlace(String mensaje) {
        return new ResultadoAccion(mensaje, estado(), null, diagnostico(false));
    }

    private enum Camara {
        A, B, C, D, E, F;

        List<Camara> salidas() {
            return switch (this) {
                case A -> List.of(B, C);
                case B -> List.of(A, D);
                case C -> List.of(A, E);
                case D -> List.of(B, F);
                case E -> List.of(C, D);
                case F -> List.of();
            };
        }
    }
}
