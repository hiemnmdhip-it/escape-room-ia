# Descripción y Justificación del Proyecto

**Título:** Videojuego Web de Escape Room Conversacional de Terror Sobrenatural y Pensamiento Algorítmico con IA (*Cripta de Nocturnia*)

## 1. Concepto y Propósito

El proyecto consiste en el desarrollo de un **videojuego web interactivo tipo Escape Room** ambientado en una estética gótica neón de terror psicológico, Halloween, vampiros y demonios. El jugador debe escapar de una cripta sellada resolviendo enigmas físicos y mecanismos sobrenaturales que representan problemas reales de lógica, estructuras de datos y algoritmos de programación en Java alineados con el **Grado Superior en Desarrollo de Aplicaciones Web (DAW)** y un nivel técnico intermedio, **sin escribir código**: todo se resuelve interactuando dinámicamente con los elementos visuales de la escena o escribiendo acciones en lenguaje natural a través de un chat integrado.

El propósito de la plataforma abarca tres vertientes principales:

1. **Entrenamiento en lógica y pensamiento algorítmico (Nivel DAW y Avanzado en Java):** Cada sello demoníaco encubre un problema real de programación en Java (búsqueda binaria `O(log n)`, pilas `Deque` LIFO, tablas `HashMap` de frecuencias, máquinas de estados finitos y recorrido de grafos sin ciclos), planteado como un mecanismo físico coherente dentro de la cripta.
2. **Retroalimentación pedagógica al finalizar la partida:** Al romper todos los sellos (o agotar la vitalidad/intentos), el agente revela qué estructuras de datos, algoritmos o patrones de diseño en Java se resolvieron durante la partida, por qué son más eficientes que una solución ingenua y cómo aplicarlos en una aplicación web real. Por defecto entrega una **explicación resumida**, permitiendo alternar a una **versión extensa** bajo demanda.
3. **Interacción Dual (Escenario Visual + Lenguaje Natural):** Demuestra cómo combinar una escena gráfica interactiva con un agente de IA que actúa simultáneamente como árbitro estricto de estado y narrador en tiempo real.

## 2. Ambientación, Estética y Personalidad del Agente

- **Atmósfera y Estética Visual:** Gótico neón oscuro (Halloween, criptas vampíricas, energía demoníaca). Paleta basada en negro abismo (`#09070D`), piedra oscura (`#130F1D`), **naranja neón infernal (`#FF5E00`)**, carmesí vampírico (`#D90429`) y púrpura arcano (`#8338EC`).
- **Personalidad del Chat:** Coherente, pragmático, ligeramente irónico pero siempre orientativo y amable.
- **Estilo de Comunicación:** **Frases cortas, directas y simples.** Sin párrafos largos ni relleno.
- **Consultas de Significado:** Si el jugador pregunta qué significa un símbolo, término o pista dentro del contexto del juego, el agente lo aclara de forma coherente con la historia sin gastar intentos.
- **Sistema de Pistas Adaptativo:** Detecta automáticamente cuando el jugador repite intentos fallidos o lleva varios turnos sin avanzar para ofrecer pistas sutiles e irónicas sin arruinar la deducción.

## 3. Funcionamiento General (Interacción Bidireccional)

La pantalla de juego (Spring MVC + Thymeleaf + JavaScript ES6+) se divide en dos paneles sincronizados en tiempo real:

- **Panel Izquierdo — Escenario Interactivo del Videojuego:** Muestra la escena visual de la estancia con elementos clicables (altares, cofres, candelabros, sellos) que reaccionan con animaciones, destellos neón y cambios de estado al pulsarlos, junto con un inventario inferior de reliquias seleccionables para combinarlas con el entorno.
- **Panel Derecho — Chat Narrativo Conversacional:**
  - *De Escena a Chat:* Hacer clic en un objeto del escenario o usar una reliquia dispara automáticamente la acción en el backend y el narrador describe la reacción en tiempo real.
  - *De Chat a Escena:* Escribir una orden en texto libre (*"examino el cofre de obsidiana"*, *"uso la estaca de plata en el altar"*) ejecuta las `@Tool` en Java y actualiza visualmente los objetos, el inventario y los sellos en el panel izquierdo.
  - Conserva todo el historial de la partida en memoria para visualizarlo en cualquier momento y descargarlo como bitácora (`.txt`).

## 4. Arquitectura y Decisiones Tecnológicas (Arquitectura MVC)

Para garantizar baja latencia y una distribución autónoma sencilla (sin Docker ni bases de datos externas), se implementa una arquitectura MVC *stateful* en memoria:

### 4.1. Backend y Patrón MVC

- **Java 21 + Spring Boot 4.1.x + Maven (YAML):** Estructurado estrictamente por capas MVC (`config`, `controller`, `service`, `tools`, `model`, `dto`, `exception`).
- **Controladores:**
  - `VistaJuegoController` (`@Controller`): Renderiza las vistas Thymeleaf principales (`index.html`, `juego.html`).
  - `PartidaApiController` (`@RestController`): Gestiona creación/reinicio de partidas, ejecución de acciones desde escenario o chat mediante streaming SSE, consulta de estado visual, historial en memoria e informe algorítmico final.

### 4.2. Orquestación de IA y Tool Calling

- **Spring AI + `@Tool` (Function Calling):** Controla las acciones físicas de la cripta (`inspeccionarElemento`, `recogerReliquia`, `usarReliquia`, `resolverSello`, `solicitarPista`) validando la coherencia lógica en Java para evitar alucinaciones.
- **Inferencia local con Ollama.** Spring AI usa el modelo `qwen3:8b` en `127.0.0.1:11434`. La configuración se carga a través del perfil `ollama`.

### 4.3. Streaming, Estado e Historial en Memoria

- **Streaming en Tiempo Real:** SSE (`SseEmitter` o `Flux<String>`) consumido desde JavaScript ES6+ mediante `Fetch API` (`ReadableStream`).
- **Estado Concurrente (`ConcurrentHashMap<String, EscapeSession>`):** Almacena por `UUID` la sala actual, estado visual de cada elemento interactivo, progreso de los 3 sellos algorítmicos, inventario de reliquias, intentos/vitalidad restante, contador de turnos estancados e historial cronológico (`CopyOnWriteArrayList<MensajeHistorial>`), sincronizado con `MessageWindowChatMemory`.
- **Frontend:** Thymeleaf + HTML5 + CSS3 (estética gótica neón basada en el prototipo de referencia) + JavaScript ES6+ Vanilla.
- **Entrega Autónoma:** JAR ejecutable (`mvn clean package`), script `iniciar.bat` para Windows, `.zip` de entrega y repositorio Git limpio.
