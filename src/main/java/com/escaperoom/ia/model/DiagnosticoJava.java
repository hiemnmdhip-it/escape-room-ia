package com.escaperoom.ia.model;

public final class DiagnosticoJava {
    private DiagnosticoJava() {}

    public static String explicar(Idioma idioma, boolean extenso) {
        String resumen = idioma.texto("""
                1. Búsqueda binaria: O(log n) frente a O(n). Sirve para buscar IDs en una lista ordenada.
                2. Pila LIFO: ArrayDeque retira la última entrada en O(1) amortizado. Sirve para deshacer cambios en un editor web.
                3. Máquina de estados: enum limita las transiciones y Set registra visitas. Sirve para validar un flujo de pedidos.
                Escribe /diagnostico para ver el análisis extenso.
                """, """
                1. Binary search: O(log n) versus O(n). It finds IDs in a sorted list.
                2. LIFO stack: ArrayDeque removes the last entry in amortized O(1). It can undo edits in a web editor.
                3. State machine: an enum limits transitions and a Set tracks visits. It can validate an order workflow.
                Type /diagnostico to see the full analysis.
                """).strip();
        if (!extenso) {
            return resumen;
        }
        return idioma.texto("""
                Sello I. Tras comparar el punto medio, descarta el lado imposible. Hay hasta siete consultas entre 100 pesos.
                Un escaneo lineal puede probar los 100. En Java, Arrays.binarySearch localiza un ID en un índice ordenado en memoria.
                Sello II. La última runa activada se apaga primero. ArrayDeque modela la pila con push, peek y pop.
                Las operaciones en el extremo son O(1) amortizado. Un editor web puede apilar comandos reversibles por sesión.
                Sello III. Cada cámara es un valor enum con salidas permitidas. Validar una compuerta de este mapa acotado cuesta O(1).
                Un conjunto de visitados bloquea reentradas. En un backend, el servidor puede validar así el avance de un pedido y evitar repetir pasos.
                """, """
                Seal I. Compare the midpoint and discard the impossible half. There are at most seven reads among 100 weights.
                A linear scan can check all 100. In Java, Arrays.binarySearch finds an ID in a sorted in-memory index.
                Seal II. The last rune activated is the first to go out. ArrayDeque models the stack with push, peek and pop.
                End operations are amortized O(1). A web editor can stack reversible commands per session.
                Seal III. Each chamber is an enum value with allowed exits. Checking an edge in this fixed map takes O(1).
                A visited set blocks re-entry. A backend can use this approach to validate order progress and prevent repeated steps.
                """).strip();
    }
}
