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
    private final EscapeSession partida = new EscapeSession("prueba", Idioma.ES, 70);

    @Test
    void springAiDescubreLasCincoHerramientasYDespachaArgumentosJson() {
        var herramientas = ToolCallbacks.from(new HerramientasCripta(partida));
        var nombres = Arrays.stream(herramientas).map(tool -> tool.getToolDefinition().name())
                .collect(Collectors.toSet());
        assertEquals(Set.of("inspeccionarElemento", "recogerReliquia", "usarReliquia", "resolverSello",
                "solicitarPista"), nombres);
        var calibrar = Arrays.stream(herramientas)
                .filter(tool -> tool.getToolDefinition().name().equals("resolverSello")).findFirst().orElseThrow();
        assertTrue(calibrar.call("{\"sello\":1,\"intento\":\"50\"}").contains("ligero"));
        calibrar.call("{\"sello\":1,\"intento\":\"70\"}");
        assertEquals(0, partida.estado().sellosResueltos());
        assertEquals(6, partida.estado().calibracion().intentosRestantes());
    }

    @Test
    void validaInventarioSecuenciaEInspeccionSinMutacion() {
        var anterior = partida.estado();
        new HerramientasCripta(partida).inspeccionarElemento(" ALTAR ");
        new HerramientasCripta(partida).solicitarPista();
        new HerramientasCripta(partida).usarReliquia("chalice", "organ");
        new HerramientasCripta(partida).resolverSello(3, "F");
        assertEquals(anterior, partida.estado());
        new HerramientasCripta(partida).recogerReliquia("chalice");
        assertEquals("OPEN", partida.estado().escena().get("chest"));
        new HerramientasCripta(partida).resolverSello(1, "70");
        assertTrue(new HerramientasCripta(partida).usarReliquia("chalice", "organ").contains("runas"));
    }
}
