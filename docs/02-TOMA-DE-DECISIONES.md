# Toma de decisiones

Los registros describen las decisiones de arquitectura del proyecto y los compromisos que implica cada elección.

## ADR 001. Spring MVC, Thymeleaf y JavaScript nativo

**Contexto.** La aplicación reúne una página de juego servida por Java y acciones del navegador sobre una sesión gestionada por el mismo servidor.

**Decisión.** Usar Spring MVC para las rutas y los controladores, Thymeleaf para la vista y JavaScript ES6 para los eventos de escena y chat. El controlador traduce la entrada y delega en el servicio. El servicio aplica las reglas del juego y devuelve datos que la vista puede representar.

**Alternativa evaluada.** Una SPA con un framework añade un proceso de compilación del cliente y un contrato de despliegue entre dos aplicaciones. Aporta ventajas cuando la navegación y el estado de interfaz crecen. La página de la cripta y su flujo de actualizaciones caben en el ciclo de compilación de Maven y en los eventos del navegador.

**Consecuencia.** Las reglas se prueban en Java y el navegador refleja las respuestas del servidor. Los módulos JS gestionan la interacción local, la renderización del estado publicado y la recepción de eventos.

## ADR 002. Sesiones en memoria con `ConcurrentHashMap`

**Contexto.** Cada partida necesita aislar su inventario, progreso, idioma e historial.

**Decisión.** Indexar cada `EscapeSession` por UUID en un `ConcurrentHashMap`. El mapa resuelve el acceso concurrente al registro de sesiones. Las transiciones que cambian varios datos de una sesión requieren su propia coordinación para mantener una actualización consistente. El historial usa `CopyOnWriteArrayList<MensajeHistorial>` y se presenta mediante una copia que no puede alterar el consumidor.

**Alternativa evaluada.** Una base relacional habilita recuperación tras reinicios, transacciones persistentes y sesiones compartidas entre instancias. Introduce un servicio adicional, esquema y migraciones. La partida de la entrega autónoma se ejecuta en una instancia y tiene duración limitada.

**Consecuencia.** El contenido de las sesiones depende de la vida del proceso. La copia interna al añadir un mensaje tiene un coste proporcional al historial, asumido para el volumen previsto de una partida y sus consultas.

## ADR 003. Herramientas de Spring AI y reglas en Java

**Contexto.** Una misma intención puede expresarse de varias maneras y en dos idiomas. El modelo puede interpretar el texto y producir una acción propuesta.

**Decisión.** Describir las acciones con métodos `@Tool` y validar el objetivo, la secuencia del sello y el inventario en el motor. Java reconoce directamente las opciones del sello y reserva su resolución a la entrada efectiva del jugador. El modelo interpreta órdenes de escena y recibe las herramientas de ese turno. Las inspecciones y las combinaciones válidas aportan hechos que el modelo puede ambientar. Las consultas de significado conservan el estado de la sala.

**Alternativa evaluada.** Una gramática con expresiones regulares puede resolver comandos explícitos. Cada variación lingüística requiere mantener nuevos patrones, especialmente para distinguir preguntas sobre acciones de instrucciones para ejecutarlas.

**Consecuencia.** El comportamiento frente a entradas inválidas se prueba sin proveedor de IA. La disponibilidad y calidad de la interpretación conversacional dependen del modelo activo.

## ADR 004. SSE para la salida del servidor

**Contexto.** El chat debe mostrar la respuesta a medida que se genera y la escena debe recibir el estado que confirma el motor.

**Decisión.** Publicar los resultados mediante Server-Sent Events. El navegador inicia acciones mediante solicitudes HTTP. Spring MVC publica texto y estado con `SseEmitter`. Java emite las respuestas del motor en fragmentos espaciados. Las conversaciones se publican frase a frase conforme `ChatClient.stream().content()` entrega texto que supera la revisión. Ambos flujos se codifican en UTF-8.

**Alternativa evaluada.** WebSocket admite mensajes bidireccionales sobre una sola conexión y exige coordinar su ciclo de vida. La comunicación de entrada ya tiene un canal HTTP que encaja con los controladores MVC y los eventos del navegador. Una llamada completa seguida de un troceado retrasa todo el texto hasta el final de la inferencia.

**Consecuencia.** Una acción validada actualiza la escena mediante el evento `escena`. Los fragmentos `texto` siguen el resultado y el evento `estado` cierra el turno. Si el flujo se corta, el navegador consulta el estado por HTTP sin reenviar la acción.

## ADR 005. Inferencia local con Ollama

