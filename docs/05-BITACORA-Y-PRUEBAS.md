# Bitácora y pruebas

## Inicio de la Fase 1

El trabajo parte de un proyecto Maven importado en Spring Tool Suite. La revisión inicial tiene por objeto identificar las dependencias, la configuración existente y las referencias de diseño. La fase cubre la estructura MVC, la configuración de proveedores, las sesiones en memoria, los sellos encadenados, las herramientas de Spring AI y la personalidad del guardián.

## Cobertura de las pruebas

- **Motor y sesiones.** Crear partidas independientes con UUID. Comprobar vitalidad inicial, idioma, cambios de escena, recogida y selección de reliquias, rechazo de combinaciones inválidas y pista tras el umbral de estancamiento.
- **Sello I.** Verificar los cien pesos posibles, los avisos de peso alto o bajo, el límite de siete intentos, la estrategia por punto medio y el cambio al segundo sello.
- **Sello II.** Verificar que el último elemento activado es el primero que puede apagarse y que una elección incorrecta conserva el orden pendiente. Confirmar la transición al sello final al vaciar la pila.
- **Sello III.** Verificar transiciones válidas, rechazo de pasos ilegales o ciclos y llegada al estado de victoria. Comprobar la derrota por agotamiento de vitalidad.
- **Historial.** Comprobar orden cronológico, roles, aislamiento por UUID y correspondencia entre la bitácora y los mensajes recientes de la ventana conversacional.
- **Herramientas.** Probar inspección, recogida y uso de reliquias, resolución y petición de pistas. Verificar sus contratos y el rechazo de identificadores o acciones inválidos sin iniciar Ollama ni Gemini.
- **Consultas y cierre.** Comprobar que una pregunta contextual preserva intentos y estado. Verificar la explicación breve y el acceso a la versión extensa en el desenlace.
- **API y perfiles.** Comprobar la ruta del historial, el intercambio SSE y la creación del modelo adecuado con cada perfil, sin enviar solicitudes a los proveedores.

## Incidencias resueltas

- El contexto base intentaba iniciar Gemini sin una clave. Cada perfil activa ahora solo su proveedor y el predeterminado es `ollama`. Los tres archivos usan la extensión `.yaml`.
- Spring Boot 4.1.3 gestiona JUnit 6 y su extensión de contexto usa métodos nuevos de JUnit. Para conservar JUnit 5, las pruebas de arranque abren y cierran los contextos directamente con `SpringApplicationBuilder`.
- Jackson rechazaba el campo opcional `accion` cuando se omitía en el chat. El controlador lo interpreta como `false` y la prueba de SSE cubre ese JSON.

## Evidencia de ejecución

El comando de aceptación es `mvn test`. En Windows se usó el Maven Wrapper del repositorio porque Maven no estaba disponible en el PATH.

El 25 de noviembre de 2026, `.\mvnw.cmd -B -o test` terminó con `BUILD SUCCESS`. Ejecutó 16 pruebas JUnit 5.14.2 con 0 fallos, 0 errores y 0 omisiones. Se usó JDK 25 y se compiló con `--release 21`. La ejecución en modo desconectado comprobó reglas, sesiones, historial, herramientas, SSE y arranque de los dos perfiles sin llamar a Ollama ni a Gemini.

El primer empaquetado sin red pasó las pruebas. El paso `repackage` necesitaba dependencias de Maven que todavía no estaban en caché. `.\mvnw.cmd -B -ntp clean package` las resolvió y terminó con `BUILD SUCCESS`. El resultado fue `target/escape-room-ia-0.0.1-SNAPSHOT.jar`. Se verificó su arranque con `iniciar.bat` en modo sin servidor HTTP.
