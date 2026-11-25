# Toma de decisiones

Los registros describen las decisiones de arquitectura del proyecto y los compromisos que implica cada elección.

## ADR 001. Spring MVC, Thymeleaf y JavaScript nativo

**Contexto.** La aplicación reúne una página de juego servida por Java y acciones del navegador sobre una sesión gestionada por el mismo servidor.

**Decisión.** Usar Spring MVC para las rutas y los controladores, Thymeleaf para la vista y JavaScript ES6 para los eventos de escena y chat. El controlador traduce la entrada y delega en el servicio. El servicio aplica las reglas del juego y devuelve datos que la vista puede representar.

**Alternativa evaluada.** Una SPA con un framework añade un proceso de compilación del cliente y un contrato de despliegue entre dos aplicaciones. Aporta ventajas cuando la navegación y el estado de interfaz crecen. La página de la cripta y su flujo de actualizaciones caben en el ciclo de compilación de Maven y en los eventos del navegador.

**Consecuencia.** Las reglas se prueban en Java y el navegador refleja las respuestas del servidor. Los módulos JS gestionan la interacción local, la renderización del estado publicado y la recepción de eventos.

## ADR 002. Sesiones en memoria con `ConcurrentHashMap`

**Contexto.** Cada partida necesita aislar su inventario, progreso, intentos, idioma e historial.

**Decisión.** Indexar cada `EscapeSession` por UUID en un `ConcurrentHashMap`. El mapa resuelve el acceso concurrente al registro de sesiones. Las transiciones que cambian varios datos de una sesión requieren su propia coordinación para mantener una actualización consistente. El historial usa `CopyOnWriteArrayList<MensajeHistorial>` y se presenta mediante una copia que no puede alterar el consumidor.

**Alternativa evaluada.** Una base relacional habilita recuperación tras reinicios, transacciones persistentes y sesiones compartidas entre instancias. Introduce un servicio adicional, esquema y migraciones. La partida de la entrega autónoma se ejecuta en una instancia y tiene duración limitada.

**Consecuencia.** El contenido de las sesiones depende de la vida del proceso. La copia interna al añadir un mensaje tiene un coste proporcional al historial, asumido para el volumen previsto de una partida y sus consultas.

## ADR 003. Herramientas de Spring AI y reglas en Java

**Contexto.** Una misma intención puede expresarse de varias maneras y en dos idiomas. El modelo puede interpretar el texto y producir una acción propuesta.

**Decisión.** Describir las acciones con métodos `@Tool` y validar la partida, el objetivo, la secuencia del sello y el inventario en el motor. El servidor asocia la llamada con la partida correspondiente mediante el contexto de la herramienta. La respuesta de la herramienta comunica el resultado que guía la narración. Las aclaraciones narrativas conservan el estado y los intentos.

**Alternativa evaluada.** Una gramática con expresiones regulares puede resolver comandos explícitos. Cada variación lingüística requiere mantener nuevos patrones, especialmente para distinguir preguntas sobre acciones de instrucciones para ejecutarlas.

**Consecuencia.** El comportamiento frente a entradas inválidas se prueba sin proveedor de IA. La disponibilidad y calidad de la interpretación conversacional dependen del modelo activo.

## ADR 004. SSE para la salida del servidor

**Contexto.** El chat debe mostrar la respuesta a medida que se genera y la escena debe recibir el estado que confirma el motor.

**Decisión.** Publicar los resultados mediante Server-Sent Events. El navegador inicia acciones mediante solicitudes HTTP. El servidor devuelve los eventos de texto y estado por el canal de salida. Spring MVC permite gestionar el flujo con `SseEmitter`.

**Alternativa evaluada.** WebSocket admite mensajes bidireccionales sobre una sola conexión y exige coordinar su ciclo de vida. La comunicación de entrada ya tiene un canal HTTP que encaja con los controladores MVC y los eventos del navegador.

