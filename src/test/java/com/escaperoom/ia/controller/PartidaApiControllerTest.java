package com.escaperoom.ia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

class PartidaApiControllerTest {
    @Test
    void laBienvenidaYElReinicioTransmitenElTextoEnElIdiomaDeLaPartida() throws Exception {
        var partidas = new PartidasService();
        var mvc = mvcSinModelo(partidas);
        var anterior = partidas.crear(Idioma.ES);
        String bienvenida = "/api/partidas/{id}/bienvenida";
        var inicio = mvc.perform(get(bienvenida, anterior.id()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted()).andReturn();
        String flujo = mvc.perform(asyncDispatch(inicio)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String texto = extraerTextoProgresivo(flujo);
        assertTrue(texto.startsWith("Has despertado"));
        assertTrue(texto.contains("\n\nTres sellos"));
        mvc.perform(get("/api/partidas/{id}/historial", anterior.id()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].texto").value(texto));

        String reinicio = mvc.perform(post("/api/partidas/{id}/reiniciar", anterior.id()).param("idioma", "EN"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String nuevoId = JsonPath.read(reinicio, "$.sessionId");
        var nueva = mvc.perform(get(bienvenida, nuevoId).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted()).andReturn();
        String flujoNuevo = mvc.perform(asyncDispatch(nueva)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(extraerTextoProgresivo(flujoNuevo).startsWith("You woke"));
    }

    @Test
    void iniciarYReiniciarDanEstadoInicialYUuidDistintos() throws Exception {
        var partidas = new PartidasService();
        var mvc = mvcSinModelo(partidas);
        mvc.perform(post("/api/partidas").param("idioma", "EN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.idioma").value("EN"))
                .andExpect(jsonPath("$.vitalidad").doesNotExist())
                .andExpect(jsonPath("$.sellosResueltos").value(0))
                .andExpect(jsonPath("$.fasesRunas.altar").value(1))
                .andExpect(jsonPath("$.escena.altar").value("READY"));
        var anterior = partidas.crear(Idioma.EN);
        mvc.perform(post("/api/partidas/{id}/escena", anterior.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"RECOGER\",\"reliquia\":\"chalice\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/partidas/{id}/reiniciar", anterior.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", not(anterior.id())))
                .andExpect(jsonPath("$.idioma").value("EN"))
                .andExpect(jsonPath("$.escena.chest").value("CLOSED"))
                .andExpect(jsonPath("$.fasesRunas.altar").value(1));
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
        String flujo = mvc.perform(asyncDispatch(peticion)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:escena")))
                .andExpect(content().string(containsString("event:texto")))
                .andExpect(content().string(containsString("event:estado")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(flujo.indexOf("event:escena") < flujo.indexOf("event:texto"));
        assertTrue(flujo.indexOf("event:texto") < flujo.indexOf("event:estado"));
        String texto = extraerTextoProgresivo(flujo);
        assertTrue(texto.contains("La tapa está abierta."));
        mvc.perform(get("/api/partidas/{id}/estado", partida.id()))
                .andExpect(jsonPath("$.escena.chest").value("OPEN"))
                .andExpect(jsonPath("$.inventario[2]").value("chalice"));
        mvc.perform(get("/api/partidas/{id}/historial", partida.id()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].autor").value("JUGADOR"))
                .andExpect(jsonPath("$[1].texto").value(texto));
    }

    @Test
    void rechazaEntradasSseAmbiguasOIncompletasSinAbrirUnTurno() throws Exception {
        var partidas = new PartidasService();
        var partida = partidas.crear(Idioma.ES);
        var mvc = mvcSinModelo(partidas);
        for (String entrada : List.of("{}", "{\"texto\":\"  \"}",
                "{\"texto\":\"Pista\",\"escena\":{\"tipo\":\"PISTA\"}}",
                "{\"escena\":{\"tipo\":\"PISTA\"},\"accion\":true}",
                "{\"escena\":{}}", "{\"escena\":{\"tipo\":\"RESOLVER\",\"intento\":\"50\"}}",
                "{\"escena\":{\"tipo\":\"RENDIRSE\"}}")) {
            mvc.perform(post("/api/partidas/{id}/chat", partida.id()).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.TEXT_EVENT_STREAM).content(entrada))
                    .andExpect(status().isBadRequest()).andExpect(request().asyncNotStarted());
        }
        assertEquals(0, partida.historial().size());
    }

    @Test
    void elDiagnosticoDevuelveResumenYDetalleSoloAlFinalizar() throws Exception {
        var partidas = mock(PartidasService.class);
        var partida = new EscapeSession("victoria", Idioma.ES);
        when(partidas.obtener(partida.id())).thenReturn(partida);
        var mvc = mvcSinModelo(partidas);
        String diagnostico = "/api/partidas/{id}/diagnostico";
        mvc.perform(get(diagnostico, partida.id())).andExpect(status().isConflict());
        mvc.perform(get(diagnostico + "/stream", partida.id()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isConflict());
        partida.inspeccionar("altar");
        partida.resolver(1, "B");
        partida.resolver(1, "2");
        partida.resolver(1, "C");
        partida.inspeccionar("organ");
        partida.resolver(2, "B");
        partida.resolver(2, "3");
        partida.resolver(2, "A");
        partida.usar("stake", "chandelier");
        partida.resolver(3, "C");
        var peticion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RESOLVER\",\"sello\":3,\"intento\":\"1\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(peticion)).andExpect(status().isOk());
        var finalizacion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RESOLVER\",\"sello\":3,\"intento\":\"B\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        String flujoSello = mvc.perform(asyncDispatch(finalizacion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("PLAYING")))
                .andExpect(content().string(not(containsString("MessageWindowChatMemory"))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(extraerTextoProgresivo(flujoSello).contains("cáliz"));
        mvc.perform(get(diagnostico, partida.id())).andExpect(status().isConflict());
        var recogida = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"escena\":{\"tipo\":\"RECOGER\",\"reliquia\":\"chalice\"}}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(recogida)).andExpect(status().isOk())
                .andExpect(content().string(containsString("VICTORY")))
                .andExpect(content().string(containsString("MessageWindowChatMemory")))
                .andExpect(content().string(not(containsString("QuestionAnswerAdvisor"))));
        mvc.perform(get(diagnostico, partida.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("VectorStore")))
                .andExpect(jsonPath("$.informe", not(containsString("QuestionAnswerAdvisor"))));
        mvc.perform(get(diagnostico, partida.id()).param("extenso", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.informe", containsString("QuestionAnswerAdvisor")));
        var informe = mvc.perform(get(diagnostico + "/stream", partida.id()).param("extenso", "true")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted()).andReturn();
        String flujoInforme = mvc.perform(asyncDispatch(informe)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(partida.diagnostico(true), extraerTextoProgresivo(flujoInforme));
    }

    private MockMvc mvcSinModelo(PartidasService partidas) {
        var memoria = MessageWindowChatMemory.builder().build();
        var historial = new HistorialService(memoria);
        var beans = new DefaultListableBeanFactory();
        var narrador = new NarradorService(beans.getBeanProvider(ChatClient.Builder.class), historial,
                new ClassPathResource("cripta-system-prompt.txt"));
        return MockMvcBuilders.standaloneSetup(new PartidaApiController(partidas, new EscenaService(historial), narrador))
                .build();
    }
    @Test
    void elChatEnviaFragmentoYEstadoPorSse() throws Exception {
        var partidas = new PartidasService();
        var memoria = MessageWindowChatMemory.builder().build();
        var historial = new HistorialService(memoria);
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
        String flujo = mvc.perform(asyncDispatch(peticion))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:texto")))
                .andExpect(content().string(containsString("event:estado")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals("El informe se abre al terminar.", extraerTextoProgresivo(flujo));
    }

    @Test
    void unFalloDeChatPublicaLaRunaAntesDeLaNarracion() throws Exception {
        var partidas = new PartidasService();
        var partida = partidas.crear(Idioma.ES);
        partida.inspeccionar("altar");
        var mvc = mvcSinModelo(partidas);
        var peticion = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"texto\":\"Elijo A\",\"accion\":true}"))
                .andExpect(request().asyncStarted()).andReturn();
        String flujo = mvc.perform(asyncDispatch(peticion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"runaFallida\":\"altar\"")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(flujo.indexOf("event:escena") < flujo.indexOf("event:texto"));
        assertTrue(flujo.indexOf("event:escena") >= 0);
        assertEquals(partida.historial().getLast().texto(), extraerTextoProgresivo(flujo));
        assertEquals(1, partida.estado().fasesRunas().get("altar"));

        var acierto = mvc.perform(post("/api/partidas/{id}/chat", partida.id())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"texto\":\"Elijo B\",\"accion\":true}"))
                .andExpect(request().asyncStarted()).andReturn();
        String flujoAcierto = mvc.perform(asyncDispatch(acierto)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(extraerTextoProgresivo(flujoAcierto).startsWith("Fase 2/3"));
        assertEquals(2, partida.estado().fasesRunas().get("altar"));
        assertEquals(0, partida.estado().sellosResueltos());
    }

    private static String extraerTextoProgresivo(String flujo) {
        var eventos = flujo.split("\\R\\R");
        assertTrue(eventos[eventos.length - 1].startsWith("event:estado"));
        var fragmentos = Arrays.stream(eventos)
                .filter(evento -> evento.startsWith("event:texto"))
                .map(evento -> evento.lines().skip(1)
                        .map(linea -> linea.substring("data: ".length()))
                        .collect(Collectors.joining("\n")))
                .toList();
        assertTrue(fragmentos.size() > 1, "Se esperaban múltiples eventos texto");
        return String.join("", fragmentos);
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
