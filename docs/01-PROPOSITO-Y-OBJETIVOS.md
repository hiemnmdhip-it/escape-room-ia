# Propósito y objetivos

## Propósito

Escape Room Conversacional de Terror Sobrenatural y Pensamiento Algorítmico con IA es un videojuego web ambientado en la Cripta de Nocturnia. El jugador examina el entorno, recoge reliquias y conversa con su guardián para abrir tres sellos encadenados. La historia convierte en decisiones visibles conceptos que suelen enseñarse mediante ejercicios de código. El jugador completa la aventura por deducción y puede identificar al final qué lógica de Java puso en práctica.

La aplicación aborda dos dificultades frecuentes en el aprendizaje. La primera es reconocer cuándo conviene aplicar un algoritmo o una estructura de datos en una situación nueva. La segunda es explicar una hipótesis, pedir una aclaración y reformular una instrucción en otro idioma. La partida ofrece un contexto común para practicar ambas habilidades y observar las consecuencias de cada decisión.

## Público objetivo

El público principal son estudiantes del ciclo superior de Desarrollo de Aplicaciones Web y personas que se inician en Java y en el desarrollo web. Los dos primeros sellos practican contenidos de nivel DAW. El tercero introduce el razonamiento sobre transiciones y ciclos propio de un flujo de aplicación. El docente puede usar la partida como actividad de introducción o como ejercicio de reflexión sobre decisiones ya estudiadas.

## Objetivos pedagógicos

1. Acotar una búsqueda ordenada con la respuesta de cada calibración. El jugador aprende a reducir el intervalo, valorar el punto medio y relacionar el número de consultas con el crecimiento logarítmico.
2. Reconstruir el orden de activación de unas runas y desactivarlas en orden inverso. La mecánica representa el comportamiento LIFO de una pila en Java, como un `Deque` implementado mediante `ArrayDeque`.
3. Distinguir los estados de una compuerta de las acciones que provocan transiciones. El jugador elige pasos válidos y recuerda las cámaras visitadas para reconocer un ciclo.
4. Practicar la comprensión y la formulación de preguntas, hipótesis e instrucciones en español o inglés. Preguntar por un término o símbolo forma parte del juego y conserva los intentos.
5. Explicar las decisiones tras la victoria o la derrota. El diagnóstico breve vincula cada sello con Java, su coste o su ventaja y un uso web. El diagnóstico ampliado desarrolla los ejemplos y los casos límite cuando el jugador lo solicita.

## Alcance funcional

El alcance del producto incluye una escena de cripta interactiva, un inventario seleccionable, un chat narrativo bilingüe y un registro consultable y descargable. Cada elemento de la escena comunica visualmente su estado. Las acciones realizadas en la escena o a través del chat se resuelven con las reglas del mismo motor. La partida dispone de vitalidad y de un contador de acciones sin progreso. Cuando alcanza el umbral previsto, el guardián ofrece una pista que sugiere una estrategia y preserva la deducción.

La Fase 1 establece la configuración de proveedores, el modelo en memoria, las tres pruebas y las herramientas de chat validadas en Java. La API entrega el estado visual en JSON y transmite la conversación por SSE. La fase de interfaz conectará esa API con la presentación gráfica y la bitácora descargable.

La sesión vive en la memoria de la instancia durante la partida. El historial completo permite recuperar el relato de la partida. La ventana de memoria conversacional conserva los mensajes recientes necesarios para mantener el contexto del diálogo.
