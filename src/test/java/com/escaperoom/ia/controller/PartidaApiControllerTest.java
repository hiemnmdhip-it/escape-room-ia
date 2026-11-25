package com.escaperoom.ia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import com.escaperoom.ia.service.EscenaService;
import com.escaperoom.ia.service.HistorialService;
import com.escaperoom.ia.service.NarradorService;
import com.escaperoom.ia.service.PartidasService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

class PartidaApiControllerTest {
    @Test
    void iniciarYReiniciarDanEstadoInicialYUuidDistintos() throws Exception {
        var partidas = new PartidasService();
        var mvc = mvcSinModelo(partidas);
        mvc.perform(post("/api/partidas").param("idioma", "EN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.idioma").value("EN"))
                .andExpect(jsonPath("$.vitalidad").value(10))
                .andExpect(jsonPath("$.sellosResueltos").value(0))
                .andExpect(jsonPath("$.calibracion.intentosRestantes").value(7))
                .andExpect(jsonPath("$.escena.altar").value("READY"));
        var anterior = partidas.crear(Idioma.EN);
        mvc.perform(post("/api/partidas/{id}/escena", anterior.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"RECOGER\",\"reliquia\":\"chalice\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/partidas/{id}/reiniciar", anterior.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", not(anterior.id())))
                .andExpect(jsonPath("$.idioma").value("EN"))
                .andExpect(jsonPath("$.escena.chest").value("CLOSED"))
                .andExpect(jsonPath("$.calibracion.intentosRestantes").value(7));
        mvc.perform(get("/api/partidas/{id}/estado", anterior.id()))
                .andExpect(jsonPath("$.escena.chest").value("OPEN"));
        mvc.perform(get("/api/partidas/{id}/historial", anterior.id()))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/partidas/{id}/reiniciar", anterior.id()).param("idioma", "ES"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idioma").value("ES"));
        mvc.perform(post("/api/partidas/{id}/reiniciar", "inexistente")).andExpect(status().isNotFound());
    }

