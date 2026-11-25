# Bitácora y pruebas

## Fase 1. Motor, memoria y herramientas

El trabajo partió de un proyecto Maven importado en Spring Tool Suite. La revisión inicial identificó las dependencias, la configuración y las referencias de diseño. La fase incorporó los paquetes MVC, la selección inicial de proveedores, las sesiones en memoria, los sellos encadenados, las herramientas de Spring AI y la personalidad del guardián.

### Cobertura inicial

- **Motor y sesiones.** Crear partidas independientes con UUID. Comprobar vitalidad inicial, idioma, cambios de escena, recogida y selección de reliquias, rechazo de combinaciones inválidas y pista tras el umbral de estancamiento.
- **Sello I.** Verificar los cien pesos posibles, los avisos de peso alto o bajo, el límite de siete intentos, la estrategia por punto medio y el cambio al segundo sello.
- **Sello II.** Verificar que el último elemento activado es el primero que puede apagarse y que una elección incorrecta conserva el orden pendiente. Confirmar la transición al sello final al vaciar la pila.
- **Sello III.** Verificar transiciones válidas, rechazo de pasos ilegales o ciclos y llegada al estado de victoria. Comprobar la derrota por agotamiento de vitalidad.
- **Historial.** Comprobar orden cronológico, roles, aislamiento por UUID y correspondencia entre la bitácora y los mensajes recientes de la ventana conversacional.
- **Herramientas.** Probar inspección, recogida y uso de reliquias, resolución y petición de pistas. Verificar sus contratos y el rechazo de identificadores o acciones inválidos sin iniciar Ollama ni Gemini.
- **Consultas y cierre.** Comprobar que una pregunta contextual preserva intentos y estado. Verificar la explicación breve y el acceso a la versión extensa en el desenlace.
- **API y perfiles.** Comprobar la ruta del historial, el intercambio SSE y la creación del modelo adecuado con cada perfil, sin enviar solicitudes a los proveedores.

### Incidencias resueltas

- El contexto base intentaba iniciar Gemini sin una clave. En la Fase 1 se aisló la activación por perfil y se eligió `ollama` como predeterminado. La Fase 2 sustituyó esa configuración por una única integración local.
- Spring Boot 4.1.3 gestiona JUnit 6 y su extensión de contexto usa métodos nuevos de JUnit. Para conservar JUnit 5, las pruebas de arranque abren y cierran los contextos directamente con `SpringApplicationBuilder`.
- Jackson rechazaba el campo opcional `accion` cuando se omitía en el chat. El controlador lo interpreta como `false` y la prueba de SSE cubre ese JSON.

### Evidencia de ejecución

El comando de aceptación es `mvn test`. En Windows se usó el Maven Wrapper del repositorio porque Maven no estaba disponible en el PATH.

El 25 de noviembre de 2026, `.\mvnw.cmd -B -o test` terminó con `BUILD SUCCESS`. Ejecutó 16 pruebas JUnit 5.14.2 con 0 fallos, 0 errores y 0 omisiones. Se usó JDK 25 y se compiló con `--release 21`. La ejecución en modo desconectado comprobó reglas, sesiones, historial, herramientas, SSE y arranque de los dos perfiles sin llamar a Ollama ni a Gemini.

El primer empaquetado sin red pasó las pruebas. El paso `repackage` necesitaba dependencias de Maven que todavía no estaban en caché. `.\mvnw.cmd -B -ntp clean package` las resolvió y terminó con `BUILD SUCCESS`. El resultado fue `target/escape-room-ia-0.0.1-SNAPSHOT.jar`. Se verificó su arranque con `iniciar.bat` en modo sin servidor HTTP.

## Fase 2. Ollama local e integración MVC

La Fase 2 consolida Ollama como único proveedor. La configuración carga el modelo local sin claves de servicio y con la descarga automática desactivada. Se añadieron las rutas de portada y partida en Thymeleaf y los contratos REST para reinicio, sincronización del estado y diagnóstico. El canal SSE admite mensajes escritos y acciones estructuradas de la escena. Java aplica las reglas de juego y Ollama genera la narración.

### Cobertura de regresión

- **Vistas y sesiones.** Selección ES/EN y arranque en el idioma elegido. Estado inicial con UUID, vitalidad, lecturas y sellos. Reinicio con UUID nuevo y acceso al historial anterior.
- **Configuración de Ollama.** El contexto de Spring expone un único modelo de chat, incluso cuando se activa un perfil adicional. Esta comprobación inicia Spring sin pedir inferencia al modelo.
- **Turnos SSE.** Consumo de eventos de texto, sincronización con el evento de estado y validación del cuerpo HTTP. Una acción de escena y un mensaje de chat comparten el transporte y alimentan la bitácora. La prueba unitaria de ambientación comprueba que el narrador recibe el resultado validado y conserva la instantánea de Java.
- **Historial en memoria.** Orden de las entradas, correspondencia con la ventana reciente y aislamiento entre partidas, incluida la creada al reiniciar.
- **Diagnóstico de Java.** Acceso al finalizar, por defecto en versión resumida y con opción extensa. Las dos versiones explican la búsqueda binaria, la pila LIFO y la máquina de estados.

### Evidencia de ejecución

El 25 de noviembre de 2026, `.\mvnw.cmd -B -o test` terminó con `BUILD SUCCESS`. Ejecutó 22 pruebas JUnit 5 con 0 fallos, 0 errores y 0 omisiones. El entorno usó JDK 25 con compilación para Java 21 mediante `--release 21`.

La primera prueba HTTP del JAR confirmó las vistas, los eventos SSE del motor, el estado y el historial. El primer fragmento generado por Ollama superó el tiempo de espera inicial. El servicio local disponía de `llama3:latest`. Una consulta directa con ese modelo tardó 43,7 segundos en procesar un mensaje mínimo en frío. Se acortó la petición de ambientación de la escena y se amplió la espera de lectura a 90 segundos entre fragmentos. El emisor SSE pasó a admitir 120 segundos por petición.

La ejecución final de `.\mvnw.cmd -B -o clean package` terminó con `BUILD SUCCESS` y 23 pruebas JUnit 5, con 0 fallos, 0 errores y 0 omisiones. El empaquetado se ejecutó en una copia aislada porque otro proceso Java tenía abierto el JAR anterior. El JAR de verificación quedó en `target/fase2-verificacion/target/escape-room-ia-0.0.1-SNAPSHOT.jar`.

La prueba HTTP final del JAR se ejecutó con `llama3:latest` en Ollama local. La portada, la vista inglesa y el JavaScript respondieron con HTTP 200. Recoger el cáliz emitió 36 eventos de texto y el estado final por SSE. El nuevo inventario coincidió con la consulta de estado y la acción quedó registrada en el historial. Una pregunta de chat obtuvo narración de Ollama y conservó la vitalidad. El reinicio devolvió otro UUID, el idioma inglés y el estado inicial.

La inspección del JAR identificó las dos configuraciones YAML y la integración Spring AI de Ollama. La prueba de configuración comprobó `qwen3:8b` como modelo predeterminado. La prueba HTTP usó el modelo ya instalado en el equipo.
