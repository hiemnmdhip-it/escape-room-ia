'use strict';

const textos = {
    ES: {
        vitalidad: 'Vitalidad', sellos: 'Sellos', bitacora: 'Bitácora', reiniciar: 'Reiniciar',
        cripta: 'Cámara del Altar Carmesí', altar: 'Altar de Sangre', organ: 'Órgano de Runas',
        chandelier: 'Candelabro', chest: 'Cofre de Obsidiana', scroll: 'Pergamino rúnico',
        stake: 'Estaca de plata', chalice: 'Cáliz de ceniza', reliquias: 'Reliquias',
        ayudaEscena: 'Selecciona una reliquia y pulsa el objeto sobre el que quieras usarla.',
        recoger: 'Recoger cáliz', pista: 'Pedir pista', probar: 'Probar', entidad: 'Entidad de la Cripta',
        mensaje: 'Pregunta o indica qué quieres hacer', jugador: 'Jugador', enviar: 'Enviar', diagnostico: 'Ver informe extenso',
        cerrar: 'Cerrar', descargar: 'Descargar .txt', READY: 'Activo', LOCKED: 'Bloqueado',
        SOLVED: 'Resuelto', CLOSED: 'Cerrado', OPEN: 'Abierto', VICTORY: 'Has escapado', DEFEAT: 'Partida terminada',
        intento: 'Intento', intentos: ['Peso', 'Runa', 'Cámara'], espera: 'La cripta responde…',
        error: 'Se interrumpió la comunicación con la cripta.', vacia: 'La bitácora está vacía.',
        lecturas: (min, max, n) => `Balanza · ${min}–${max} · ${n} lecturas restantes`,
        camara: (actual, visitas) => `Cámara ${actual} · Visitadas: ${visitas}`
    },
    EN: {
        vitalidad: 'Vitality', sellos: 'Seals', bitacora: 'Log', reiniciar: 'Restart',
        cripta: 'Crimson Altar Chamber', altar: 'Blood Altar', organ: 'Rune Organ',
        chandelier: 'Chandelier', chest: 'Obsidian Chest', scroll: 'Rune scroll',
        stake: 'Silver stake', chalice: 'Ash chalice', reliquias: 'Relics',
        ayudaEscena: 'Select a relic, then click the object you want to use it on.',
        recoger: 'Collect chalice', pista: 'Ask for a hint', probar: 'Try', entidad: 'Entity of the Crypt',
        mensaje: 'Ask a question or say what you want to do', jugador: 'Player', enviar: 'Send', diagnostico: 'Read the full report',
        cerrar: 'Close', descargar: 'Download .txt', READY: 'Active', LOCKED: 'Locked',
        SOLVED: 'Solved', CLOSED: 'Closed', OPEN: 'Open', VICTORY: 'You escaped', DEFEAT: 'Game over',
        intento: 'Attempt', intentos: ['Weight', 'Rune', 'Chamber'], espera: 'The crypt is responding…',
        error: 'Communication with the crypt was interrupted.', vacia: 'The log is empty.',
        lecturas: (min, max, n) => `Scales · ${min}–${max} · ${n} readings left`,
        camara: (actual, visitas) => `Chamber ${actual} · Visited: ${visitas}`
    }
};

const porId = id => document.getElementById(id);
const base = document.body.dataset.api;
const idioma = document.body.dataset.idioma;
const traduccion = textos[idioma];
const objetos = [...document.querySelectorAll('[data-objeto]')];
const mensajes = porId('mensajes');
const estadoTurno = porId('estado-turno');
const dialogo = porId('dialogo-bitacora');
let estado;
let ocupado = false;

for (const etiqueta of document.querySelectorAll('[data-texto]')) {
    etiqueta.textContent = traduccion[etiqueta.dataset.texto];
}
porId('texto').placeholder = traduccion.mensaje;

