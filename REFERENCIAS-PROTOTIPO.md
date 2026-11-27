# Referencias del prototipo de Cripta de Nocturnia

La escena jugable toma como referencia el SVG y los estilos de `.codex-context/index.html` y `.codex-context/estilos.css`. La portada parte del prototipo incluido en `.codex-context/cripta-nocturnia-prototipo/`.

Las vistas activas son `src/main/resources/templates/inicio.html` y `src/main/resources/templates/juego.html`. La interfaz usa `static/css/cripta.css` y `static/js/cripta.js` bajo `src/main/resources/`. Los recursos visuales se sirven desde `static/assets/`.

La escena permite examinar el altar, el órgano, el candelabro y el cofre. Los rótulos muestran el progreso de los sellos. Las reliquias permiten combinar objetos con el entorno y el chat recibe la narración y el estado confirmado por Java.

Consulta la [integración de la escena](docs/03-ARQUITECTURA-Y-FLUJOS.md#integración-de-la-escena) y las [reglas vigentes](docs/04-API-TOOLS-Y-ESCENARIO.md).
