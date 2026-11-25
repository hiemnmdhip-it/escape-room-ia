package com.escaperoom.ia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

class NarradorServiceTest {
    private final ChatModel modelo = mock(ChatModel.class);
    private final HistorialService historial = new HistorialService(MessageWindowChatMemory.builder().build());
    private final EscapeSession partida = new EscapeSession("prueba", Idioma.ES, 70);

    @Test
    void laConsultaRecibeContextoSinHerramientasAunqueSeMarqueComoAccion() {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(modelo.stream(any(Prompt.class))).thenReturn(Flux.just(
                respuesta("Es una marca."), respuesta(" Guía tu camino.")));
        var estadoInicial = partida.estado();
        var fragmentos = new ArrayList<String>();
        var resultado = narrador().conversar(partida, "¿Qué significa este símbolo?", true, fragmentos::add);
        assertEquals("Es una marca. Guía tu camino.", resultado.mensaje());
        assertEquals(List.of("Es una marca.", " Guía tu camino."), fragmentos);
        assertEquals(estadoInicial, partida.estado());
        var enviado = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(enviado.capture());
        var opciones = assertInstanceOf(ToolCallingChatOptions.class, enviado.getValue().getOptions());
        assertTrue(opciones.getToolCallbacks() == null || opciones.getToolCallbacks().isEmpty());
        assertTrue(enviado.getValue().getSystemMessage().getText().contains("frases cortas"));
        assertEquals(2, partida.historial().size());
        assertEquals(2, historial.ventana(partida).size());
        assertFalse(NarradorService.permiteAccion("explica el símbolo", true));
        assertFalse(NarradorService.permiteAccion("what is this symbol", true));
        assertFalse(NarradorService.permiteAccion("Me dices qué significa esta runa", true));
        assertFalse(NarradorService.permiteAccion("I wonder what this rune means", true));
        assertTrue(NarradorService.permiteAccion("Calibro 70", true));
    }

    @Test
    void unaInteraccionEstructuradaEmiteElResultadoAntesDeAmbientarSinHerramientas() {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(modelo.stream(any(Prompt.class))).thenReturn(Flux.just(respuesta("La cripta contiene la respiración.")));
        var verificado = partida.recoger("chalice");
        var fragmentos = new ArrayList<String>();

        var resultado = narrador().narrarEscena(verificado, fragmentos::add);

        assertEquals(List.of(verificado.mensaje(), "\n\nLa cripta contiene la respiración."), fragmentos);
        assertEquals(String.join("", fragmentos), resultado.mensaje());
        assertEquals(verificado.estado(), resultado.estado());
        var enviado = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(enviado.capture());
        var opciones = assertInstanceOf(ToolCallingChatOptions.class, enviado.getValue().getOptions());
        assertTrue(opciones.getToolCallbacks() == null || opciones.getToolCallbacks().isEmpty());
    }

    @Test
    void elComandoExtensoFuncionaSinInvocarUnModelo() {
        for (int i = 0; i < 7; i++) {
            partida.resolver(1, "50");
        }
        var resultado = narrador().conversar(partida, "/diagnostico", false, texto -> {});
        assertTrue(resultado.mensaje().contains("Arrays.binarySearch"));
        assertEquals("DEFEAT", resultado.estado().resultado());
        verifyNoInteractions(modelo);
    }

    @Test
    void unaOrdenEjecutaLaHerramientaYDevuelveElEstadoVerificado() {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        var llamada = new AssistantMessage.ToolCall("intento-1", "function", "resolverSello",
                "{\"sello\":1,\"intento\":\"70\"}");
        var accion = AssistantMessage.builder().toolCalls(List.of(llamada)).build();
        when(modelo.stream(any(Prompt.class))).thenReturn(
                Flux.just(new ChatResponse(List.of(new Generation(accion)))),
                Flux.just(respuesta("El órgano despierta.")));

        var resultado = narrador().conversar(partida, "Calibro 70", true, texto -> {});

        assertEquals(1, resultado.estado().sellosResueltos());
        assertEquals("READY", resultado.estado().escena().get("organ"));
        assertEquals("El órgano despierta.", resultado.mensaje());
        verify(modelo, times(2)).stream(any(Prompt.class));
    }

    private NarradorService narrador() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("chatClient", ChatClient.builder(modelo));
        return new NarradorService(beans.getBeanProvider(ChatClient.Builder.class), historial,
                new ClassPathResource("cripta-system-prompt.txt"));
    }

    private ChatResponse respuesta(String texto) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))));
    }
}