function dibujar() {
    const finalizada = estado && estado.resultado !== 'PLAYING';
    porId('chat-form').querySelector('button').disabled = ocupado || !estado;
    porId('reiniciar').disabled = ocupado;
    porId('bitacora').disabled = ocupado || !estado;
    porId('pista').disabled = ocupado || !estado || finalizada;
    porId('recoger').disabled = ocupado || !estado || finalizada || estado.escena.chest === 'OPEN';
    porId('sello-form').hidden = !estado || finalizada;
    porId('sello-form').querySelector('button').disabled = ocupado;
    porId('diagnostico').hidden = !finalizada;
    porId('diagnostico').disabled = ocupado;
    for (const boton of objetos) {
        boton.disabled = ocupado || !estado;
        if (estado) {
            boton.dataset.estado = estado.escena[boton.dataset.objeto];
            boton.querySelector('small').textContent = traduccion[boton.dataset.estado];
        }
    }
    if (!estado) return;
    porId('vitalidad').textContent = `${estado.vitalidad}/10`;
    porId('sellos').textContent = `${estado.sellosResueltos}/3`;
    porId('etiqueta-intento').textContent = traduccion.intentos[estado.sellosResueltos] ?? traduccion.intento;
    const {minimo, maximo, intentosRestantes} = estado.calibracion;
    porId('calibracion').textContent = finalizada ? traduccion[estado.resultado]
        : estado.camara ? traduccion.camara(estado.camara, [...estado.visitadas].sort().join(', '))
            : estado.sellosResueltos === 0 ? traduccion.lecturas(minimo, maximo, intentosRestantes) : '';
    porId('inventario').replaceChildren(...estado.inventario.map(reliquia => {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.textContent = traduccion[reliquia];
        boton.setAttribute('aria-pressed', String(estado.reliquiaSeleccionada === reliquia));
        boton.disabled = ocupado || finalizada;
        boton.addEventListener('click', () => turno({escena: {tipo: 'SELECCIONAR', reliquia}}, boton.textContent));
        return boton;
    }));
}

async function solicitar(url, opciones) {
    const respuesta = await fetch(url, opciones);
    if (!respuesta.ok) throw new Error(String(respuesta.status));
    return respuesta;
}

function ruta(sufijo) {
    return `${base}/${estado.sessionId}/${sufijo}`;
}

async function operar(operacion) {
    if (ocupado) return;
    ocupado = true;
    estadoTurno.textContent = traduccion.espera;
    dibujar();
    try {
        await operacion();
        estadoTurno.textContent = '';
    } catch {
        estadoTurno.textContent = traduccion.error;
    } finally {
        ocupado = false;
        dibujar();
    }
}

function burbuja(autor, texto = '') {
    const bloque = document.createElement('p');
    bloque.className = 'burbuja';
    bloque.dataset.autor = autor;
    bloque.textContent = texto;
    mensajes.append(bloque);
    mensajes.scrollTop = mensajes.scrollHeight;
    return bloque;
}

async function leerEventos(respuesta, recibir) {
    const lector = respuesta.body.pipeThrough(new TextDecoderStream()).getReader();
    let pendiente = '';
    try {
        while (true) {
            const {value, done} = await lector.read();
            if (done) break;
            pendiente += value;
            let fin = pendiente.indexOf('\n\n');
            while (fin !== -1) {
                const lineas = pendiente.slice(0, fin).split('\n');
                const evento = lineas.find(linea => linea.startsWith('event:'))?.slice(6).trim();
                const datos = lineas.filter(linea => linea.startsWith('data:'))
                    .map(linea => linea.slice(5).replace(/^ /, '')).join('\n');
                recibir(evento, datos);
                pendiente = pendiente.slice(fin + 2);
                fin = pendiente.indexOf('\n\n');
            }
        }
    } finally {
        await lector.cancel();
    }
}

