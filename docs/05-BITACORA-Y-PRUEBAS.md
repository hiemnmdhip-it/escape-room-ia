# Bitácora y pruebas

Los apartados fechados registran el comportamiento y los resultados de cada revisión. Para el alcance y las evidencias de la entrega actual, consulta el [informe final](INFORME-FINAL.md).

## Fase 1. Motor, memoria y herramientas

El trabajo partió de un proyecto Maven importado en Spring Tool Suite. La revisión inicial identificó las dependencias, la configuración y las referencias de diseño. La fase incorporó los paquetes MVC, la selección inicial de proveedores, las sesiones en memoria, los sellos encadenados, las herramientas de Spring AI y la personalidad del guardián.

### Cobertura inicial

- **Motor y sesiones.** Crear partidas independientes con UUID. Comprobar el idioma, los cambios de escena, la recogida y selección de reliquias, el rechazo de combinaciones inválidas y la pista tras el umbral de estancamiento.
- **Sello I.** Verificar los cien pesos posibles, los avisos de peso alto o bajo, la estrategia por punto medio en hasta siete lecturas y el cambio al segundo sello.
- **Sello II.** Verificar que el último elemento activado es el primero que puede apagarse y que una elección incorrecta conserva el orden pendiente. Confirmar la transición al sello final al vaciar la pila.
- **Sello III.** Verificar las transiciones válidas, el rechazo de pasos ilegales o ciclos y la llegada al estado de victoria.
- **Historial.** Comprobar orden cronológico, roles, aislamiento por UUID y correspondencia entre la bitácora y los mensajes recientes de la ventana conversacional.
- **Herramientas.** Probar inspección, recogida y uso de reliquias, resolución y petición de pistas. Verificar sus contratos y el rechazo de identificadores o acciones inválidos sin iniciar el modelo.
- **Consultas y cierre.** Comprobar que una pregunta contextual preserva intentos y estado. Verificar la explicación breve y el acceso a la versión extensa en el desenlace.
- **API y perfiles.** Comprobar la ruta del historial, el intercambio SSE y la creación del modelo adecuado con cada perfil, sin enviar solicitudes a los proveedores.

### Incidencias resueltas

- En la Fase 1 se aisló la configuración del modelo por perfil y se eligió `ollama` como predeterminado. La Fase 2 consolidó la integración local.
- Spring Boot 4.1.3 gestiona JUnit 6 y su extensión de contexto usa métodos nuevos de JUnit. Para conservar JUnit 5, las pruebas de arranque abren y cierran los contextos directamente con `SpringApplicationBuilder`.
- Jackson rechazaba el campo opcional `accion` cuando se omitía en el chat. El controlador lo interpreta como `false` y la prueba de SSE cubre ese JSON.

### Evidencia de ejecución

El comando de aceptación es `mvn test`. En Windows se usó el Maven Wrapper del repositorio porque Maven no estaba disponible en el PATH.

El 25 de noviembre de 2026, `.\mvnw.cmd -B -o test` terminó con `BUILD SUCCESS`. Ejecutó 16 pruebas JUnit 5.14.2 con 0 fallos, 0 errores y 0 omisiones. Se usó JDK 25 y se compiló con `--release 21`. La ejecución en modo desconectado comprobó reglas, sesiones, historial, herramientas, SSE y arranque de Spring sin inferencia.

El primer empaquetado sin red pasó las pruebas. El paso `repackage` necesitaba dependencias de Maven que todavía no estaban en caché. `.\mvnw.cmd -B -ntp clean package` las resolvió y terminó con `BUILD SUCCESS`. El resultado fue `target/escape-room-ia-0.0.1-SNAPSHOT.jar`. Se verificó su arranque con `iniciar.bat` en modo sin servidor HTTP.

## Fase 2. Ollama local e integración MVC

La Fase 2 consolida Ollama como único proveedor. La configuración carga el modelo local sin claves de servicio y con la descarga automática desactivada. Se añadieron las rutas de portada y partida en Thymeleaf y los contratos REST para reinicio, sincronización del estado y diagnóstico. El canal SSE admite mensajes escritos y acciones estructuradas de la escena. Java aplica las reglas de juego y Ollama genera la narración.

### Cobertura de regresión

