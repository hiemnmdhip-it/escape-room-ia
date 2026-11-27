# Cripta de Nocturnia

Escape room conversacional de terror sobrenatural para aprender arquitectura Spring AI. La Cripta de Nocturnia transforma la recuperación semántica, la memoria de chat y la validación de herramientas en tres runas de tres fases. Examina la sala, deduce las opciones y reúne todas las reliquias para escapar. El Diagnóstico de Arquitectura Spring AI relaciona cada reto con una aplicación Spring Boot.

Spring Boot 4.1, Java 21 y Thymeleaf sirven la portada y la partida. Java valida las acciones y conserva las sesiones. Ollama genera las explicaciones y digresiones con Spring AI. Cada frase se revisa antes de salir por SSE. Las inscripciones, los resultados del motor y el diagnóstico también se transmiten de forma progresiva. El estado del juego viaja en eventos separados.

## Requisitos

- JDK 21 y `JAVA_HOME` apuntando a su directorio. Comprueba el entorno con `java -version` y `.\mvnw.cmd -v`.
- Ollama y el modelo local `qwen3:8b`.
- Un navegador con soporte para Fetch y ReadableStream. El repositorio incluye el Maven Wrapper.

## Instalar y preparar Ollama en Windows

1. Descarga y ejecuta `OllamaSetup.exe`. Abre Ollama desde Inicio y deja que se ejecute en segundo plano. [Descarga oficial de Ollama para Windows](https://ollama.com/download/windows) · [Instalación en Windows](https://docs.ollama.com/windows).
2. Abre una terminal nueva y descarga el modelo de la cripta. `ollama ls` debe mostrar `qwen3:8b`. [Modelo Qwen3 8B](https://ollama.com/library/qwen3:8b) · [Referencia de la CLI de Ollama](https://docs.ollama.com/cli).

   ```powershell
   ollama pull qwen3:8b
   ollama ls
   ```

3. Comprueba que responde la API local. Si usas la distribución independiente, puedes iniciar el servidor en otra terminal con `ollama serve`. [API de modelos instalados](https://docs.ollama.com/api/tags).

   ```powershell
   Invoke-RestMethod 'http://localhost:11434/api/tags'
   ```

## Compilar y ejecutar en Windows

1. Ejecuta `.\mvnw.cmd clean package` desde la raíz. El comando compila para Java 21, ejecuta JUnit 5 y empaqueta el JAR en `target/`. Si ya tienes Maven en el PATH, el comando equivalente es `mvn clean package`.
2. Ejecuta `.\iniciar.bat`. El script busca el JAR junto al lanzador y, si no está allí, en `target/`. Activa el perfil `ollama`, arranca en el puerto 8080 y abre [la portada](http://localhost:8080/) cuando responde la aplicación. Deja libre ese puerto antes de arrancar y mantén abierta la consola durante la partida.

Desde la carpeta de entrega basta con ejecutar `iniciar.bat` junto al JAR. También puedes iniciar el servidor directamente. En ese caso se aplica el puerto 8081 configurado en `application.yml`.

```powershell
java -jar target/escape-room-ia-0.0.1-SNAPSHOT.jar
```

El JAR contiene el servidor web y los recursos de la interfaz. Ollama se ejecuta como un proceso local separado.

## Configuración de Ollama

La aplicación incluye siempre el perfil `ollama`. `application-ollama.yml` usa `qwen3:8b` en `http://127.0.0.1:11434`. La primera descarga se hace con `ollama pull`. Puedes indicar otro modelo local compatible con herramientas mediante `OLLAMA_MODEL`.

## Probar una partida

1. Pulsa **Entrar en la cripta**. El botón **ES/EN** del HUD cambia el idioma de la sesión.
2. Examina el Altar de los Fragmentos para revelar la Fase 1/3. Responde con una letra en las fases 1 y 3 y con un número en la fase 2. Cada acierto abre la fase siguiente. Al romper el primer sello, examina el Órgano de la Memoria Efímera. Después, usa la estaca en el candelabro para revelar la primera fase de la Compuerta de las Invocaciones.
3. Explora el cofre y selecciona una reliquia de la barra inferior. Pulsa sobre el objeto de la escena donde quieras aplicarla. Otra pulsación sobre la reliquia cancela la selección.
4. Explora y prueba combinaciones a tu ritmo. Usa **Pedir pista** para consultar la orientación del motor o escribe «¿Cómo así?» para que Ollama te explique el criterio con otro ejemplo. Los intentos de los sellos siguen disponibles si Ollama se interrumpe.
5. Abre **Bitácora** para consultar el historial y descargarlo como `.txt`. Puedes seguir explorando y preguntando sin límite de turnos. El Diagnóstico Pedagógico de Spring AI aparece al completar las tres runas y reunir todas las reliquias. Pulsa **Versión extensa** para ver los ejemplos web.

Puedes probar la API directamente desde PowerShell. El ejemplo usa el puerto 8080 del lanzador. Cambia a 8081 si arrancaste con `java -jar`.

```powershell
$partida = Invoke-RestMethod -Method Post 'http://localhost:8080/api/partidas?idioma=ES'
$base = "http://localhost:8080/api/partidas/$($partida.sessionId)"
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"INSPECCIONAR","elemento":"altar"}'
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"INSPECCIONAR","elemento":"altar"}'
Invoke-RestMethod -Method Post "$base/escena" -ContentType 'application/json' -Body '{"tipo":"PISTA"}'
Invoke-RestMethod "$base/estado"
Invoke-RestMethod "$base/historial"
```

Para el chat, envía `POST /api/partidas/{sessionId}/chat` con `{"texto":"¿Qué significa esta runa?"}` y lee la respuesta SSE. Después de escapar, puedes escribir `/diagnostico` para solicitar el informe extenso.

## Documentación

- [Visión y alcance de la versión entregada](docs/00-VISION-Y-ARQUITECTURA.md)
- [Propósito y objetivos](docs/01-PROPOSITO-Y-OBJETIVOS.md)
- [Decisiones de arquitectura](docs/02-TOMA-DE-DECISIONES.md)
- [Arquitectura y flujos](docs/03-ARQUITECTURA-Y-FLUJOS.md)
- [Rutas, herramientas y reglas del escenario](docs/04-API-TOOLS-Y-ESCENARIO.md)
- [Bitácora y pruebas](docs/05-BITACORA-Y-PRUEBAS.md)
- [Informe final de entrega](docs/INFORME-FINAL.md)
