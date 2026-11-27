package com.escaperoom.ia.model;

import java.util.List;
import java.util.Locale;

enum PruebaCripta {
    FRAGMENTOS("altar",
            new Fase("B",
                    "El altar pide un pasaje sobre el reposo de los muertos. Un pergamino dice «reposo» y habla de un viajero cansado. Otro habla del sueño eterno. ¿Cómo buscarías? A — por la palabra exacta | B — por el sentido del pasaje | C — por la posición del pergamino.",
                    "The altar asks for a passage about the rest of the dead. One scroll says ‘rest’ and speaks of a weary traveler. Another speaks of eternal sleep. How would you search? A — by exact word | B — by the passage's meaning | C — by scroll position.",
                    "El altar pide una idea completa. Una palabra repetida puede pertenecer a otra historia.",
                    "The altar asks for a whole idea. A repeated word may belong to a different story."),
            new Fase("2",
                    "La profecía ocupa 80 palabras. Por similitud coseno, el ruido llega a 0,65 y el pasaje útil a 0,86. Elige tamaño y umbral entre 1 — 40 palabras / 0,75 | 2 — 100 palabras / 0,75 | 3 — 100 palabras / 0,90.",
                    "The prophecy takes 80 words. Cosine similarity reaches 0.65 for noise and 0.86 for the useful passage. Pick size and threshold from 1 — 40 words / 0.75 | 2 — 100 words / 0.75 | 3 — 100 words / 0.90.",
                    "El fragmento debe contener la profecía entera. El umbral debe quedar entre el ruido y el pasaje.",
                    "The chunk must fit the entire prophecy. The threshold belongs between noise and the useful passage."),
            new Fase("C",
                    "El núcleo pide devolver la luz a quien la perdió. Elige el concepto más cercano entre A — luz de luna, 0,77 | B — candelabro de oro, 0,79 | C — llama que vuelve a encenderse, 0,86.",
                    "The core asks for light to be restored to whoever lost it. Pick the closest concept from A — moonlight, 0.77 | B — golden chandelier, 0.79 | C — a flame rekindled, 0.86.",
                    "Compara el significado completo y la puntuación. Una palabra idéntica no gana por sí sola.",
                    "Compare the whole meaning and its score. An exact word alone earns nothing.")),
    MEMORIA("organ",
            new Fase("B",
                    "Cada invocación deja un eco en los tubos. Al soplar de nuevo, el órgano recuerda las voces recientes, pero ya no oye los cantos más antiguos. El canto grabado en piedra sigue entero. ¿Por qué se pierden los otros? A — la piedra borra todas las voces | B — los tubos tienen sitio para un número limitado de ecos | C — el órgano solo recuerda las voces más fuertes.",
                    "Each invocation leaves an echo in the pipes. When the organ breathes again, it remembers recent voices but cannot hear the oldest songs. The song carved in stone remains whole. Why do the others fade? A — the stone erases all voices | B — the pipes can hold only so many echoes | C — the organ remembers only the loudest voices.",
                    "Fíjate en qué cantos se desvanecen al llegar nuevas voces. La piedra permanece intacta.",
                    "Notice which songs fade when new voices arrive. The stone remains untouched."),
            new Fase("3",
                    "El fuelle solo retiene 120 suspiros antes de ahogarse. El acorde final exige 40 y el canto grabado en piedra ocupa 20. Cada eco de una invocación consume otros 20. ¿Cuál es el máximo de ecos recientes que puedes guardar junto al canto? 1 — dos | 2 — cuatro | 3 — tres.",
                    "The bellows hold only 120 breaths before choking. The final chord needs 40 and the song carved in stone takes 20. Each invocation echo uses another 20. What is the greatest number of recent echoes you can keep alongside the song? 1 — two | 2 — four | 3 — three.",
                    "Aparta el aliento del acorde y del canto de piedra. Luego cuenta los ecos que caben.",
                    "Set aside breath for the chord and the stone song. Then count the echoes that fit."),
            new Fase("A",
                    "Los tubos guardan un eco de la entidad del ritual anterior, el canto de piedra y las voces que acaban de afinar el acorde. El fuelle se ahoga. ¿Qué dejas salir sin perder la melodía? A — el eco viejo | B — el canto grabado en piedra | C — un eco reciente.",
                    "The pipes hold an echo of the entity from the previous ritual, the stone song, and the voices that just tuned the chord. The bellows are choking. What do you let out without losing the melody? A — the old echo | B — the song carved in stone | C — a recent echo.",
                    "Escucha qué voz ya no sostiene el acorde. La inscripción de piedra lo guía.",
                    "Listen for the voice that no longer supports the chord. The stone inscription guides it.")),
    INVOCACIONES("chandelier",
            new Fase("C",
                    "La voz pronunció «abierto». El cerrojo sigue encajado. ¿Qué hará ceder la compuerta? A — repetir el decreto | B — dar por abierta la compuerta | C — accionar el mecanismo que mueve el cerrojo.",
                    "The voice said ‘open’. The bolt is still in place. What will make the gate give way? A — repeat the decree | B — assume the gate is open | C — operate the mechanism that moves the bolt.",
                    "Compara lo que la voz afirma con lo que ha cambiado en el cerrojo.",
                    "Compare what the voice claims with what has changed at the bolt."),
            new Fase("1",
                    "Solo una herramienta registrada mueve el cerrojo. La inscripción pide «mover cerrojo», cámara norte y un giro. Elige 1 — mover cerrojo / norte / un giro | 2 — abrir por decreto / norte / un giro | 3 — mover cerrojo / sur / dos giros.",
                    "Only a registered tool moves the bolt. The inscription asks for ‘move bolt’, north chamber and one turn. Pick 1 — move bolt / north / one turn | 2 — open by decree / north / one turn | 3 — move bolt / south / two turns.",
                    "Contrasta el nombre, la cámara y los giros con la inscripción del mecanismo.",
                    "Check the name, chamber and turns against the mechanism's inscription."),
            new Fase("B",
                    "El cerrojo se movió y la compuerta exige un veredicto verificable. Elige A — la frase libre «abierto» | B — un molde con cámara de texto y cerrojo sí/no, comprobados contra el mecanismo | C — un molde que admita cualquier campo sin comprobarlo.",
                    "The bolt moved and the gate demands a verifiable verdict. Pick A — the free-form word ‘open’ | B — a mold with chamber text and bolt yes/no, checked against the mechanism | C — a mold that accepts any field without checking.",
                    "El molde necesita campos y tipos concretos. El mecanismo confirma que sean ciertos.",
                    "The mold needs specific fields and types. The mechanism confirms they are true."));