- **Vistas y sesiones.** Selección ES/EN, arranque en el idioma elegido, estado inicial con UUID y reinicio con acceso al historial anterior.
- **Configuración de Ollama.** El contexto de Spring expone un único modelo de chat, incluso cuando se activa un perfil adicional. Esta comprobación inicia Spring sin pedir inferencia al modelo.
- **Turnos SSE.** Consumo de eventos de texto, sincronización con el evento de estado y validación del cuerpo HTTP. Una acción de escena y un mensaje de chat comparten el transporte y alimentan la bitácora. La prueba unitaria de ambientación comprueba que el narrador recibe el resultado validado y conserva la instantánea de Java.
- **Historial en memoria.** Orden de las entradas, correspondencia con la ventana reciente y aislamiento entre partidas, incluida la creada al reiniciar.
- **Diagnóstico de Java.** Acceso al finalizar, por defecto en versión resumida y con opción extensa. Las dos versiones explican la búsqueda binaria, la pila LIFO y la máquina de estados.

### Evidencia de ejecución

El 25 de noviembre de 2026, `.\mvnw.cmd -B -o test` terminó con `BUILD SUCCESS`. Ejecutó 22 pruebas JUnit 5 con 0 fallos, 0 errores y 0 omisiones. El entorno usó JDK 25 con compilación para Java 21 mediante `--release 21`.

La primera prueba HTTP del JAR confirmó las vistas, los eventos SSE del motor, el estado y el historial. El primer fragmento generado por Ollama superó el tiempo de espera inicial. El servicio local disponía de `llama3:latest`. Una consulta directa con ese modelo tardó 43,7 segundos en procesar un mensaje mínimo en frío. Se acortó la petición de ambientación de la escena y se amplió la espera de lectura a 90 segundos entre fragmentos. El emisor SSE pasó a admitir 120 segundos por petición.

La ejecución final de `.\mvnw.cmd -B -o clean package` terminó con `BUILD SUCCESS` y 23 pruebas JUnit 5, con 0 fallos, 0 errores y 0 omisiones. El empaquetado se ejecutó en una copia aislada porque otro proceso Java tenía abierto el JAR anterior. El JAR de verificación quedó en `target/fase2-verificacion/target/escape-room-ia-0.0.1-SNAPSHOT.jar`.

La prueba HTTP final del JAR se ejecutó con `llama3:latest` en Ollama local. La portada, la vista inglesa y el JavaScript respondieron con HTTP 200. Recoger el cáliz emitió 36 eventos de texto y el estado final por SSE. El nuevo inventario coincidió con la consulta de estado y la acción quedó registrada en el historial. Una pregunta de chat obtuvo narración de Ollama sin modificar la sala. El reinicio devolvió otro UUID, el idioma inglés y el estado inicial.

La inspección del JAR identificó las dos configuraciones YAML y la integración Spring AI de Ollama. La prueba de configuración comprobó `qwen3:8b` como modelo predeterminado. La prueba HTTP usó el modelo ya instalado en el equipo.

## Fase 3. Interfaz final y distribución

La referencia local de `.codex-context/cripta-nocturnia-prototipo/` sirvió para adaptar la ilustración de la estancia, la disposición de los objetos y la barra de reliquias. La interfaz aplica la paleta gótica neón de la entrega y enlaza los controles con los estados de Java. El prototipo utilizaba respuestas simuladas. La entrega ejecuta los sellos del motor, recibe la narración por SSE y obtiene la bitácora desde la sesión activa.

La acción directa de escena publica su resultado antes de esperar la frase de ambientación. Esto permite ver el destello y la actualización del inventario o los sellos mientras sigue abierto el turno. La tarjeta final arranca con el diagnóstico resumido y ofrece una consulta del informe extenso. La bitácora descarga el registro en texto plano desde el navegador.

`iniciar.bat` ejecuta el JAR con Ollama y espera a que responda el servidor antes de abrir la portada en el navegador.

### Cobertura de aceptación

- El turno estructurado debe publicar el evento `escena` antes de los fragmentos `texto` y cerrar con el evento `estado`. El JSON de estado debe coincidir con la sesión y el efecto del turno debe aparecer una sola vez en su bitácora.
- El recorrido de navegador comprueba la inspección de la cripta, la selección y el uso de reliquias, la apertura del cofre, el cambio de idioma en la sesión activa y la exportación de la bitácora.
- El desenlace debe abrir el diagnóstico resumido. Alternar entre las versiones debe conservar el estado de la partida.

