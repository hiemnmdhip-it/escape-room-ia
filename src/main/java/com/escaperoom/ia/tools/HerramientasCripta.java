package com.escaperoom.ia.tools;

import com.escaperoom.ia.dto.ResultadoAccion;
import com.escaperoom.ia.model.EscapeSession;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public final class HerramientasCripta {
    private static final String SIN_SOLUCIONES = " Jamás reveles, confirmes ni sugieras la solución, opción, número u orden correctos. Java valida los intentos.";
    private final EscapeSession partida;
    private final Consumer<ResultadoAccion> alEjecutar;
    private ResultadoAccion ultimaAccion;

    public HerramientasCripta(EscapeSession partida) {
        this(partida, resultado -> {});
    }

    public HerramientasCripta(EscapeSession partida, Consumer<ResultadoAccion> alEjecutar) {
        this.partida = partida;
        this.alEjecutar = alEjecutar;
    }

    @Tool(returnDirect = true, description = "Examina por orden explícita. Puede descubrir la fase 1 del altar u órgano activos. No usar en preguntas." + SIN_SOLUCIONES)
    public String inspeccionarElemento(@ToolParam(description = "room, altar, organ, chandelier, chest, scroll, stake o chalice")
            String elemento) {
        return ejecutar(() -> partida.inspeccionar(id(elemento)));
    }

    @Tool(returnDirect = true, description = "Recoge por orden explícita. Abrir el cofre recoge chalice, necesario para ganar." + SIN_SOLUCIONES)
    public String recogerReliquia(@ToolParam(description = "Identificador de la reliquia: chalice, scroll o stake")
            String reliquia) {
        return ejecutar(() -> partida.recoger(id(reliquia)));
    }

    @Tool(returnDirect = true, description = "Aplica una reliquia poseída por orden explícita. La estaca en el candelabro activo descubre el mecanismo de la compuerta." + SIN_SOLUCIONES)
    public String usarReliquia(@ToolParam(description = "Identificador de reliquia: scroll, stake o chalice") String reliquia,
            @ToolParam(description = "Identificador del elemento: altar, organ, chandelier o chest") String elemento) {
        return ejecutar(() -> partida.usar(id(reliquia), id(elemento)));
    }

    @Tool(returnDirect = true, description = "Valida en Java el intento explícito del jugador. Nunca elijas el intento." + SIN_SOLUCIONES)
    public String resolverSello(@ToolParam(description = "Número de sello: 1, 2 o 3") int sello,
            @ToolParam(description = "Opción elegida por el jugador: A, B o C en fases 1 y 3, 1, 2 o 3 en fase 2") String intento) {
        return ejecutar(() -> partida.resolver(sello, id(intento).toUpperCase(Locale.ROOT)));
    }

    @Tool(returnDirect = true, description = "Orienta sobre el concepto de la fase activa sin afectar al progreso." + SIN_SOLUCIONES)
    public String solicitarPista() {
        return ejecutar(partida::solicitarPista);
    }

    public synchronized ResultadoAccion ultimaAccion() {
        return ultimaAccion;
    }

    private synchronized String ejecutar(Supplier<ResultadoAccion> accion) {
        if (ultimaAccion == null) {
            ultimaAccion = accion.get();
            alEjecutar.accept(ultimaAccion);
        }
        return ultimaAccion.mensaje();
    }

    private String id(String valor) {
        return valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
    }
}
