# Propósito y objetivos

## Propósito

Cripta de Nocturnia es un escape room conversacional de terror sobrenatural. La aventura convierte en decisiones observables tres problemas de integración de Spring AI. El jugador distingue coincidencia literal de similitud semántica, decide qué cabe en el contexto de un modelo y comprueba qué hace falta para convertir una propuesta del narrador en una acción de Java.

La partida busca que el jugador pueda explicar las decisiones antes de implementar un ejemplo. El escenario facilita practicar una hipótesis, pedir una aclaración y reformular una instrucción en español o inglés.

## Público objetivo

La actividad se dirige a estudiantes de Desarrollo de Aplicaciones Web y a personas que conocen los fundamentos de Java y quieren introducirse en aplicaciones con modelos de lenguaje. Puede utilizarse antes de una práctica de Spring Boot o para revisar decisiones de arquitectura después de implementarla.

## Objetivos pedagógicos

1. Distinguir entre fragmentación, embeddings, similitud coseno y umbral al recuperar documentos por significado con un `VectorStore`.
2. Reconocer los roles System, User y Assistant. Dimensionar una ventana de mensajes y reservar tokens de contexto para la directiva y la salida.
3. Diferenciar lenguaje narrativo, argumentos aceptados por un método `@Tool` y datos tipados de salida. Comprender qué comprueba `BeanOutputConverter` y qué reglas valida Java.
4. Explicar cómo `ChatClient.stream().content()` entrega fragmentos a una aplicación MVC y cómo el navegador los representa mediante SSE.
5. Pedir aclaraciones, deducir las respuestas en español o inglés y explicar lo aprendido con el diagnóstico breve o extenso.

## Alcance funcional

El alcance del producto incluye una escena de cripta interactiva, un inventario seleccionable, un chat narrativo bilingüe y un registro consultable y descargable. Cada elemento comunica visualmente su estado. La escena y el chat comparten las reglas del motor. El jugador puede explorar y probar a su ritmo. Dos jugadas consecutivas sin avance activan una pista que sugiere una estrategia.

El motor encadena tres fases por runa. Las opciones se resuelven con acciones de escena, números y letras. La narración acompaña el turno y el servidor confirma el progreso con las reglas de Java. El jugador consulta el progreso en el encabezado y las reliquias bajo la escena. El chat muestra el relato y las reacciones de la cripta. El cierre permite elegir entre una explicación breve y otra ampliada.

La partida empieza con el pergamino y la estaca. Explorar permite conseguir el cáliz. Las cuatro ranuras reservan espacio visual para el inventario, cuya ocupación depende de lo que registre el motor. La bitácora exportada sirve para repasar el razonamiento empleado en cada sello después de jugar.

La sesión vive en la memoria de la instancia durante la partida. El historial completo permite recuperar el relato de la partida. La ventana de memoria conversacional conserva los mensajes recientes necesarios para mantener el contexto del diálogo.