    @Test
    void laEscenaPorSseResuelveUnTurnoYLoRegistraUnaVez() throws Exception {
        var partidas = new PartidasService();
        var partida = partidas.crear(Idioma.ES);
        var mvc = mvcSinModelo(partidas);
        var peticion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RECOGER\",\"reliquia\":\"chalice\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(peticion)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:texto")))
                .andExpect(content().string(containsString("Cofre abierto.")))
                .andExpect(content().string(containsString("event:estado")));
        mvc.perform(get("/api/partidas/{id}/estado", partida.id()))
                .andExpect(jsonPath("$.escena.chest").value("OPEN"))
                .andExpect(jsonPath("$.inventario[2]").value("chalice"));
        mvc.perform(get("/api/partidas/{id}/historial", partida.id()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].autor").value("JUGADOR"))
                .andExpect(jsonPath("$[1].texto", containsString("Cofre abierto.")));
    }

    @Test
    void rechazaEntradasSseAmbiguasOIncompletasSinAbrirUnTurno() throws Exception {
        var partidas = new PartidasService();
        var partida = partidas.crear(Idioma.ES);
        var mvc = mvcSinModelo(partidas);
        for (String entrada : List.of("{}", "{\"texto\":\"  \"}",
                "{\"texto\":\"Pista\",\"escena\":{\"tipo\":\"PISTA\"}}",
                "{\"escena\":{\"tipo\":\"PISTA\"},\"accion\":true}",
                "{\"escena\":{}}", "{\"escena\":{\"tipo\":\"RESOLVER\",\"intento\":\"50\"}}")) {
            mvc.perform(post("/api/partidas/{id}/chat", partida.id()).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.TEXT_EVENT_STREAM).content(entrada))
                    .andExpect(status().isBadRequest()).andExpect(request().asyncNotStarted());
        }
        assertEquals(0, partida.historial().size());
    }

    @Test
    void elDiagnosticoDevuelveResumenYDetalleSoloAlFinalizar() throws Exception {
        var partidas = mock(PartidasService.class);
        var partida = new EscapeSession("victoria", Idioma.ES, 50);
        var derrota = new EscapeSession("derrota", Idioma.EN, 70);
        when(partidas.obtener(partida.id())).thenReturn(partida);
        when(partidas.obtener(derrota.id())).thenReturn(derrota);
        var mvc = mvcSinModelo(partidas);
        String diagnostico = "/api/partidas/{id}/diagnostico";
        mvc.perform(get(diagnostico, partida.id())).andExpect(status().isConflict());
        partida.resolver(1, "50");
        List.of("A", "R", "Z", "A").forEach(runa -> partida.resolver(2, runa));
        var peticion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RESOLVER\",\"sello\":3,\"intento\":\"B\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(peticion)).andExpect(status().isOk());
        partida.resolver(3, "D");
        var finalizacion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RESOLVER\",\"sello\":3,\"intento\":\"F\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(finalizacion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("VICTORY")))
                .andExpect(content().string(containsString("ArrayDeque")))
                .andExpect(content().string(not(containsString("Arrays.binarySearch"))));
        mvc.perform(get(diagnostico, partida.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("O(log n)")))
                .andExpect(jsonPath("$.informe", not(containsString("Arrays.binarySearch"))));
        mvc.perform(get(diagnostico, partida.id()).param("extenso", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("Arrays.binarySearch")));
        for (int i = 0; i < 7; i++) derrota.resolver(1, "50");
        mvc.perform(get(diagnostico, derrota.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("Binary search")));
        mvc.perform(get(diagnostico, derrota.id()).param("extenso", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("Seal III.")));
    }

    private MockMvc mvcSinModelo(PartidasService partidas) {
        var historial = new HistorialService(MessageWindowChatMemory.builder().build());
        var beans = new DefaultListableBeanFactory();
        var narrador = new NarradorService(beans.getBeanProvider(ChatClient.Builder.class), historial,
                new ClassPathResource("cripta-system-prompt.txt"));
        return MockMvcBuilders.standaloneSetup(new PartidaApiController(partidas, new EscenaService(historial), narrador))
                .build();
    }
    @Test
    void elChatEnviaFragmentoYEstadoPorSse() throws Exception {
        var partidas = new PartidasService();
        var historial = new HistorialService(MessageWindowChatMemory.builder().build());
        var beans = new DefaultListableBeanFactory();
        var narrador = new NarradorService(beans.getBeanProvider(ChatClient.Builder.class), historial,
                new ClassPathResource("cripta-system-prompt.txt"));
        var mvc = MockMvcBuilders.standaloneSetup(new PartidaApiController(partidas, new EscenaService(historial), narrador)).build();
        var partida = partidas.crear(Idioma.ES);
        var peticion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"texto\":\"/diagnostico\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(peticion))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:texto")))
                .andExpect(content().string(containsString("event:estado")))
                .andExpect(content().string(containsString("El informe se abre al terminar.")));
    }

    @Test
    void unaAccionDeEscenaApareceEnElEstadoYEnElHistorialDeSuPartida() throws Exception {
        var partidas = new PartidasService();
        var historial = new HistorialService(MessageWindowChatMemory.builder().build());
        var controlador = new PartidaApiController(partidas, new EscenaService(historial), mock(NarradorService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controlador).build();
        var partida = partidas.crear(Idioma.ES);
        var otra = partidas.crear(Idioma.EN);
        mvc.perform(post("/api/partidas/{id}/escena", partida.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"RECOGER\",\"reliquia\":\"chalice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.escena.chest").value("OPEN"));
        mvc.perform(get("/api/partidas/{id}/historial", partida.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].autor").value("JUGADOR"))
                .andExpect(jsonPath("$[1].autor").value("NARRADOR"));
        mvc.perform(get("/api/partidas/{id}/historial", otra.id())).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get("/api/partidas/{id}/historial", "desconocida")).andExpect(status().isNotFound());
    }
}
