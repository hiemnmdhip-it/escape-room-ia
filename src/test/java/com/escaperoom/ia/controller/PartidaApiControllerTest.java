package com.escaperoom.ia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

class PartidaApiControllerTest {
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
