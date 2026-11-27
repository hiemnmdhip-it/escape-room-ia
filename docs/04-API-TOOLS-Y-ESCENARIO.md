# API, herramientas y escenario

## Vistas y sesiones

- `GET /` sirve la portada. `GET /juego?idioma=EN` sirve la pantalla de juego. El HUD permite alternar entre `ES` y `EN`. La sesión comienza en `ES` si no se especifica idioma.
- `POST /api/partidas?idioma=ES` crea una partida y responde HTTP 201 con su `sessionId`. El altar comienza disponible en fase 1.
- `POST /api/partidas/{sessionId}/reiniciar` responde HTTP 201 con el estado inicial y un UUID nuevo. Conserva el idioma de la partida anterior, salvo que se envíe `?idioma=ES` o `?idioma=EN`. La partida anterior sigue disponible en memoria.
- `GET /api/partidas/{sessionId}/estado` devuelve el estado actual. `GET /api/partidas/{sessionId}` admite la misma consulta.
- `PATCH /api/partidas/{sessionId}/idioma?idioma=EN` cambia el idioma y devuelve el estado actualizado.
- `GET /api/partidas/{sessionId}/historial` devuelve el arreglo completo de mensajes en orden de registro. Cada entrada contiene `fecha` en UTC, `autor` con valor `JUGADOR` o `NARRADOR` y `texto`.
- `GET /api/partidas/{sessionId}/diagnostico` devuelve `{"informe":"…"}` con la explicación resumida. `?extenso=true` amplía el contenido. Si la partida sigue en curso, devuelve HTTP 409.
- `GET /api/partidas/{sessionId}/bienvenida` transmite la introducción en el idioma de la sesión. El navegador lo solicita al entrar y al reiniciar. El historial guarda una copia por idioma.
- `GET /api/partidas/{sessionId}/diagnostico/stream` transmite el informe resumido. Admite `?extenso=true` y devuelve HTTP 409 si la partida sigue en curso.

Ambos flujos GET devuelven `text/event-stream` con eventos `texto` y cierran con `estado`, igual que un turno de chat.

El estado incluye `sessionId`, `idioma`, `resultado` (`PLAYING` o `VICTORY`), `turnosEstancados` y `sellosResueltos`. También expone `escena`, `inventario`, `reliquiaSeleccionada` y `reliquiasPendientes`. Los campos `calibracion`, `camara` y `visitadas` del contrato anterior se conservan por compatibilidad y se devuelven como `null`, `null` y un conjunto vacío, respectivamente.

`fasesRunas` indica la fase visible de cada sello mediante las claves `altar`, `organ` y `chandelier` y los valores 1, 2 o 3. Un sello bloqueado empieza en 1. Un sello resuelto conserva el 3 y tiene estado `SOLVED` en `escena`. El SVG muestra «Fase n/3» o «Desbloqueada» en sus rótulos existentes. La barra del HUD representa el porcentaje de sellos resueltos. El cofre usa `OPEN` o `CLOSED`, y los coleccionables usan `COLLECTED` o `HIDDEN`.

## Turnos de chat y escena

`POST /api/partidas/{sessionId}/chat` recibe un JSON con exactamente uno de los campos `texto` o `escena`. El texto debe contener algún carácter distinto de un espacio y admite hasta 2000 caracteres. El booleano opcional `accion` se admite en turnos de texto y su valor predeterminado es `false`. Con `accion: true`, Java valida directamente una opción de la runa activa (`1`, `2`, `3`, `A`, `B` o `C`). También admite entradas como «Elijo 2». Java también atiende `/diagnostico` para entregar el informe extenso una vez cerrada la partida.

```json
{"texto":"¿Qué significa el símbolo del altar?"}
```

```json
{"escena":{"tipo":"USAR","reliquia":"scroll","elemento":"altar"}}
```

La respuesta usa `text/event-stream`. Al ejecutar una acción estructurada, un intento directo de chat o una herramienta, el servidor publica `escena` con el resultado validado por Java. El cliente actualiza el escenario y muestra la reacción mientras continúa el turno. Cada evento `texto` aporta un fragmento narrativo. El último evento, `estado`, contiene el resultado final. Los resultados incluyen `mensaje`, `estado`, `pista`, `diagnostico` y `runaFallida`. Los tres últimos admiten valores nulos.

Las inspecciones y los intentos publican un único bloque. Su texto procede del resultado de Java. El narrador explica criterios en las pistas y Java conserva la validación de las opciones. Una orden escrita puede invocar una herramienta con retorno directo. El resultado de la herramienta se publica sin una segunda generación.

