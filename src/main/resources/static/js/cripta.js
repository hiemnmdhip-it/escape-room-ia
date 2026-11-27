(() => {
  'use strict';

  const $ = selector => document.querySelector(selector);
  const $$ = selector => Array.from(document.querySelectorAll(selector));
  const messages = $('#messages');
  const chatInput = $('#chatInput');
  const flash = $('#flash');
  const logModal = $('#logModal');
  const logText = $('#logText');
  const base = document.body.dataset.api;
  const objects = $$('.scene-object');
  let state;
  let lang = document.body.dataset.idioma.toLowerCase();
  let busy = false;
  let failed = false;
  let report;

  const i18n = {
    es: {
      title:'CRIPTA DE NOCTURNIA // EL SELLO DE SANGRE', subtitle:'SESIÓN: CÁMARA DEL ALTAR CARMESÍ', progress:'AVANCE DE LA CRIPTA', seals:'SELLOS ROTOS', log:'Bitácora', reset:'Reiniciar', relics:'RELIQUIAS', scroll:'Pergamino Rúnico', stake:'Estaca de Plata', empty:'[ Vacío ]', entity:'ENTIDAD DE LA CRIPTA', entitySub:'CANAL RÚNICO ESTABLE', inspect:'Inspeccionar sala', hint:'Pedir pista', send:'ENVIAR', logTitle:'BITÁCORA DE LA CRIPTA', logNote:'Registro de la sesión actual.', download:'Descargar .txt', placeholder:'Escribe qué quieres hacer...', sceneTitle:'LA CÁMARA DEL ALTAR CARMESÍ<small>4 PUNTOS DE INTERÉS DETECTADOS</small>', selected:'Reliquia seleccionada', cleared:'Selección cancelada', obtained:'Reliquia obtenida'
    },
    en: {
      title:'CRYPT OF NOCTURNIA // THE BLOOD SEAL', subtitle:'SESSION: CRIMSON ALTAR CHAMBER', progress:'CRYPT PROGRESS', seals:'BROKEN SEALS', log:'Journal', reset:'Restart', relics:'RELICS', scroll:'Runic Scroll', stake:'Silver Stake', empty:'[ Empty ]', entity:'CRYPT ENTITY', entitySub:'RUNIC CHANNEL STABLE', inspect:'Inspect room', hint:'Ask for a hint', send:'SEND', logTitle:'CRYPT JOURNAL', logNote:'Record of the current session.', download:'Download .txt', placeholder:'Type what you want to do...', sceneTitle:'THE CRIMSON ALTAR CHAMBER<small>4 POINTS OF INTEREST DETECTED</small>', selected:'Relic selected', cleared:'Selection cleared', obtained:'Relic obtained'
    }
  };

  const objectNames = {
    es:{chandelier:'Candelabro de Llamas Naranjas', altar:'Altar con Balanza de Sangre', organ:'Órgano de Tubos Demoníaco', chest:'Cofre de Obsidiana'},
    en:{chandelier:'Orange-Flame Chandelier', altar:'Blood-Scale Altar', organ:'Demonic Pipe Organ', chest:'Obsidian Chest'}
  };
  const itemNames = {
    es:{scroll:'Pergamino Rúnico', stake:'Estaca de Plata', chalice:'Cáliz de Ceniza'},
    en:{scroll:'Runic Scroll', stake:'Silver Stake', chalice:'Chalice of Ash'}
  };
  i18n.es.chalice = itemNames.es.chalice;
  i18n.en.chalice = itemNames.en.chalice;

  const labels = {
    es: {
      online: '● EN LÍNEA', waiting: '● ESCUCHANDO', error: '● CANAL INTERRUMPIDO',
      report: 'Ver diagnóstico',
      won: 'SESIÓN SUPERADA',
      summary: 'Versión resumida', full: 'Versión extensa', close: 'Cerrar', message: 'Mensaje',
      diagnosis: 'DIAGNÓSTICO PEDAGÓGICO DE SPRING AI', entity: 'ENTIDAD', player: 'TÚ',
      inspect: 'Examinar', use: 'Usar', ready: 'LISTO', locked: 'SELLADO', closed: 'CERRADO',
      open: 'ABIERTO', phase: 'Fase', unlocked: 'Desbloqueada',
      interaction: 'Haz clic en un objeto para examinarlo. Selecciona una reliquia y luego un objeto para usarla.',
      active: 'Reliquia activa', target: 'Ahora elige un elemento de la sala.',
      objectTags: ['CANDELABRO', 'ALTAR DE SANGRE', 'ÓRGANO DEMONÍACO', 'COFRE DE OBSIDIANA']
    },
    en: {
      online: '● ONLINE', waiting: '● LISTENING', error: '● CHANNEL INTERRUPTED',
      report: 'View report',
      won: 'SESSION CLEARED',
      summary: 'Summary version', full: 'Extended version', close: 'Close', message: 'Message',
      diagnosis: 'SPRING AI LEARNING REPORT', entity: 'ENTITY', player: 'YOU',
      inspect: 'Inspect', use: 'Use', ready: 'READY', locked: 'SEALED', closed: 'CLOSED',
      open: 'OPEN', phase: 'Phase', unlocked: 'Unlocked',
      interaction: 'Click an object to inspect it. Select a relic, then a room object to use it.',
      active: 'Active relic', target: 'Now choose a room object.',
      objectTags: ['CHANDELIER', 'BLOOD ALTAR', 'DEMONIC ORGAN', 'OBSIDIAN CHEST']
    }
  };
  const t = () => labels[lang];
  const playing = () => state?.resultado === 'PLAYING';
  const scroll = () => { messages.scrollTop = messages.scrollHeight; };

  function translate() {
    document.documentElement.lang = lang;
    $('#langBtn').textContent = lang.toUpperCase();
    $$('[data-i18n]').forEach(node => { node.textContent = i18n[lang][node.dataset.i18n]; });
    $('#sceneWrap .scene-title').innerHTML = i18n[lang].sceneTitle;
    chatInput.placeholder = i18n[lang].placeholder;
    chatInput.setAttribute('aria-label', t().message);
    $('#closeModal').setAttribute('aria-label', t().close);
    objects.forEach((node, index) => {
      node.setAttribute('aria-label', objectNames[lang][node.dataset.object]);
      node.querySelector('.object-tag').textContent = `[${index + 1}] ${t().objectTags[index]}`;
    });
  }

  function draw() {
    $('#statusText').textContent = failed ? t().error : busy ? t().waiting : t().online;
    $('#resetBtn').disabled = busy;
    $('#langBtn').disabled = busy || !state;
    $('#logBtn').disabled = busy || !state;
    $('#chatForm button').disabled = busy || !state;
    $$('.chip').forEach(button => { button.disabled = busy || !state; });
    $('[data-quick="hint"]').disabled ||= !playing();
    const reportButton = $('[data-quick="report"]');
    reportButton.disabled ||= state?.resultado !== 'VICTORY';
    reportButton.textContent = t().report;
    if (!state) return;

    const count = state.sellosResueltos;
    const percent = Math.round(count * 100 / 3);
    $('#progressText').textContent = `${percent} %`;
    $('#energyFill').style.width = `${percent}%`;
    $('#sealCount').textContent = `${count} / 3`;
    objects.forEach(node => {
      const id = node.dataset.object;
      const solved = state.escena[id] === 'SOLVED';
      const phase = state.fasesRunas[id];
      node.classList.toggle('solved', solved);
      const label = phase ? (solved ? t().unlocked : `${t().phase} ${phase}/3`)
        : state.escena.chest === 'OPEN' ? t().open : t().closed;
      $('#state-' + id).textContent = label;
      node.setAttribute('aria-label', `${objectNames[lang][id]}. ${label}`);
    });
    $('#obj-chest').classList.toggle('open', state.escena.chest === 'OPEN');
    const chalice = $('[data-item="chalice"]');
    const acquired = state.inventario.includes('chalice');
    chalice.classList.toggle('new-item', acquired && chalice.classList.contains('empty'));
    chalice.classList.toggle('empty', !acquired);
    chalice.querySelector('div').hidden = !acquired;
    chalice.querySelector('.empty-label').hidden = acquired;
    $$('[data-item]').forEach(slot => {
      const item = slot.dataset.item;
      const selected = state.reliquiaSeleccionada === item;
      slot.disabled = busy || !playing() || !state.inventario.includes(item);
      slot.classList.toggle('selected', selected);
      slot.setAttribute('aria-pressed', String(selected));
      slot.title = itemNames[lang][item];
    });
    $('#interactionHint').textContent = state.reliquiaSeleccionada
      ? `${t().active}: ${itemNames[lang][state.reliquiaSeleccionada]}. ${t().target}`
      : t().interaction;
  }

  async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(String(response.status));
    return response;
  }

  const route = path => `${base}/${state.sessionId}/${path}`;

  async function run(task) {
    if (busy) return;
    busy = true;
    failed = false;
    draw();
    try {
      await task();
    } catch (error) {
      failed = true;
      console.error(error);
    } finally {
      busy = false;
      draw();
    }
  }

  function message(role, text = '') {
    const row = document.createElement('div');
    row.className = `msg ${role}`;
    if (role !== 'system') {
      const meta = document.createElement('div');
      meta.className = 'meta';
      meta.textContent = `${role === 'narrator' ? t().entity : t().player} // ${new Date().toLocaleTimeString(lang, {
        hour: '2-digit', minute: '2-digit'
      })}`;
      row.append(meta);
    }
    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    bubble.textContent = text;
    row.append(bubble);
    messages.append(row);
    scroll();
    return bubble;
  }

  async function leerEventos(respuesta, recibir) {
      const lector = respuesta.body.pipeThrough(new TextDecoderStream()).getReader();
      let pendiente = '';
      try {
          while (true) {
              const {value, done} = await lector.read();
              if (done) break;
              pendiente += value;
              let separador;
              while ((separador = /\r?\n\r?\n/.exec(pendiente))) {
                  const lineas = pendiente.slice(0, separador.index).split(/\r?\n/);
                  const evento = lineas.find(linea => linea.startsWith('event:'))?.slice(6).trim();
                  const datos = lineas.filter(linea => linea.startsWith('data:'))
                      .map(linea => linea.slice(5).replace(/^ /, '')).join('\n');
                  if (evento) recibir(evento, datos);
                  pendiente = pendiente.slice(separador.index + separador[0].length);
              }
          }
      } finally {
          lector.releaseLock();
      }
  }

  function createReport(extended) {
    const card = $('#reportTemplate').content.firstElementChild.cloneNode(true);
    card.querySelector('h3').textContent = t().diagnosis;
    card.querySelector('small').textContent = t().won;
    const toggle = card.querySelector('button');
    toggle.textContent = extended ? t().summary : t().full;
    toggle.setAttribute('aria-expanded', String(extended));
    toggle.addEventListener('click', () => run(() => loadReport(!extended)));
    return card;
  }

  function formatReport(card, text, extended) {
    const entries = card.querySelector('.algos');
    entries.replaceChildren();
    const lines = text.split('\n').filter(Boolean);
    if (extended) {
      for (let index = 0; index < lines.length; index += 2) {
        const [heading, detail] = lines.slice(index, index + 2);
        const end = heading.indexOf('. ');
        addReportEntry(entries, heading.slice(0, end), heading.slice(end + 2) + '\n' + (detail || ''));
      }
    } else {
      for (const line of lines) {
        const colon = line.indexOf(': ');
        addReportEntry(entries, colon < 0 ? '' : line.slice(0, colon), colon < 0 ? line : line.slice(colon + 2));
      }
    }
  }

  function addReportEntry(entries, heading, text) {
    const entry = document.createElement('div');
    entry.className = 'algo';
    if (heading) {
      const title = document.createElement('b');
      title.textContent = heading;
      entry.append(title);
    }
    const paragraph = document.createElement('p');
    paragraph.textContent = text;
    entry.append(paragraph);
    entries.append(entry);
  }

  async function loadReport(extended = false) {
    const previous = report;
    const card = createReport(extended);
    addReportEntry(card.querySelector('.algos'), '', '');
    const paragraph = card.querySelector('.algo p');
    paragraph.style.whiteSpace = 'pre-wrap';
    let text = '';
    const toggle = card.querySelector('button');
    toggle.disabled = true;
    try {
      await stream(route(`diagnostico/stream?extenso=${extended}`), {}, (event, data) => {
        if (event !== 'texto') return;
        if (!card.isConnected) {
          previous?.remove();
          messages.append(card);
        }
        text += data;
        paragraph.textContent = text;
        scroll();
      });
      formatReport(card, text, extended);
      toggle.disabled = false;
      report = card;
      scroll();
    } catch (error) {
      if (card.isConnected) {
        if (previous) card.replaceWith(previous);
        else card.remove();
      }
      throw error;
    }
  }

  async function stream(url, options, receive) {
    const response = await request(url, {
      ...options,
      headers: {'Accept': 'text/event-stream', ...options.headers}
    });
    let confirmed = false;
    await leerEventos(response, (event, data) => {
      if (event === 'estado') confirmed = true;
      receive(event, data);
    });
    if (!confirmed) throw new Error('SSE');
  }

  async function narrate(url, options = {}) {
    const typing = $('#typingTemplate').content.firstElementChild.cloneNode(true);
    typing.querySelector('span').textContent = t().entity + '…';
    messages.append(typing);
    scroll();
    let bubble;
    let text = '';
    let shaken = false;
    const render = value => {
      if (value || bubble) {
        typing.remove();
        bubble ??= message('narrator');
        bubble.textContent = value;
        scroll();
      }
    };
    try {
      await stream(url, options, (event, data) => {
        if (event === 'texto') {
          text += data;
          render(text);
          return;
        }
        if (event === 'escena' || event === 'estado') {
          const result = JSON.parse(data);
          state = result.estado;
          draw();
          if (result.runaFallida && !shaken) {
            const rune = $('#obj-' + result.runaFallida);
            if (rune) {
              shaken = true;
              rune.classList.remove('rune-failed');
              rune.getBoundingClientRect();
              rune.classList.add('rune-failed');
            }
          }
          if (event === 'estado' && result.diagnostico) {
            const diagnosis = result.diagnostico;
            render(text.replace(diagnosis, '').trimEnd());
            report?.remove();
            report = createReport(false);
            formatReport(report, diagnosis, false);
            messages.append(report);
            scroll();
          }
        }
      });
    } catch (error) {
      state = await (await request(route('estado'))).json();
      if (state.resultado === 'VICTORY' && !report) await loadReport();
      throw error;
    } finally {
      typing.remove();
    }
  }

  function turn(input, description) {
    return run(async () => {
      message('player', description);
      await narrate(route('chat'), {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(input)
      });
    });
  }

  function start() {
    return run(async () => {
      const url = state ? route('reiniciar') : `${base}?idioma=${lang.toUpperCase()}`;
      state = await (await request(url, {method: 'POST'})).json();
      messages.replaceChildren();
      report = null;
      chatInput.value = '';
      await narrate(route('bienvenida'));
    });
  }

  function activate(node) {
    if (busy || !state) return;
    const element = node.dataset.object;
    const item = state.reliquiaSeleccionada;
    const name = objectNames[lang][element];
    const x = {organ: '20%', chest: '82%'}[element] || '50%';
    flash.style.setProperty('--fx-x', x);
    flash.style.setProperty('--fx-y', element === 'chandelier' ? '24%' : '64%');
    flash.classList.add('on');
    setTimeout(() => flash.classList.remove('on'), 220);
    node.classList.add('focused');
    setTimeout(() => node.classList.remove('focused'), 520);
    if (item) {
      turn({escena: {tipo: 'USAR', elemento: element, reliquia: item}},
        `${t().use} ${itemNames[lang][item]} → ${name}`);
    } else if (element === 'chest' && state.escena.chest === 'CLOSED' && playing()) {
      turn({escena: {tipo: 'RECOGER', reliquia: 'chalice'}}, `${t().inspect} ${name}`);
    } else {
      turn({escena: {tipo: 'INSPECCIONAR', elemento: element}}, `${t().inspect} ${name}`);
    }
  }

  objects.forEach(node => {
    node.addEventListener('click', () => activate(node));
    node.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        activate(node);
      }
    });
  });
  $$('[data-item]').forEach(slot => slot.addEventListener('click', () => run(async () => {
    const item = slot.dataset.item;
    const response = await request(route('escena'), {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({tipo: 'SELECCIONAR', reliquia: item})
    });
    state = (await response.json()).estado;
    const toast = $('#toast');
    toast.textContent = state.reliquiaSeleccionada
      ? `${i18n[lang].selected} · ${itemNames[lang][item]}` : i18n[lang].cleared;
    toast.classList.add('show');
    clearTimeout(toast.hideTimer);
    toast.hideTimer = setTimeout(() => toast.classList.remove('show'), 1700);
  })));
  $('[data-quick="inspect"]').addEventListener('click', () =>
    turn({escena: {tipo: 'INSPECCIONAR', elemento: 'room'}}, i18n[lang].inspect));
  $('[data-quick="hint"]').addEventListener('click', () =>
    turn({escena: {tipo: 'PISTA'}}, i18n[lang].hint));
  $('[data-quick="report"]').addEventListener('click', () => {
    if (state?.resultado === 'VICTORY') run(() => loadReport());
  });
  $('#chatForm').addEventListener('submit', event => {
    event.preventDefault();
    const text = chatInput.value.trim();
    if (!state || !text || busy) return;
    chatInput.value = '';
    turn({texto: text, accion: playing()}, text);
  });
  $('#resetBtn').addEventListener('click', start);
  $('#langBtn').addEventListener('click', () => run(async () => {
    const next = lang === 'es' ? 'EN' : 'ES';
    state = await (await request(route(`idioma?idioma=${next}`), {method: 'PATCH'})).json();
    lang = state.idioma.toLowerCase();
    translate();
    if (report) await loadReport(report.querySelector('button').getAttribute('aria-expanded') === 'true');
  }));
  $('#logBtn').addEventListener('click', () => run(async () => {
    const history = await (await request(route('historial'))).json();
    logText.textContent = history.map(({fecha, autor, texto}) =>
      `[${new Date(fecha).toLocaleTimeString(lang)}] ${autor === 'JUGADOR' ? t().player : t().entity}\n${texto}`)
      .join('\n\n');
    logModal.classList.add('open');
    $('#closeModal').focus();
  }));
  function closeLog() {
    logModal.classList.remove('open');
    $('#logBtn').focus();
  }
  $('#closeModal').addEventListener('click', closeLog);
  logModal.addEventListener('click', event => { if (event.target === logModal) closeLog(); });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && logModal.classList.contains('open')) closeLog();
  });
  $('#downloadLog').addEventListener('click', () => {
    const url = URL.createObjectURL(new Blob([logText.textContent], {type: 'text/plain;charset=utf-8'}));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'bitacora-cripta-nocturnia.txt';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  translate();
  start();
})();