function turno(entrada, descripcion) {
    return operar(async () => {
        burbuja('jugador', descripcion);
        let narracion;
        let confirmado = false;
        try {
            const respuesta = await solicitar(ruta('chat'), {
                method: 'POST',
                headers: {'Accept': 'text/event-stream', 'Content-Type': 'application/json'},
                body: JSON.stringify(entrada)
            });
            await leerEventos(respuesta, (evento, datos) => {
                if (evento === 'texto') {
                    narracion ??= burbuja('narrador');
                    narracion.textContent += datos;
                    mensajes.scrollTop = mensajes.scrollHeight;
                }
                if (evento === 'estado') {
                    const resultado = JSON.parse(datos);
                    estado = resultado.estado;
                    narracion ??= burbuja('narrador');
                    narracion.textContent = resultado.mensaje;
                    confirmado = true;
                    dibujar();
                    mensajes.scrollTop = mensajes.scrollHeight;
                }
            });
            if (!confirmado) throw new Error('SSE');
        } catch (error) {
            try {
                estado = await (await solicitar(ruta('estado'))).json();
            } finally {
                throw error;
            }
        }
    });
}

function iniciar() {
    return operar(async () => {
        const url = estado ? ruta('reiniciar') : `${base}?idioma=${idioma}`;
        estado = await (await solicitar(url, {method: 'POST'})).json();
        mensajes.replaceChildren();
        porId('intento').value = '';
        porId('texto').value = '';
    });
}

for (const boton of objetos) {
    boton.addEventListener('click', () => {
        const elemento = boton.dataset.objeto;
        const reliquia = estado.reliquiaSeleccionada;
        boton.classList.add('activado');
        setTimeout(() => boton.classList.remove('activado'), 550);
        const escena = reliquia ? {tipo: 'USAR', elemento, reliquia} : {tipo: 'INSPECCIONAR', elemento};
        turno({escena}, reliquia ? `${traduccion[reliquia]} → ${traduccion[elemento]}` : traduccion[elemento]);
    });
}

porId('recoger').addEventListener('click', () => turno(
    {escena: {tipo: 'RECOGER', reliquia: 'chalice'}}, traduccion.recoger));
porId('pista').addEventListener('click', () => turno({escena: {tipo: 'PISTA'}}, traduccion.pista));
porId('reiniciar').addEventListener('click', iniciar);
porId('sello-form').addEventListener('submit', evento => {
    evento.preventDefault();
    const campo = porId('intento');
    const intento = campo.value.trim();
    if (ocupado || !estado || estado.resultado !== 'PLAYING' || !intento) return;
    campo.value = '';
    turno({escena: {tipo: 'RESOLVER', sello: estado.sellosResueltos + 1, intento}}, intento);
});
porId('chat-form').addEventListener('submit', evento => {
    evento.preventDefault();
    const campo = porId('texto');
    const texto = campo.value.trim();
    if (ocupado || !estado || !texto) return;
    campo.value = '';
    turno({texto, accion: estado.resultado === 'PLAYING'}, texto);
});
porId('diagnostico').addEventListener('click', () => turno({texto: '/diagnostico'}, '/diagnostico'));
porId('bitacora').addEventListener('click', () => operar(async () => {
    const historial = await (await solicitar(ruta('historial'))).json();
    const registro = historial.map(({fecha, autor, texto}) => {
        const hora = new Date(fecha).toLocaleTimeString(document.documentElement.lang);
        return `[${hora}] ${autor === 'JUGADOR' ? traduccion.jugador : traduccion.entidad}\n${texto}`;
    }).join('\n\n');
    porId('contenido-bitacora').textContent = registro || traduccion.vacia;
    porId('descargar').disabled = !registro;
    dialogo.showModal();
}));
porId('descargar').addEventListener('click', () => {
    const url = URL.createObjectURL(new Blob([porId('contenido-bitacora').textContent], {type: 'text/plain;charset=utf-8'}));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'bitacora-cripta-nocturnia.txt';
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
});

iniciar();
