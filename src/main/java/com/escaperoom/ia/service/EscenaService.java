package com.escaperoom.ia.service;

import com.escaperoom.ia.dto.AccionEscena;
import com.escaperoom.ia.dto.ResultadoAccion;
import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.MensajeHistorial.Autor;
import com.escaperoom.ia.tools.HerramientasCripta;
import java.util.Locale;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;

@Service
public class EscenaService {
    private final HistorialService historial;

    public EscenaService(HistorialService historial) {
        this.historial = historial;
    }

    public ResultadoAccion ejecutar(EscapeSession partida, AccionEscena accion) {
        return ejecutar(partida, accion, UnaryOperator.identity());
    }

    public ResultadoAccion ejecutar(EscapeSession partida, AccionEscena accion,
            UnaryOperator<ResultadoAccion> narrar) {
        return partida.enTurno(() -> {
            var idioma = partida.estado().idioma();
            String elemento = accion.elemento();
            String reliquia = accion.reliquia();
            var herramientas = new HerramientasCripta(partida);
            String descripcion = switch (accion.tipo()) {
                case INSPECCIONAR -> {
                    herramientas.inspeccionarElemento(elemento);
                    yield idioma.texto("Inspeccionar ", "Inspect ") + elemento;
                }
                case RECOGER -> {
                    herramientas.recogerReliquia(reliquia);
                    yield idioma.texto("Recoger ", "Collect ") + reliquia;
                }
                case USAR -> {
                    herramientas.usarReliquia(reliquia, elemento);
                    yield idioma.texto("Usar %s en %s", "Use %s on %s").formatted(reliquia, elemento);
                }
                case PISTA -> {
                    herramientas.solicitarPista();
                    yield idioma.texto("Pedir pista", "Ask for a hint");
                }
                case RESOLVER -> {
                    herramientas.resolverSello(accion.sello(), accion.intento());
                    yield idioma.texto("Sello %d: %s", "Seal %d: %s").formatted(accion.sello(), accion.intento());
                }
                case SELECCIONAR -> idioma.texto("Seleccionar ", "Select ") + reliquia;
            };
            ResultadoAccion resultado = accion.tipo() == AccionEscena.Tipo.SELECCIONAR
                    ? partida.seleccionar(reliquia == null ? "" : reliquia.strip().toLowerCase(Locale.ROOT))
                    : herramientas.ultimaAccion();
            historial.registrar(partida, Autor.JUGADOR, descripcion);
            ResultadoAccion narrado = narrar.apply(resultado);
            String texto = narrado.mensaje();
            if (narrado.pista() != null && !texto.contains(narrado.pista())) {
                texto += "\n" + narrado.pista();
            }
            if (narrado.diagnostico() != null && !texto.contains(narrado.diagnostico())) {
                texto += "\n" + narrado.diagnostico();
            }
            historial.registrar(partida, Autor.NARRADOR, texto);
            return narrado;
        });
    }
}