**Contexto.** La instalación autónoma requiere inferencia local sin credenciales externas.

**Decisión.** Incluir el perfil `ollama` y preparar `qwen3:8b` antes de jugar. Ollama implementa el contrato de `ChatModel` que usa el narrador.

**Alternativas evaluadas.** Un único proveedor local, como en la Fase 2, elimina la configuración remota y exige recursos de cálculo en el equipo. Un proveedor exclusivamente remoto reduce esos requisitos y exige una clave y conectividad durante la partida.

**Consecuencia.** Los mensajes y su contexto se envían al proceso local. Las pruebas del motor y los controladores se ejecutan sin inferencia. La narración generada requiere que Ollama esté disponible.

## ADR 006. Historial de partida y memoria conversacional

**Contexto.** El jugador debe poder consultar la bitácora completa. El modelo necesita un contexto reciente de tamaño acotado.

**Decisión.** Guardar el historial cronológico completo en la sesión y alimentar `MessageWindowChatMemory` con un `InMemoryChatMemoryRepository`. Usar el UUID de partida como identificador de conversación y registrar cada mensaje visible una sola vez en `HistorialService`. Java lee la ventana para interpretar referencias recientes y conserva las inscripciones dentro del motor.

**Alternativa evaluada.** Reutilizar la ventana como bitácora haría que el criterio de recorte del modelo determinara qué datos puede consultar el jugador. Enviar el historial completo a cada generación deja crecer el tamaño de la petición durante la partida.

**Consecuencia.** La continuidad del diálogo y la exportación sirven a consumidores distintos. Se requiere probar que conservan el orden, que diferencian las sesiones y que la bitácora permanece completa al recortarse la ventana.

## ADR 007. Aprendizaje de Spring AI en tres sellos

**Contexto.** El jugador debe razonar sobre recuperación semántica, contexto conversacional y comprobación de invocaciones dentro de una escena gótica.

**Decisión.** Revelar un sello en la escena presenta la primera pregunta. Java valida tres elecciones en orden antes de desbloquear cada runa. El altar usa datos didácticos de similitud y la compuerta plantea un contrato de salida tipada. El órgano expresa el presupuesto como suspiros de un fuelle y explica su equivalencia técnica en el diagnóstico.

**Alternativas evaluadas.** Una búsqueda real con embeddings requeriría preparar un corpus y un proveedor de embeddings. Un veredicto generado por el modelo puede incumplir el esquema o contener hechos inventados. Las tablas de datos y los contratos del acertijo permiten repetir y probar la deducción con los mismos criterios en ambos idiomas.

**Consecuencia.** La partida usa realmente Spring AI para memoria, herramientas de escena y streaming. Los ejercicios de RAG y conversión de salidas representan sus respectivos contratos. La victoria comprueba tres sellos terminados y el inventario completo.

## ADR 008. Exploración libre y pistas progresivas

**Contexto.** La partida debe dar margen para deducir, probar y pedir explicaciones. Las jugadas repetidas que no aportan información indican que puede hacer falta una pista.

**Decisión.** Mostrar una pista tras dos intentos consecutivos sin progreso. El botón de pista consulta directamente al motor y las preguntas escritas se tratan como conversación. Avanzar una fase reinicia el contador. El diagnóstico se abre al ganar.

**Alternativa evaluada.** Ofrecer una pista en cada turno garantiza visibilidad, aunque revela continuamente la estrategia. Un umbral más alto deja más espacio para experimentar y puede retrasar la orientación hasta después de varias acciones fallidas.

**Consecuencia.** Las pruebas verifican que los errores reiterados permiten seguir jugando, que la pista automática aparece al alcanzar el umbral y que cada desenlace conserva los sellos resueltos.

## ADR 009. Escena visual y controles HTML

**Contexto.** El prototipo sitúa los objetos en una estancia ilustrada y usa su posición para orientar al jugador. Los mismos objetos deben poder inspeccionarse con ratón, teclado o pantalla táctil.

**Decisión.** Trasladar el SVG interactivo, sus zonas de pulsación y su CSS desde el HTML original. Thymeleaf resuelve las rutas de la aplicación. JavaScript conecta el ratón y el teclado con la API y actualiza las etiquetas y los efectos según el estado publicado.

**Alternativa evaluada.** Una lista de tarjetas con imágenes facilita colocar más objetos en poco espacio. Una escena con posiciones fijas conserva la relación espacial entre el candelabro, el altar, el órgano y el cofre.

**Consecuencia.** Los objetos se pueden enfocar y activar con el teclado. Java valida la selección, los sellos y la apertura del cofre.