Cuando `runaFallida` contiene un identificador de escena, el cliente aplica `rune-shake` una vez en el turno, con un destello carmesí de 360 ms. Con la preferencia de movimiento reducido, aplica solo el destello.

`POST /api/partidas/{sessionId}/escena` admite el objeto de acción directamente y devuelve el resultado del motor como JSON. Los tipos de acción y sus campos son los siguientes.

- `INSPECCIONAR` recibe `elemento`. Examinar individualmente el altar o el órgano en su fase 1 inicia la prueba correspondiente. El botón rápido **Inspeccionar** usa `room` para describir la estancia.
- `RECOGER` y `SELECCIONAR` reciben `reliquia`. Seleccionar la reliquia activa cancela la selección.
- `USAR` recibe `reliquia` y `elemento`.
- `PISTA` devuelve la siguiente pista del sello activo. Comparte la progresión con las peticiones de ayuda en el chat.
- `RESOLVER` recibe `sello` e `intento`. El intento se envía como cadena y se comprueba contra la fase activa.

Un UUID desconocido devuelve HTTP 404. Las solicitudes que incumplen el contrato HTTP devuelven HTTP 400. Los intentos válidos según el contrato que el motor rechaza devuelven un resultado de juego con el motivo y la instantánea actual.

## Herramientas Spring AI

- `inspeccionarElemento(elemento)` ejecuta una inspección explícita y puede revelar el enunciado de la Fase 1/3 del altar u órgano activo. Los identificadores admitidos incluyen `room`, `altar`, `organ`, `chandelier`, `chest`, `scroll`, `stake` y `chalice`.
- `recogerReliquia(reliquia)` abre el cofre al recoger `chalice`. La repetición conserva el inventario.
- `usarReliquia(reliquia, elemento)` comprueba la posesión y la combinación. Las parejas de la cripta son `scroll` con `altar`, `stake` con `chandelier` y `chalice` con `organ`.
- `resolverSello(sello, intento)` comprueba el sello activo, la fase y la opción escrita por el jugador. Java lo invoca directamente en los turnos de elección.
- `solicitarPista()` da la orientación de la fase activa. Las siguientes peticiones vuelven a mostrar la inscripción.

El servidor registra herramientas de escena para el modelo al reconocer una orden explícita durante la partida. Cada turno admite una ejecución. Las consultas de significado, fase o inventario usan la escena en modo de lectura, el estado validado y el contexto reciente. Dos errores consecutivos en una fase activan una pista. Las elecciones incorrectas mantienen fase e inventario y publican `runaFallida` con el identificador del objeto.

## Reglas de los sellos

Cada runa requiere resolver tres elecciones en orden. La interacción con la escena revela la primera pregunta. Una respuesta correcta avanza de fase y solo el acierto de la tercera rompe el sello. Las fases 1 y 3 usan letras y la fase 2 usa números.

1. **Altar de los Fragmentos.** Examinar el altar o aplicarle el pergamino revela la fase 1, que pide distinguir la búsqueda por significado de la coincidencia literal. La fase 2 cruza tamaño de fragmento y umbral de similitud para conservar una profecía de 80 palabras y recuperar el pasaje de puntuación 0,86 por encima del ruido. La fase 3 pide elegir el concepto más cercano entre tres resultados.
2. **Órgano de la Memoria Efímera.** Examinar el órgano o aplicarle el cáliz revela la fase 1, que pregunta por qué se olvidan los cantos antiguos. La fase 2 pide calcular cuántos ecos recientes caben en un fuelle de 120 suspiros, reservando aliento para el acorde y el canto grabado en piedra. La fase 3 pide purgar el eco viejo y conservar la inscripción y las voces recientes. El informe final relaciona la metáfora con la ventana conversacional y el presupuesto del modelo.
3. **Compuerta de las Invocaciones.** Aplicar la estaca de plata al candelabro revela la fase 1, que pide distinguir una declaración de una acción que mueve el cerrojo. La fase 2 exige identificar el mecanismo registrado, la cámara y el giro admitidos. La fase 3 pide elegir un veredicto con campos tipados, comprobados contra el mecanismo. La actividad representa la relación entre `@Tool`, `BeanOutputConverter` y la comprobación del dominio.

La victoria exige tres sellos desbloqueados y los tres coleccionables. El pergamino y la estaca están en el inventario inicial y el cáliz se recoge en el cofre. Con los tres sellos abiertos y el cáliz pendiente, el resultado sigue siendo `PLAYING`. Las consultas y los intentos pueden continuar sin límite de turnos. La acción que completa ambas condiciones declara `VICTORY` y entrega el Diagnóstico Pedagógico de Spring AI resumido. La versión extensa desarrolla la aplicación en Spring Boot de RAG, memoria conversacional, herramientas, salidas tipadas y streaming.