### Evidencia de ejecución

`node --check src/main/resources/static/js/cripta.js` validó la sintaxis del cliente JavaScript.

El 25 de noviembre de 2026, el empaquetado en la raíz encontró el JAR anterior abierto por otra instancia. Se ejecutó `.\mvnw.cmd -B clean package` en una copia aislada bajo `target/fase3-verificacion/`. El comando terminó con `BUILD SUCCESS` y ejecutó 24 pruebas JUnit 5, con 0 fallos, 0 errores y 0 omisiones. Se usó JDK 25 con `--release 21`. El resultado quedó en `target/fase3-verificacion/target/escape-room-ia-0.0.1-SNAPSHOT.jar`.

La suite comprobó que el perfil local selecciona Ollama. La prueba de contexto se ejecutó sin inferencia.

`iniciar.bat --spring.main.web-application-type=none --spring.main.banner-mode=off` arrancó el JAR aislado con el perfil `ollama` y terminó con código 0. La apertura automática del navegador en el puerto 8080 quedó sin verificar porque el puerto estaba ocupado.

La prueba de inferencia local usó `llama3:latest`, disponible en el equipo, y limitó la salida a 60 tokens. Una pregunta en inglés sobre el cáliz produjo fragmentos SSE y el evento final `estado`. La partida conservó cero sellos y dos reliquias. Quedó pendiente la inferencia con `qwen3:8b`.

El recorrido de extremo a extremo en Microsoft Edge usó el JAR de verificación y un servidor simulado compatible con Ollama para controlar las respuestas y la llamada a `resolverSello`. Verificó la escena, las cuatro ranuras, la apertura del cofre, la selección y aplicación de una reliquia, la inspección, la pista gratuita y el cambio ES/EN con la misma sesión. Una orden escrita actualizó el sello a través de la herramienta de Spring AI. El recorrido completó los tres sellos, alternó las dos versiones del diagnóstico, descargó el `.txt` al disco y reinició la partida. A 375 píxeles de ancho, la interfaz se mostró sin desplazamiento horizontal.

## 26 de noviembre de 2026. Fidelidad visual y exploración libre

La partida recuperó la estructura, el SVG interactivo y el CSS de `.codex-context/index.html`. La portada usa el HTML del paquete del prototipo. El medidor del HUD muestra el avance de los sellos y el botón del diagnóstico requiere confirmación si el jugador todavía está en la cripta.

El motor conserva el intervalo posible de la balanza, las runas pendientes y las cámaras visitadas entre jugadas. Las inspecciones describen las inscripciones y las reglas. Java entrega tres niveles de ayuda por sello y un recordatorio automático tras dos jugadas consecutivas sin avance. Las consultas libres reciben contexto verificado y pueden recurrir a la explicación local cuando falla el proveedor.

Se ejecutó `.\mvnw.cmd -B clean package` en `target/verificacion-correccion/` porque Windows mantenía abierto el JAR anterior. El empaquetado terminó con `BUILD SUCCESS`. Las 25 pruebas JUnit 5 pasaron sin fallos, errores ni omisiones. Cubren lecturas repetidas, pistas progresivas, victoria, rendición con progreso parcial y el contrato SSE del diagnóstico.

Chrome comparó la portada y los estilos de los paneles del juego con los prototipos. La prueba midió la vista de juego a 1440 y 390 píxeles. Las dimensiones y posiciones de los paneles difirieron como máximo un píxel. La escena estrecha conserva el recorte lateral de la referencia.

El recorrido con un proveedor simulado compatible con Ollama comprobó la ambientación, la explicación local ante una respuesta vacía y los tres niveles de pista. Completó los tres sellos desde el chat después de repetir errores, cambió el idioma, comprobó la descarga de la bitácora y verificó que la rendición abre el diagnóstico con el progreso real.

## 26 de noviembre de 2026. Fases, victoria y narración contextual

Cada sello pasó a tener tres fases validadas por el motor. La calibración usa rondas de siete lecturas y un turno de confirmación. La pila se descubre al examinar el órgano y se vacía en orden inverso. El candelabro requiere la estaca para activar A, un recorrido hasta D y la confirmación de F. El pergamino y la estaca se entregan al iniciar la partida. El cáliz del cofre completa el inventario obligatorio y puede recogerse después de abrir F.

