package com.escaperoom.ia.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

class HerramientasCriptaTest {
    private final EscapeSession partida = new EscapeSession("prueba", Idioma.ES);

    @Test
    void springAiDescubreLasCincoHerramientasYDespachaArgumentosJson() {
        partida.inspeccionar("altar");
        var herramientas = ToolCallbacks.from(new HerramientasCripta(partida));
        var nombres = Arrays.stream(herramientas).map(tool -> tool.getToolDefinition().name())
                .collect(Collectors.toSet());
        assertEquals(Set.of("inspeccionarElemento", "recogerReliquia", "usarReliquia", "resolverSello",
                "solicitarPista"), nombres);
        var resolver = Arrays.stream(herramientas)
                .filter(tool -> tool.getToolDefinition().name().equals("resolverSello")).findFirst().orElseThrow();
        assertTrue(resolver.call("{\"sello\":1,\"intento\":\"B\"}").contains("Fase 2/3"));
        assertEquals(2, partida.estado().fasesRunas().get("altar"));
        // El contenedor de herramientas acepta una única acción por turno.
        resolver.call("{\"sello\":1,\"intento\":\"2\"}");
        assertEquals(2, partida.estado().fasesRunas().get("altar"));
        assertEquals(0, partida.estado().sellosResueltos());
    }

    @Test
    void validaInventarioSecuenciaYLaInspeccionActivaSoloSuFase() {
        var anterior = partida.estado();
        new HerramientasCripta(partida).inspeccionarElemento(" ORGAN ");
        new HerramientasCripta(partida).solicitarPista();
        new HerramientasCripta(partida).usarReliquia("chalice", "organ");
        new HerramientasCripta(partida).resolverSello(3, "1");
        assertEquals(anterior, partida.estado());
        new HerramientasCripta(partida).inspeccionarElemento(" ALTAR ");
        assertEquals(1, partida.estado().fasesRunas().get("altar"));
        new HerramientasCripta(partida).recogerReliquia("chalice");
        assertEquals("OPEN", partida.estado().escena().get("chest"));
        new HerramientasCripta(partida).resolverSello(1, "B");
        new HerramientasCripta(partida).resolverSello(1, "2");
        new HerramientasCripta(partida).resolverSello(1, "C");
        assertTrue(new HerramientasCripta(partida).usarReliquia("chalice", "organ").contains("Fase 1/3"));
        assertTrue(new HerramientasCripta(partida).resolverSello(2, "B").contains("120 suspiros"));
    }
}
