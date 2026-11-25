# Cripta de Nocturnia

Escape room conversacional de terror sobrenatural y pensamiento algorítmico. El jugador calibra una balanza, apaga runas en orden inverso y cruza una red de cámaras. Resuelve los tres sellos mediante deducción. El desenlace relaciona las decisiones del jugador con Java y con aplicaciones web reales.

Spring MVC y Thymeleaf sirven la portada y la partida. El panel de la cripta refleja el estado validado por Java y recibe, junto con el chat, las respuestas de cada turno por SSE. Spring AI conecta el narrador con Ollama local. El servidor conserva el progreso y la bitácora en memoria.

## Requisitos

- JDK 21, con `JAVA_HOME` apuntando al JDK. El proyecto incluye Maven Wrapper.
- Ollama iniciado en el equipo con `qwen3:8b` disponible. El comando `ollama pull qwen3:8b` prepara el modelo antes de la primera partida.

## Compilar y ejecutar en Windows

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\iniciar.bat
```

El script lanza el mismo JAR que `java -jar target/escape-room-ia-0.0.1-SNAPSHOT.jar`. El perfil `ollama` se incluye automáticamente. `application-ollama.yaml` configura la inferencia en `127.0.0.1:11434` y desactiva la descarga automática de modelos. La aplicación funciona sin claves de API.

## Probar una partida

Abre `http://localhost:8080/` y elige español o inglés. Inspecciona el altar, introduce un peso en el campo de intento y sigue las pistas del chat. Puedes seleccionar una reliquia, aplicarla a un objeto y descargar la bitácora desde el encabezado. Tras una victoria o derrota, el chat muestra el resumen pedagógico y habilita el botón para leer el informe extenso.

Para probar la API directamente:

```powershell
$partida = Invoke-RestMethod -Method Post 'http://localhost:8080/api/partidas?idioma=ES'
$base = "http://localhost:8080/api/partidas/$($partida.sessionId)"
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"INSPECCIONAR","elemento":"altar"}'
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"RESOLVER","sello":1,"intento":"50"}'
Invoke-RestMethod "$base/estado"
Invoke-RestMethod "$base/historial"
```

Para el chat, envía `POST /api/partidas/{sessionId}/chat` con `{"texto":"¿Qué significa esta runa?"}` y lee la respuesta como SSE. Tras victoria o derrota, usa `/diagnostico` en el chat para solicitar el informe extenso. Consulta el [contrato de la API, herramientas y escenario](docs/04-API-TOOLS-Y-ESCENARIO.md).

## Documentación

- [Visión del proyecto](docs/00-VISION-Y-ARQUITECTURA.md)
- [Propósito y objetivos](docs/01-PROPOSITO-Y-OBJETIVOS.md)
- [Decisiones de arquitectura](docs/02-TOMA-DE-DECISIONES.md)
- [Arquitectura y flujos](docs/03-ARQUITECTURA-Y-FLUJOS.md)
- [Bitácora y pruebas](docs/05-BITACORA-Y-PRUEBAS.md)