La escena reutiliza los rótulos del SVG para indicar la fase o el desbloqueo. Un fallo publica su identificador en el evento SSE de acción y dispara una sacudida breve con brillo carmesí. El guardián recibe las fases y las reliquias pendientes como contexto, conserva el estado en las consultas y contrasta la respuesta del modelo con el historial. Las pruebas de narración usan un modelo simulado y cubren una respuesta repetida, el retorno al objetivo de la sala y el uso de herramientas ante una orden explícita.

JUnit 5 verificó los cien pesos posibles, el reinicio de una ronda agotada, las transiciones de los tres sellos, la conservación del progreso ante errores y el rechazo de saltos y ciclos. La prueba HTTP verificó que el diagnóstico devuelve 409 con los tres sellos y el cáliz pendiente, y que la recogida posterior anuncia la victoria y entrega el informe por SSE. Otra prueba confirmó que el evento con la runa fallida precede al texto del chat. `node --check src/main/resources/static/js/cripta.js` completó la comprobación de sintaxis.

Windows tenía abierto el JAR de `target`. Se conservó la instancia y se ejecutó `mvnw.cmd -B -o -f target/fases-verificacion/pom.xml clean package` en una copia del proyecto. El empaquetado terminó con `BUILD SUCCESS`, 27 pruebas, 0 fallos, 0 errores y 0 omisiones. Se compiló con JDK 25 y destino Java 21. El ejecutable quedó en `target/fases-verificacion/target/escape-room-ia-0.0.1-SNAPSHOT.jar`.

La prueba de arranque que comprueba el modelo predeterminado fallaba si el proceso heredaba `OLLAMA_MODEL=llama3:latest`. Los tests de arranque ahora cargan los YAML del proyecto en un entorno aislado. La suite completa pasó con un modelo, un perfil y una URL de Ollama definidos para el proceso de Maven. Resultado de esa ejecución en la raíz del proyecto: 25 pruebas, 0 fallos, 0 errores y 0 omisiones.


## 27 de noviembre de 2026. Arquitectura Spring AI y streaming en directo

Se reemplazaron los acertijos de Java general por el Altar de los Fragmentos, el Órgano de la Memoria Efímera y la Compuerta de las Invocaciones. Cada runa conserva descubrimiento, ajuste y cierre. Java valida la elección en la fase activa. Los errores publican la runa fallida para activar la animación existente. La tarjeta de cierre presenta el diagnóstico de Spring AI con las dos extensiones del informe.

El `ChatClient` aplica un advisor de memoria por UUID. `HistorialService` escribe los mensajes que vio el jugador. Los fragmentos de `ChatClient.stream().content()` se reenvían por SSE conforme se reciben y el resultado validado viaja en su propio evento.

JUnit 5 cubre las nueve fases, opciones inválidas, pistas, victoria en ambos órdenes de recogida y el informe de cierre. Un modelo controlado emite el primer fragmento y mantiene abierto el flujo. La prueba exige recibir ese fragmento antes de permitir la respuesta completa. También comprueba que el advisor aporta la conversación anterior una sola vez y respeta el aislamiento entre partidas.

`node --check src/main/resources/static/js/cripta.js` terminó correctamente. El JAR anterior estaba en uso en la raíz. `mvnw.cmd -B -o -f target/spring-ai-verificacion/pom.xml clean package` compiló los mismos archivos con JDK 25 y destino Java 21. Resultado de la copia aislada: `BUILD SUCCESS`, 28 pruebas JUnit 5, 0 fallos, 0 errores y 0 omisiones. El ejecutable quedó en `target/spring-ai-verificacion/target/escape-room-ia-0.0.1-SNAPSHOT.jar`.

Microsoft Edge ejecutó el JAR de verificación con la instancia local de Ollama y el modelo instalado `llama3:latest`. En la pasada completa, el navegador registró 27 actualizaciones de la burbuja en 429 ms durante el turno de chat. El recorrido completó las nueve fases, comprobó que una respuesta errónea activa la sacudida sin avanzar de fase y confirmó que el informe se abre al recoger el cáliz tras desbloquear las tres runas. El botón cambió del resumen al informe extenso. Las pruebas de arranque verificaron la selección de `qwen3:8b` mediante su configuración. La inferencia de extremo a extremo correspondió a `llama3:latest`.

