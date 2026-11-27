# Informe final de entrega

## Proyecto

Cripta de Nocturnia es un escape room conversacional en español e inglés. El altar enseña recuperación por significado. El órgano introduce la memoria conversacional mediante el fuelle, los ecos y el canto grabado en piedra. La compuerta distingue una afirmación de una acción ejecutada y pide comprobar el resultado con un formato fijo.

Cada runa presenta tres fases. Java valida las elecciones y conserva la fase si una opción falla. La victoria exige completar las nueve fases y reunir el pergamino, la estaca y el cáliz. Al escapar se abre un diagnóstico pedagógico en versión breve o extensa.

## Implementación

El JAR incluye el servidor Spring Boot MVC y la interfaz Thymeleaf con JavaScript. Las sesiones, la bitácora y la ventana de chat residen en memoria. Java ejecuta las reglas y transmite sus textos progresivamente por SSE. Ollama genera la conversación mediante Spring AI y las frases aprobadas llegan por el mismo canal. El navegador recibe el estado en eventos independientes.

El altar presenta datos didácticos de similitud. La compuerta plantea un veredicto estructurado como ejercicio. El diagnóstico explica cómo implementar estas técnicas en una aplicación Spring Boot.

## Compilación y verificación

- `mvn clean package` terminó con `BUILD SUCCESS` y 34 pruebas JUnit 5, sin fallos, errores ni omisiones. La cobertura del flujo del narrador incluyó un proveedor simulado.
- En Edge se comprobaron las nueve fases, la sacudida ante una elección incorrecta en cada fase, el texto progresivo del órgano y la victoria al recoger el cáliz después de abrir los sellos. La prueba conversacional en ese entorno usó Ollama con `llama3:latest`.
- El lanzador de la entrega arrancó el JAR copiado junto al script con el perfil `ollama`. La portada y la pantalla de juego respondieron HTTP 200 en el puerto 8080.

## Puesta en marcha

Instala Java 21 o posterior y prepara Ollama según el [README](../README.md). Ejecuta `iniciar.bat` dentro de la entrega. El lanzador abre [la portada](http://localhost:8080/). Si arrancas el JAR directamente con Java, el puerto predeterminado es 8081.

Consulta la [arquitectura](03-ARQUITECTURA-Y-FLUJOS.md), el [contrato de la API](04-API-TOOLS-Y-ESCENARIO.md) y la [bitácora de pruebas](05-BITACORA-Y-PRUEBAS.md).
