# API, herramientas y escenario

## Rutas de la Fase 1

- `POST /api/partidas?idioma=ES` crea una partida y devuelve HTTP 201 con su UUID y estado inicial. `ES` es el valor predeterminado. Reiniciar consiste en crear otro UUID.
- `GET /api/partidas/{sessionId}` devuelve el estado actual.
- `PATCH /api/partidas/{sessionId}/idioma?idioma=EN` cambia el idioma. Acepta `ES` y `EN`.
- `GET /api/partidas/{sessionId}/historial` devuelve mensajes en orden de registro con fecha UTC, autor y texto.
- `POST /api/partidas/{sessionId}/escena` ejecuta una acción estructurada. Admite `INSPECCIONAR` con `elemento`, `RECOGER` y `SELECCIONAR` con `reliquia`, `USAR` con ambos, `PISTA` y `RESOLVER` con `sello` e `intento`. El intento es una cadena.
- `POST /api/partidas/{sessionId}/chat` recibe `texto` y el booleano opcional `accion`, cuyo valor predeterminado es `false`. Responde con `text/event-stream`. Los eventos `texto` traen fragmentos. El evento final `estado` contiene el resultado estructurado completo. El comando `/diagnostico` entrega la versión extensa si la partida terminó.

Un UUID desconocido devuelve 404. Una solicitud que no cumple el contrato HTTP devuelve 400. Los intentos de juego rechazados regresan como resultados de la partida para mostrar la reacción, la vitalidad y el progreso.

El objeto `estado` incluye reserva de vitalidad, lecturas, intervalo de calibración, sellos, inventario, selección y estado visual. El resultado añade `mensaje`, `pista` y `diagnostico`. Las propiedades de pista y diagnóstico pueden ser nulas.

## Herramientas Spring AI

- `inspeccionarElemento(elemento)` describe un objeto y su contexto. Los identificadores admitidos incluyen `altar`, `organ`, `chandelier`, `chest`, `scroll`, `stake` y `chalice`.
- `recogerReliquia(reliquia)` abre el cofre al recoger `chalice`. La repetición conserva el inventario.
- `usarReliquia(reliquia, elemento)` comprueba la posesión y la combinación. Las parejas de la cripta son `scroll` con `altar`, `stake` con `chandelier` y `chalice` con `organ`.
- `resolverSello(sello, intento)` valida la acción sobre el sello activo. El motor decide si el intento consume una lectura o vitalidad.
- `solicitarPista()` explica la técnica sin revelar la solución. Conserva la vitalidad.

El servidor habilita herramientas cuando el cliente marca una orden y el texto no tiene forma de consulta. Para expresiones de duda libre, el cliente puede enviar siempre `accion: false`. La partida arranca con diez puntos de vitalidad y ofrece una pista automática tras dos intentos consecutivos sin avance.

## Reglas de los sellos

1. **Altar.** El objetivo se elige por partida entre 1 y 100. Cada lectura válida compara un peso con el objetivo. Una lectura incorrecta consume un punto de vitalidad y ajusta el intervalo si aporta información. El máximo de siete lecturas viene de `floor(log2(100)) + 1`. Quedarse sin lecturas sin acertar cierra la partida.
2. **Órgano.** Las runas se encienden en el orden A, Z, R, A. El jugador apaga una por turno empezando por la última. `ArrayDeque` conserva la secuencia pendiente. Una runa válida fuera de orden cuesta un punto de vitalidad.
3. **Candelabro.** La partida empieza este tramo en A. El mapa ofrece A→B/C, B→A/D, C→A/E, D→B/F y E→C/D. Solo se permite una salida del estado actual que todavía no se haya visitado. La llegada a F completa la partida. Un salto o una reentrada cuestan un punto de vitalidad.

Al finalizar se explica la búsqueda binaria, la pila y la máquina de estados, tanto si se alcanza la salida como si se agotan las lecturas o la vitalidad. La versión extensa amplía el análisis de complejidad y los ejemplos de aplicaciones web.