**Consecuencia.** La integración web tendrá que cerrar los emisores al finalizar o desconectar el cliente y manejar errores de proveedor sin que la vista pierda el último estado confirmado.

## ADR 005. Perfiles para Ollama y Gemini

**Contexto.** El proyecto debe poder ejecutarse con un modelo local y ofrecer la alternativa remota de Google Gemini.

**Decisión.** Activar Ollama por defecto y seleccionar Gemini mediante un perfil. Cargar la credencial de Gemini desde `GEMINI_API_KEY`. Ambos perfiles usan el mismo conjunto de herramientas y la misma lógica de estado.

**Alternativa evaluada.** Mantener un único proveedor simplifica la configuración. El modo local facilita una ejecución sin clave y el modo remoto permite aprovechar Gemini cuando el equipo no dispone de un modelo adecuado.

**Consecuencia.** Las pruebas unitarias del motor y las herramientas pueden ejecutarse sin contactar con ningún proveedor. Los ensayos de integración con modelos reales requieren la configuración del perfil correspondiente.

## ADR 006. Historial de partida y memoria conversacional

**Contexto.** El jugador debe poder consultar la bitácora completa. El modelo necesita un contexto reciente de tamaño acotado.

**Decisión.** Guardar el historial cronológico completo en la sesión y alimentar `MessageWindowChatMemory` con un `InMemoryChatMemoryRepository`. Usar el UUID de partida como identificador de conversación y registrar cada mensaje visible una sola vez en el punto de escritura acordado.

**Alternativa evaluada.** Reutilizar la ventana como bitácora haría que el criterio de recorte del modelo determinara qué datos puede consultar el jugador. Enviar el historial completo a cada generación deja crecer el tamaño de la petición durante la partida.

**Consecuencia.** La continuidad del diálogo y la exportación sirven a consumidores distintos. Se requiere probar que conservan el orden, que diferencian las sesiones y que la bitácora permanece completa al recortarse la ventana.

## ADR 007. Tres sellos encadenados

**Contexto.** La dificultad debe aumentar durante la partida. El jugador necesita deducir cada regla mediante interacciones del mundo narrativo.

**Decisión.** Calibrar un peso entero de 1 a 100 en siete intentos. La comparación de cada ajuste descarta un lado del intervalo, y siete comparaciones bastan en el peor caso para una búsqueda binaria sobre 100 valores. En el siguiente sello, guardar el orden de activación en un `ArrayDeque` y validar cada apagado contra la cima. Representar las cámaras finales con estados y transiciones de un `enum` y guardar las visitas en un `Set`.

**Alternativas evaluadas.** Una tabla de frecuencias entrena el reconocimiento de repeticiones y resulta adecuada para una prueba de clasificación. Una exploración obligatoria de todo el grafo entrena búsqueda de rutas completas. Las acciones elegidas se ajustan al orden narrativo de calibrar, invertir una secuencia y cruzar la cripta.

**Consecuencia.** El servidor conserva el progreso de cada mecánica y solo habilita el siguiente sello al completar el actual. El motor puede informar del efecto de cada acción con una respuesta breve y comprobable.

## ADR 008. Límites de partida y pistas adaptativas

**Contexto.** Los intentos deben tener consecuencias. El jugador necesita orientación cuando repite acciones que no le ayudan a avanzar.

**Decisión.** Iniciar la partida con diez puntos de vitalidad y mostrar una pista sutil tras dos intentos sin progreso. Las consultas de significado y las peticiones de ayuda conservan los intentos. La vista recibe instantáneas del estado para representar la escena y el inventario.

**Alternativa evaluada.** Ofrecer una pista en cada turno garantiza visibilidad, aunque revela continuamente la estrategia. Un umbral más alto deja más espacio para experimentar y puede retrasar la orientación hasta después de varias acciones fallidas.

**Consecuencia.** Las pruebas verifican la relación entre progreso, reinicio del contador, activación de la pista y derrota.
