# Cripta de Nocturnia

Escape room conversacional de terror sobrenatural y pensamiento algorítmico. El jugador calibra una balanza, apaga runas en orden inverso y cruza una red de cámaras. Resuelve los tres sellos mediante deducción. El desenlace relaciona las decisiones del jugador con Java y con aplicaciones web reales.

La Fase 1 entrega un motor en memoria, una API Spring MVC y un narrador Spring AI con streaming SSE. El servidor valida las acciones y conserva la bitácora. La integración con las vistas Thymeleaf y la escena del prototipo corresponde a la fase de interfaz.

## Requisitos

- JDK 21, con `JAVA_HOME` apuntando al JDK. El proyecto incluye Maven Wrapper.
- Para el perfil local, Ollama iniciado y un modelo compatible con herramientas. El modelo por defecto es `qwen3:8b`. Se instala con `ollama pull qwen3:8b`.
- Para el perfil remoto, una clave de Gemini en la variable de entorno `GEMINI_API_KEY`.

## Compilar y ejecutar en Windows

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\iniciar.bat
```

El script lanza el mismo JAR que `java -jar target/escape-room-ia-0.0.1-SNAPSHOT.jar`. El perfil por defecto es `ollama` y su configuración está en `application-ollama.yaml`. Usa `OLLAMA_BASE_URL` y `OLLAMA_MODEL` para cambiar la conexión y el modelo.

Para Gemini, configura `GEMINI_API_KEY` en el entorno antes de ejecutar `.\iniciar.bat --spring.profiles.active=gemini`. El perfil usa `application-gemini.yaml` y permite cambiar el modelo con `GEMINI_MODEL`.

## Probar una partida

```powershell
$partida = Invoke-RestMethod -Method Post 'http://localhost:8080/api/partidas?idioma=ES'
$base = "http://localhost:8080/api/partidas/$($partida.sessionId)"
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"INSPECCIONAR","elemento":"altar"}'
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"RESOLVER","sello":1,"intento":"50"}'
Invoke-RestMethod "$base/historial"
```

Para el chat, envía `POST /api/partidas/{sessionId}/chat` con `{"texto":"¿Qué significa esta runa?"}` y lee la respuesta como SSE. Tras victoria o derrota, usa `/diagnostico` en el chat para solicitar el informe extenso. Consulta el [contrato de la API, herramientas y escenario](docs/04-API-TOOLS-Y-ESCENARIO.md).

## Documentación

- [Visión del proyecto](docs/00-VISION-Y-ARQUITECTURA.md)
- [Propósito y objetivos](docs/01-PROPOSITO-Y-OBJETIVOS.md)
- [Decisiones de arquitectura](docs/02-TOMA-DE-DECISIONES.md)
- [Arquitectura y flujos](docs/03-ARQUITECTURA-Y-FLUJOS.md)
- [Bitácora y pruebas](docs/05-BITACORA-Y-PRUEBAS.md)