## 27 de noviembre de 2026. SSE en respuestas locales y configuración de Ollama

La bienvenida se solicita por SSE al iniciar y reiniciar. El servidor fragmenta los textos locales de la bienvenida, los acertijos, las pistas y el diagnóstico. El navegador reconstruye cada respuesta al leer el flujo. La hoja activa incorpora el bloque de scrollbar de `.codex-context/estilos.css` para las superficies con desplazamiento. La aplicación empaqueta únicamente la integración de Ollama.

Las pruebas JUnit 5 reconstruyen el texto de varios eventos SSE y verifican el evento final de estado. Cubren la bienvenida en español e inglés, una acción de escena, los resultados de acertijos y el informe extenso. La comparación del texto reconstruido con la bitácora y el diagnóstico también comprueba saltos de línea y caracteres acentuados.

Windows mantuvo abierto el JAR de la raíz. Se ejecutó `mvnw.cmd -B -o -f target/verificacion-sse-ed4b673d/pom.xml clean package` en una copia del código actual. El resultado fue `BUILD SUCCESS` con 28 pruebas JUnit 5, 0 fallos, 0 errores y 0 omisiones. El JAR contiene `application.yml`, `application-ollama.yml` y las dependencias de Ollama.

Chrome en modo sin interfaz observó 19 actualizaciones de la bienvenida, 6 en un intento de acertijo y 90 en el informe extenso. El ancho calculado del scrollbar fue de 7 px en el chat, la bitácora y el contenedor principal. La comparación directa confirmó que el bloque CSS coincide con el archivo de referencia.

La hoja activa conserva íntegro el CSS previo y añade el bloque de la barra gótica al final. Los botones rápidos mantienen el desplazamiento horizontal con la barra oculta. El puerto 8081 seguía sirviendo un JAR anterior. Se empaquetó la versión corregida en `target/vista-scrollbar/` y se verificó en un puerto libre que el servidor entrega exactamente el CSS corregido.

## 27 de noviembre de 2026. Progresión, victoria y voz del guardián

El motor conserva tres fases encadenadas por runa. Una opción inválida o un intento de aplicar una reliquia a una runa bloqueada identifica el objeto que debe sacudirse. La partida admite intentos ilimitados. El diagnóstico breve se publica al desbloquear las tres runas y reunir las tres reliquias. Desde la tarjeta de victoria se puede pedir la versión extensa.

El guardián recibe orientación verificada tras dos intentos sin progreso. Si el modelo no aporta texto, Java contesta con información de la escena y, en caso de digresión, alude al tema del jugador con un remate variable y un paso concreto de la prueba.

Se ejecutó `mvnw.cmd -B -o -f target/restauracion-jugable-final/pom.xml clean package`. El empaquetado terminó con `BUILD SUCCESS` y 31 pruebas JUnit 5, 0 fallos, 0 errores y 0 omisiones. La cobertura incluye cien intentos fallidos sin cierre, las fases encadenadas, ambos órdenes de victoria y la respuesta local ante un modelo sin contenido. Se verificó por hash la identidad del CSS y los archivos de configuración de Ollama con los de la entrega anterior.

En Chrome se recorrieron las nueve fases. Una opción inválida mantuvo la fase y activó la sacudida. Con las tres runas abiertas y el cáliz pendiente, el diagnóstico siguió bloqueado. Recoger el cáliz abrió el resumen y el botón mostró la versión extensa. La instancia usó el modelo local instalado `llama3:latest`. Algunas frases ambientales alcanzaron su tiempo de espera y el recorrido continuó con las respuestas locales del motor.

## 27 de noviembre de 2026. Una respuesta por turno y pistas sin soluciones

Las acciones de escena emiten directamente el texto verificado por Java. Las órdenes escritas pueden activar una herramienta con retorno directo. Las opciones e inscripciones se muestran al jugador, y Java conserva su validación. El contexto de Ollama prescinde de las inscripciones y de la bitácora completa. Las pistas describen el criterio de la prueba. El texto libre del modelo queda fuera de los eventos de chat.

