# Referencias del prototipo de Cripta de Nocturnia

Deja los archivos en `C:/DEV/01_PROYECTOS/escape-room-ia/.codex-context/`, un nivel por encima del proyecto Maven importado en STS. Los nombres de la lista son orientativos. Si el prototipo usa otros nombres o reúne HTML, CSS y JavaScript en un solo archivo, conserva los originales y su estructura de carpetas.

1. **`index.html`** — Portada con el título y el acceso a la partida, si el prototipo incluye esa pantalla.
2. **`juego.html`** — Pantalla de juego con la cripta a la izquierda, el inventario debajo y el chat a la derecha. Incluye la posición de los indicadores de vitalidad y de progreso de los sellos, y el acceso a la bitácora.
3. **`estilos.css`** — Hoja de estilos original y sus importaciones locales. Debe recoger la paleta gótica neón (`#09070D`, `#130F1D`, `#FF5E00`, `#D90429`, `#8338EC`), la tipografía, los resplandores, las animaciones y la distribución de la interfaz.
4. **`juego.js`** — JavaScript original y sus módulos locales. Son especialmente importantes la inspección de objetos, la selección de una reliquia, su aplicación a un objeto y la reacción visible en la escena, el inventario y el chat. Si el prototipo es estático, puedes sustituir el JS por **`interacciones.md`** con las zonas clicables, los pasos y el efecto esperado de cada acción.
5. **`assets/cripta.webp`** — Imagen de fondo original de la estancia. Si la escena usa varias capas, incluye los archivos de cada capa.
6. **`assets/altar.webp`**, **`assets/cofre.webp`** y **`assets/candelabro.webp`** — Imágenes de los objetos interactivos, con transparencia y variantes de estado si existen, como cofre abierto y cerrado o candelabro encendido y apagado. Si los objetos forman parte del fondo, basta con identificar sus zonas clicables en el HTML, el JS o `interacciones.md`.
7. **`assets/sello-1.svg`**, **`assets/sello-2.svg`** y **`assets/sello-3.svg`** — Gráficos de los tres sellos y las variantes de estado que tenga el prototipo. Puedes conservar su formato original.
8. **`assets/reliquias/`** — Un archivo por icono de inventario. La correspondencia entre el archivo y la reliquia debe poder identificarse en el prototipo o en `interacciones.md`.
9. **`assets/fuentes/`** — Archivos de tipografía local referenciados por el CSS, si los hay, con los pesos y estilos que usa la interfaz.
