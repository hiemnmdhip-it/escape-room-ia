# API, herramientas y escenario

## Vistas y sesiones

- `GET /` sirve la portada con la selección de idioma. `GET /juego?idioma=EN` sirve la pantalla de juego. Los idiomas son `ES` y `EN`, con `ES` como valor predeterminado.
- `POST /api/partidas?idioma=ES` crea una partida y responde HTTP 201 con el estado inicial y su `sessionId`. La partida arranca con diez puntos de vitalidad, siete lecturas y el altar como primer sello disponible.
- `POST /api/partidas/{sessionId}/reiniciar` responde HTTP 201 con el estado inicial y un UUID nuevo. Conserva el idioma de la partida anterior, salvo que se envíe `?idioma=ES` o `?idioma=EN`. La partida anterior sigue disponible en memoria.
- `GET /api/partidas/{sessionId}/estado` devuelve el estado actual. `GET /api/partidas/{sessionId}` admite la misma consulta.
- `PATCH /api/partidas/{sessionId}/idioma?idioma=EN` cambia el idioma y devuelve el estado actualizado.
- `GET /api/partidas/{sessionId}/historial` devuelve el arreglo completo de mensajes en orden de registro. Cada entrada contiene `fecha` en UTC, `autor` con valor `JUGADOR` o `NARRADOR` y `texto`.
- `GET /api/partidas/{sessionId}/diagnostico` devuelve `{"informe":"…"}` con la explicación resumida. `?extenso=true` amplía el contenido. Si la partida sigue en curso, devuelve HTTP 409.

El estado incluye `sessionId`, `idioma`, `resultado` (`PLAYING`, `VICTORY` o `DEFEAT`), `vitalidad`, `turnosEstancados` y `sellosResueltos`. También expone `escena`, `inventario`, `reliquiaSeleccionada`, `calibracion` con sus límites e intentos restantes, `camara` y `visitadas`. Las claves de escena identifican objetos y reliquias. Sus valores representan la disponibilidad de los sellos (`LOCKED`, `READY`, `SOLVED`), el estado del cofre (`OPEN`, `CLOSED`) o la recogida de reliquias (`COLLECTED`, `HIDDEN`).

## Turnos de chat y escena

`POST /api/partidas/{sessionId}/chat` recibe un JSON con exactamente uno de los campos `texto` o `escena`. El texto debe contener algún carácter distinto de un espacio y admite hasta 2000 caracteres. El booleano opcional `accion` se admite en turnos de texto y su valor predeterminado es `false`. Java resuelve `/diagnostico` en el propio chat y entrega el informe extenso al terminar la partida.

```json
{"texto":"¿Qué significa el símbolo del altar?"}
```

```json
{"escena":{"tipo":"USAR","reliquia":"scroll","elemento":"altar"}}
```

La respuesta usa `text/event-stream`. Cada evento `texto` aporta un fragmento de narración. El último evento, `estado`, contiene el resultado con `mensaje`, `estado`, `pista` y `diagnostico`. Los campos `pista` y `diagnostico` pueden ser nulos. La instantánea final permite actualizar el panel de escena en el mismo turno.

`POST /api/partidas/{sessionId}/escena` admite el objeto de acción directamente y devuelve el mismo resultado como JSON. Los tipos de acción y sus campos son los siguientes.

- `INSPECCIONAR` recibe `elemento`.
- `RECOGER` y `SELECCIONAR` reciben `reliquia`. Seleccionar la reliquia activa cancela la selección.
- `USAR` recibe `reliquia` y `elemento`.
- `PISTA` usa el sello activo.
- `RESOLVER` recibe `sello` e `intento`. El intento se envía como cadena.

Un UUID desconocido devuelve HTTP 404. Las solicitudes que incumplen el contrato HTTP devuelven HTTP 400. Los intentos válidos según el contrato que el motor rechaza devuelven un resultado de juego con el motivo y la instantánea actual.

## Herramientas Spring AI

- `inspeccionarElemento(elemento)` describe un objeto y su contexto. Los identificadores admitidos incluyen `altar`, `organ`, `chandelier`, `chest`, `scroll`, `stake` y `chalice`.
- `recogerReliquia(reliquia)` abre el cofre al recoger `chalice`. La repetición conserva el inventario.
- `usarReliquia(reliquia, elemento)` comprueba la posesión y la combinación. Las parejas de la cripta son `scroll` con `altar`, `stake` con `chandelier` y `chalice` con `organ`.
- `resolverSello(sello, intento)` valida la acción sobre el sello activo. El motor decide si el intento consume una lectura o vitalidad.
- `solicitarPista()` explica la técnica sin revelar la solución. Conserva la vitalidad.

El servidor habilita herramientas cuando el cliente marca una orden explícita, la partida está en curso y el texto supera la comprobación de consultas. Las preguntas se narran sin herramientas y conservan la vitalidad y los intentos. Las acciones estructuradas ejecutan la operación Java una vez y Ollama recibe el resultado para narrarlo. Tras dos intentos consecutivos sin avance, el motor ofrece una pista automática.

## Reglas de los sellos

1. **Altar.** El objetivo se elige por partida entre 1 y 100. Cada lectura válida compara un peso con el objetivo. Una lectura incorrecta consume un punto de vitalidad y ajusta el intervalo si aporta información. El máximo de siete lecturas viene de `floor(log2(100)) + 1`. Quedarse sin lecturas sin acertar cierra la partida.
2. **Órgano.** Las runas se encienden en el orden A, Z, R, A. El jugador apaga una por turno empezando por la última. `ArrayDeque` conserva la secuencia pendiente. Una runa válida fuera de orden cuesta un punto de vitalidad.
3. **Candelabro.** La partida empieza este tramo en A. El mapa ofrece A→B/C, B→A/D, C→A/E, D→B/F y E→C/D. Solo se permite una salida del estado actual que todavía no se haya visitado. La llegada a F completa la partida. Un salto o una reentrada cuestan un punto de vitalidad.

Al finalizar se explica la búsqueda binaria, la pila y la máquina de estados, tanto si se alcanza la salida como si se agotan las lecturas o la vitalidad. La versión extensa amplía el análisis de complejidad y los ejemplos de aplicaciones web.