El paquete se reconstruyó con `mvnw.cmd -B -o -f target/narracion-sin-soluciones/pom.xml clean package`. Resultado de JUnit 5: 33 pruebas, 0 fallos, 0 errores y 0 omisiones. Los tests simulan una respuesta que incluye relleno y revela una opción, comprueban su descarte y verifican una sola llamada cuando se ejecuta la herramienta. También comprueban que el texto permitido se fragmenta y llega antes del evento final.

En Chrome se comprobó que examinar el altar muestra un único bloque y que pedir confirmación de una opción devuelve una pista sin confirmar la opción. El recorrido completó las nueve fases, activó la sacudida con un intento inválido y abrió ambos formatos del diagnóstico tras la victoria. Se comprobó que las hojas de estilo, las vistas, JavaScript y los YAML coinciden con el paquete anterior.

## 27 de noviembre de 2026. Conversación generada por Ollama

Las preguntas, aclaraciones, peticiones de pista escritas y digresiones vuelven a usar `ChatClient.stream()`. El planteamiento conversacional contiene los conceptos de la fase activa y un resumen de los intentos sin avance. Java conserva las alternativas y oculta las etiquetas en el mensaje del jugador. Cada frase generada se revisa antes de enviarla por SSE. El filtro descarta referencias directas a opciones, cifras, repeticiones literales y exclamaciones. Si se descarta la respuesta completa, se solicita otra generación a Ollama.

Se ejecutó `mvnw.cmd -B -o -f target/conversacion-ollama-final/pom.xml clean package`. El resultado fue `BUILD SUCCESS` con 33 pruebas JUnit 5, 0 fallos, 0 errores y 0 omisiones. Las pruebas comprueban la redacción del contexto, una revelación dividida en fragmentos, la publicación de una frase antes del cierre del modelo, las aclaraciones y la variación de las digresiones. Los hashes de CSS, JavaScript, vistas y YAML coinciden con los del paquete anterior.

La prueba HTTP con el Ollama local y `llama3:latest` recibió por separado tres eventos de texto para «explica», «como así?» y una digresión sobre croquetas. Las aclaraciones variaron y la fase permaneció intacta. La digresión no volvió de forma fiable al altar y algunas analogías fueron imprecisas. El modelo predeterminado del proyecto sigue siendo `qwen3:8b`.

## 27 de noviembre de 2026. Nueve elecciones y ambientación del órgano

Examinar el altar o el órgano, o aplicar la estaca al candelabro, revela la primera inscripción. Solo una elección válida avanza de fase. Las tres inscripciones del órgano, sus pistas y el contexto de Ollama describen la memoria mediante ecos, suspiros y un canto grabado en piedra. El informe final explica la relación entre esas imágenes, la ventana de mensajes y el presupuesto del modelo.

`mvn clean package` terminó con `BUILD SUCCESS` y 34 pruebas JUnit 5, con 0 fallos, 0 errores y 0 omisiones. Las pruebas comprueban el avance y los errores en las nueve fases, el evento SSE de la runa fallida, ambos órdenes de victoria y una aclaración del órgano con un flujo de Ollama simulado. El comando se ejecutó en una copia aislada para conservar las instancias que usaban JAR anteriores bajo `target/`. El JAR verificado se copió a `target/escape-room-ia-0.0.1-SNAPSHOT.jar`. Se usó JDK 25 con destino Java 21.

En Edge se recorrieron las nueve fases y se observó la sacudida al introducir una opción incorrecta en cada una. El chat mostró el enunciado del órgano por fragmentos SSE. Ollama, con el modelo local instalado `llama3:latest`, respondió a una duda sobre el fuelle en el lenguaje de la cripta y la fase permaneció en 2/3. La victoria llegó al recoger el cáliz después de romper los tres sellos. Los hashes del CSS, JavaScript, la vista de juego, el prompt base y los YAML coinciden con los de la versión anterior.

## 27 de noviembre de 2026. Empaquetado de la entrega

`mvn clean package` se ejecutó en la raíz y terminó con `BUILD SUCCESS`. JUnit 5 ejecutó 34 pruebas con 0 fallos, 0 errores y 0 omisiones. El directorio `target/` se dejó con el JAR ejecutable.

Se comprobó el arranque de la copia de entrega desde su lanzador con el perfil `ollama`. La portada y el juego respondieron HTTP 200 en el puerto 8080. El arranque directo conserva el puerto predeterminado 8081. La comprobación HTTP del paquete y la prueba de conversación real en Edge corresponden a verificaciones distintas.
