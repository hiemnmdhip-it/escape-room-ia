package com.escaperoom.ia.tools;

import com.escaperoom.ia.dto.ResultadoAccion;
import com.escaperoom.ia.model.EscapeSession;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public final class HerramientasCripta {
    private final EscapeSession partida;
    private ResultadoAccion ultimaAccion;

    public HerramientasCripta(EscapeSession partida) {
        this.partida = partida;
    }

    @Tool(description = "Inspecciona un objeto de la cripta. No gasta vitalidad ni modifica el estado.")
    public String inspeccionarElemento(@ToolParam(description = "altar, organ, chandelier, chest, scroll, stake o chalice")
            String elemento) {
        return ejecutar(() -> partida.inspeccionar(id(elemento)));
    }

    @Tool(description = "Recoge una reliquia disponible. Abrir el cofre de obsidiana equivale a recoger chalice.")
    public String recogerReliquia(@ToolParam(description = "Identificador de la reliquia: chalice, scroll o stake")
            String reliquia) {
        return ejecutar(() -> partida.recoger(id(reliquia)));
    }

    @Tool(description = "Usa una reliquia del inventario sobre un objeto de la cripta.")
    public String usarReliquia(@ToolParam(description = "Identificador de reliquia: scroll, stake o chalice") String reliquia,
            @ToolParam(description = "Identificador del elemento: altar, organ, chandelier o chest") String elemento) {
        return ejecutar(() -> partida.usar(id(reliquia), id(elemento)));
    }

    @Tool(description = "Envía exactamente un intento explícito del jugador para el sello activo. Nunca elijas tú el intento.")
    public String resolverSello(@ToolParam(description = "Número de sello: 1, 2 o 3") int sello,
            @ToolParam(description = "Peso entero, una runa (A, Z, R) o una cámara (A a F), según el sello") String intento) {
        return ejecutar(() -> partida.resolver(sello, id(intento).toUpperCase(Locale.ROOT)));
    }

    @Tool(description = "Devuelve una pista sutil para el sello activo sin gastar vitalidad.")
    public String solicitarPista() {
        return ejecutar(partida::solicitarPista);
    }

    public synchronized ResultadoAccion ultimaAccion() {
        return ultimaAccion;
    }

    private synchronized String ejecutar(Supplier<ResultadoAccion> accion) {
        if (ultimaAccion == null) {
            ultimaAccion = accion.get();
        }
        return ultimaAccion.mensaje();
    }

    private String id(String valor) {
        return valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
    }
}