    private final String elemento;
    private final List<Fase> fases;

    PruebaCripta(String elemento, Fase descubrimiento, Fase ajuste, Fase cierre) {
        this.elemento = elemento;
        this.fases = List.of(descubrimiento, ajuste, cierre);
    }

    String elemento() {
        return elemento;
    }

    String presentar(Idioma idioma, int fase, boolean revelada) {
        String titulo = idioma.texto("Fase %d/3 — ", "Phase %d/3 — ").formatted(fase);
        Fase activa = fases.get(fase - 1);
        return titulo + (revelada ? idioma.texto(activa.textoEs(), activa.textoEn()) : descubrir(idioma));
    }

    private String descubrir(Idioma idioma) {
        return switch (this) {
            case FRAGMENTOS -> idioma.texto("Examina el Altar de los Fragmentos. Hay pergaminos dispersos sobre la piedra.",
                    "Inspect the Altar of Fragments. Scrolls are scattered over the stone.");
            case MEMORIA -> idioma.texto("Examina el Órgano de la Memoria Efímera. Jura que nunca te ha visto.",
                    "Inspect the Organ of Fleeting Memory. It swears it has never seen you.");
            case INVOCACIONES -> idioma.texto("Aplica la estaca de plata al candelabro. Su luz revela el mecanismo de la Compuerta de las Invocaciones.",
                    "Use the silver stake on the chandelier. Its light reveals the Invocation Gate's mechanism.");
        };
    }

