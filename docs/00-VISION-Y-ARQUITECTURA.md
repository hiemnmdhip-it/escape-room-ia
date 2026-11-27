# Descripción y Justificación del Proyecto

**Título:** Videojuego Web de Escape Room Conversacional de Terror Sobrenatural y Pensamiento Algorítmico con IA (*Cripta de Nocturnia*)

## 1. Concepto y Propósito

Cripta de Nocturnia es un escape room web de estética gótica neón. Sus acertijos introducen conceptos de Spring AI a un público de Desarrollo de Aplicaciones Web (DAW). El jugador deduce las respuestas e interactúa con la escena o el chat sin escribir código.

El propósito de la plataforma abarca tres vertientes principales:

1. **Comprensión de Spring AI.** El altar representa la recuperación por significado, el órgano introduce el presupuesto de contexto y la compuerta ilustra la ejecución y comprobación de herramientas. Cada sello exige superar tres fases en orden.
2. **Retroalimentación pedagógica.** Después de desbloquear las tres runas y reunir todas las reliquias, se abre un diagnóstico resumido que relaciona los acertijos con Spring AI. La versión extensa ofrece ejemplos para aplicaciones web.
3. **Interacción entre escena y chat.** Java arbitra la partida y Ollama interpreta órdenes de escena y genera las aclaraciones. Las respuestas del chat llegan progresivamente.

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
  - *De Chat a Escena.* Una orden explícita puede invocar una herramienta, como al escribir «examino el cofre de obsidiana». Java comprueba los argumentos y publica el resultado. Las preguntas se tratan como conversación.
  - Conserva todo el historial de la partida en memoria para visualizarlo en cualquier momento y descargarlo como bitácora (`.txt`).

## 4. Arquitectura y Decisiones Tecnológicas (Arquitectura MVC)

Para garantizar baja latencia y una distribución autónoma sencilla (sin Docker ni bases de datos externas), se implementa una arquitectura MVC *stateful* en memoria:

### 4.1. Backend y Patrón MVC

- **Java 21 + Spring Boot 4.1.x + Maven (YAML):** Estructurado estrictamente por capas MVC (`config`, `controller`, `service`, `tools`, `model`, `dto`, `exception`).
- **Controladores:**
  - `VistaJuegoController` (`@Controller`) renderiza `inicio.html` y `juego.html` con Thymeleaf.
  - `PartidaApiController` (`@RestController`): Gestiona creación/reinicio de partidas, ejecución de acciones desde escenario o chat mediante streaming SSE, consulta de estado visual, historial en memoria e informe algorítmico final.

### 4.2. Orquestación de IA y Tool Calling

- **Spring AI y herramientas.** Una orden aceptada puede ofrecer al modelo `inspeccionarElemento`, `recogerReliquia` y `usarReliquia`. Java valida las acciones y procesa directamente las elecciones de los sellos y el botón de pista.
- **Inferencia local con Ollama.** Spring AI usa el modelo `qwen3:8b` en `127.0.0.1:11434`. La configuración se carga a través del perfil `ollama`.

### 4.3. Streaming, Estado e Historial en Memoria

- **Streaming.** Spring MVC publica con `SseEmitter` el texto progresivo y el estado devuelto por Java. La conversación usa el flujo de Spring AI y las respuestas del motor se transmiten en fragmentos. JavaScript consume ambos mediante Fetch.
- **Estado concurrente.** Cada `EscapeSession` se indexa por UUID y conserva idioma, progreso, inventario, intentos sin avance e historial. La bitácora usa `CopyOnWriteArrayList<MensajeHistorial>` y se registra también en una ventana Spring AI de 20 mensajes.
- **Frontend:** Thymeleaf + HTML5 + CSS3 (estética gótica neón basada en el prototipo de referencia) + JavaScript ES6+ Vanilla.
- **Entrega local.** La carpeta de entrega contiene el JAR ejecutable, el lanzador de Windows, el README y la documentación técnica. El lanzador abre `http://localhost:8080/`. El arranque directo con Java usa el puerto 8081 por defecto.