    boolean acepta(int fase, String intento) {
        return fases.get(fase - 1).opcion().equals(intento.strip().toUpperCase(Locale.ROOT));
    }

    String pista(Idioma idioma, int fase) {
        Fase activa = fases.get(fase - 1);
        return idioma.texto(activa.pistaEs(), activa.pistaEn());
    }

    String contextoConversacional(Idioma idioma, int fase) {
        if (fase == 1) {
            return switch (this) {
                case FRAGMENTOS -> idioma.texto("El altar compara lo que cuentan los pergaminos. Una palabra puede repetirse en historias distintas.",
                        "The altar compares what the scrolls say. A word can recur in different stories.");
                case MEMORIA -> idioma.texto("Al llegar nuevas voces, el órgano apenas oye las antiguas. El canto de piedra sigue entero. Usa el lenguaje de la cripta al explicarlo.",
                        "As new voices arrive, the organ barely hears the old ones. The stone song remains whole. Explain it in the language of the crypt.");
                case INVOCACIONES -> idioma.texto("Una voz declara abierta la compuerta. El cerrojo sigue encajado. El jugador debe distinguir lo que se dice de lo que cambia en la sala.",
                        "A voice declares the gate open. The bolt is still in place. The player must distinguish what is said from what changes in the room.");
            };
        }
        return switch (this) {
            case FRAGMENTOS -> fase == 2
                    ? idioma.texto("El altar busca recuperar una profecía completa entre pasajes irrelevantes. Conceptos: tamaño del fragmento frente a longitud del texto, similitud semántica y umbral frente al ruido. Un fragmento más corto que lo que se quiere recuperar puede partirlo. El umbral rechaza el ruido y debe dejar pasar el pasaje útil.",
                            "The altar must retrieve a complete prophecy among irrelevant passages. Concepts: chunk size versus text length, semantic similarity and threshold versus noise. A chunk shorter than the target passage may split it. The threshold must reject noise and still admit the useful passage.")
                    : idioma.texto("El altar busca un pasaje que conserve la intención del núcleo. Conceptos: significado completo, coincidencia literal y puntuación de similitud.",
                            "The altar seeks a passage that preserves the core's intent. Concepts: whole meaning, literal matches and similarity scores.");
            case MEMORIA -> fase == 2
                    ? idioma.texto("El fuelle tiene un aliento limitado. El acorde final y el canto grabado en piedra comparten ese aliento con los ecos. Usa el lenguaje de la cripta al explicarlo.",
                            "The bellows have limited breath. The final chord and the song carved in stone share that breath with the echoes. Explain it in the language of the crypt.")
                    : idioma.texto("Los tubos guardan voces de distintos rituales. El canto de piedra guía el acorde. Explica cómo distinguir las voces que aún ayudan, usando el lenguaje de la cripta.",
                            "The pipes hold voices from different rituals. The stone song guides the chord. Explain how to tell which voices still help, using the language of the crypt.");
            case INVOCACIONES -> fase == 2
                    ? idioma.texto("La compuerta requiere ejecutar una acción registrada. Conceptos: nombre de herramienta, argumentos y ejecución verificada por Java.",
                            "The gate requires a registered action. Concepts: tool name, arguments and execution verified by Java.")
                    : idioma.texto("La compuerta exige comprobar el resultado de una acción. Conceptos: campos y tipos de una salida estructurada, y verificación contra el estado real.",
                            "The gate must verify an action's result. Concepts: structured output fields and types, and verification against actual state.");
        };
    }

    private record Fase(String opcion, String textoEs, String textoEn, String pistaEs, String pistaEn) {}
}
