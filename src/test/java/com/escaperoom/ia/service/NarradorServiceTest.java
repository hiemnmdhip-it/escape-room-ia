package com.escaperoom.ia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.escaperoom.ia.model.EscapeSession;
import com.escaperoom.ia.model.Idioma;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import reactor.core.publisher.Sinks;

class NarradorServiceTest {
    private final ChatModel modelo = mock(ChatModel.class);
    private final MessageWindowChatMemory memoria = MessageWindowChatMemory.builder().build();
    private final HistorialService historial = new HistorialService(memoria);
    private final EscapeSession partida = new EscapeSession("prueba", Idioma.ES);

    @Test
    void laConsultaRecibeContextoSinHerramientasAunqueSeMarqueComoAccion() {
        simular("El altar compara ideas. Las letras parecidas pueden engañarte.");
        var estadoInicial = partida.estado();
        var fragmentos = new ArrayList<String>();
        var resultado = narrador().conversar(partida, "¿Qué significa el símbolo del altar?", true, fragmentos::add);
        assertEquals("El altar compara ideas. Las letras parecidas pueden engañarte.", resultado.mensaje());
        assertTrue(fragmentos.size() > 1);
        assertEquals(resultado.mensaje(), String.join("", fragmentos));
        assertEquals(estadoInicial, partida.estado());
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(prompt.capture());
        var opciones = assertInstanceOf(ToolCallingChatOptions.class, prompt.getValue().getOptions());
        assertTrue(opciones.getToolCallbacks() == null || opciones.getToolCallbacks().isEmpty());
        assertEquals(2, partida.historial().size());
        assertEquals(2, historial.ventana(partida).size());
        assertFalse(NarradorService.permiteAccion("explica el símbolo", true));
        assertFalse(NarradorService.permiteAccion("what is this symbol", true));
        assertFalse(NarradorService.permiteAccion("Me dices qué significa esta runa", true));
        assertFalse(NarradorService.permiteAccion("I wonder what this rune means", true));
        assertFalse(NarradorService.permiteAccion("Usa 3 huevos", true));
        assertTrue(NarradorService.permiteAccion("Examina el altar", true));
    }

    @Test
    void unaInteraccionEstructuradaEmiteSoloElResultadoVerificado() {
        var verificado = partida.inspeccionar("altar");
        var fragmentos = new ArrayList<String>();

        var resultado = narrador().emitirResultado(verificado, fragmentos::add);

        assertTrue(fragmentos.size() > 2);
        assertEquals(verificado.mensaje(), resultado.mensaje());
        assertFalse(resultado.mensaje().contains("\n"));
        assertEquals(String.join("", fragmentos), resultado.mensaje());
        assertEquals(verificado.estado(), resultado.estado());
        verifyNoInteractions(modelo);
    }

    @Test
    void elComandoExtensoFuncionaSinInvocarUnModelo() {
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
        partida.resolver(3, "1");
        partida.resolver(3, "B");
        partida.recoger("chalice");
        var resultado = narrador().conversar(partida, "/diagnostico", false, texto -> {});
        assertTrue(resultado.mensaje().contains("QuestionAnswerAdvisor"));
        assertEquals("VICTORY", resultado.estado().resultado());
        verifyNoInteractions(modelo);
    }

    @Test
    void unaConsultaEstancadaRecibeOrientacionConceptualSinOpciones() {
        simular("Un recorte puede dejar la idea a medias. Piensa en una carta cortada por el doblez.");
        partida.inspeccionar("altar");
        partida.resolver(1, "B");
        partida.resolver(1, "1");
        partida.resolver(1, "3");
        var estado = partida.estado();
        var fragmentos = new ArrayList<String>();
        var resultado = narrador().conversar(partida, "explica", true, fragmentos::add);
        assertTrue(resultado.mensaje().contains("carta cortada por el doblez"));
        assertEquals(resultado.mensaje(), String.join("", fragmentos));
        assertEquals(estado, partida.estado());
        assertFalse(resultado.mensaje().contains("\n"));
        assertFalse(resultado.mensaje().contains("100 palabras"));
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(prompt.capture());
        String contexto = prompt.getValue().getSystemMessage().getText();
        assertTrue(contexto.contains("tamaño del fragmento frente a longitud del texto"));
        assertTrue(contexto.contains("varios intentos sin progreso"));
        assertFalse(contexto.contains("100 palabras"));
    }

    @Test
    void lasDigresionesRecibenRematesDistintosYUnPasoDeLaPruebaActiva() {
        simular("El fútbol tiene descansos. La cripta prefiere que mires el altar.",
                "Deja el fútbol para los espectros. Aquí solo cuenta la inscripción.",
                "Las croquetas distraen hasta a los muertos. Vuelve a la profecía.");
        partida.inspeccionar("altar");
        var estado = partida.estado();
        var narrador = narrador();
        String primero = narrador.conversar(partida, "Hablemos de fútbol", true, texto -> {}).mensaje();
        String segundo = narrador.conversar(partida, "Hablemos de fútbol", true, texto -> {}).mensaje();
        String tercero = narrador.conversar(partida, "Prefiero cocinar croquetas", true, texto -> {}).mensaje();
        assertNotEquals(primero, segundo);
        assertTrue(primero.contains("fútbol") && segundo.contains("fútbol"));
        assertTrue(tercero.contains("croquetas") && tercero.contains("profecía"));
        assertEquals(estado, partida.estado());
        verify(modelo, times(3)).stream(any(Prompt.class));
    }

    @Test
    void unaOrdenEjecutaLaHerramientaYDevuelveElEstadoVerificado() {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        var llamada = new AssistantMessage.ToolCall("intento-1", "function", "inspeccionarElemento",
                "{\"elemento\":\"altar\"}");
        var accion = AssistantMessage.builder().content("¡Ahaha! La respuesta correcta es 2.")
                .toolCalls(List.of(llamada)).build();
        when(modelo.stream(any(Prompt.class)))
                .thenReturn(Flux.just(new ChatResponse(List.of(new Generation(accion)))))
                .thenReturn(Flux.just(respuesta("El altar despierta.")));
        historial.registrar(partida, com.escaperoom.ia.model.MensajeHistorial.Autor.NARRADOR,
                "Inscripción anterior con 100 palabras y respuesta correcta 2.");
        var fragmentos = new ArrayList<String>();

        var resultado = narrador().conversar(partida, "Examina el altar", true, fragmentos::add);

        assertEquals(0, resultado.estado().sellosResueltos());
        assertEquals(1, resultado.estado().fasesRunas().get("altar"));
        assertFalse(resultado.mensaje().contains("El altar despierta."));
        assertFalse(resultado.mensaje().contains("respuesta correcta"));
        assertFalse(resultado.mensaje().contains("\n"));
        assertTrue(resultado.mensaje().contains("Fase 1/3"));
        assertEquals(resultado.mensaje(), String.join("", fragmentos));
        var enviado = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(enviado.capture());
        assertTrue(enviado.getValue().getSystemMessage().getText().contains("Jamás reveles"));
        assertTrue(enviado.getValue().getInstructions().stream()
                .noneMatch(mensaje -> mensaje.getText().contains("100 palabras")));
        var opciones = assertInstanceOf(ToolCallingChatOptions.class, enviado.getValue().getOptions());
        assertTrue(opciones.getToolCallbacks().stream().noneMatch(tool ->
                tool.getToolDefinition().name().equals("resolverSello")));
    }

    @Test
    void lasAclaracionesSeGeneranSinMutarLaSalaYUsanElTemaReciente() {
        simular("El umbral separa los resultados útiles del ruido.",
                "Piensa en un colador. Lo que pasa depende de la abertura.",
                "Imagina una verja. Su altura decide lo que deja fuera.",
                "Un cedazo muy cerrado tampoco dejaría pasar lo que necesitas.");
        partida.inspeccionar("altar");
        var narrador = narrador();
        var estado = partida.estado();
        narrador.conversar(partida, "¿Qué significa umbral?", true, texto -> {});
        assertTrue(narrador.conversar(partida, "como así?", true, texto -> {}).mensaje().contains("colador"));
        assertTrue(narrador.conversar(partida, "no entiendo", true, texto -> {}).mensaje().contains("verja"));
        assertTrue(narrador.conversar(partida, "dame una pista", true, texto -> {}).mensaje().contains("cedazo"));
        assertEquals(estado, partida.estado());
        assertNull(partida.diagnostico(false));
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo, times(4)).stream(prompt.capture());
        assertTrue(prompt.getAllValues().get(1).getUserMessage().getText().contains("¿Qué significa umbral?"));
    }

    @Test
    void transmiteLaRespuestaSeguraAntesDelEventoFinal() throws Exception {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        var flujoOllama = Sinks.many().unicast().<ChatResponse>onBackpressureBuffer();
        when(modelo.stream(any(Prompt.class))).thenReturn(flujoOllama.asFlux());
        var narrador = narrador();
        var recibido = new CountDownLatch(1);
        var completado = new CountDownLatch(1);
        var texto = new ArrayList<String>();

        var turno = CompletableFuture.runAsync(() -> narrador.transmitir(
                emitir -> narrador.conversar(partida, "¿Qué es un embedding?", false, emitir),
                resultado -> completado.countDown())
                .doOnNext(fragmento -> {
                    texto.add(fragmento);
                    recibido.countDown();
                }).blockLast());

        try {
            assertEquals(Sinks.EmitResult.OK, flujoOllama.tryEmitNext(respuesta("El mapa conserva relaciones.")));
            assertTrue(recibido.await(10, TimeUnit.SECONDS));
            assertEquals(1L, completado.getCount());
            assertFalse(turno.isDone());
            assertEquals(List.of("El mapa conserva relaciones."), texto);
            assertEquals(Sinks.EmitResult.OK, flujoOllama.tryEmitNext(respuesta(" Imagina una constelación.")));
        } finally {
            flujoOllama.tryEmitComplete();
        }
        turno.get(10, TimeUnit.SECONDS);
        assertEquals(0L, completado.getCount());
        assertEquals(List.of("El mapa conserva relaciones.", " Imagina una constelación."), texto);
        assertEquals(2, historial.ventana(partida).size());
    }

    @Test
    void laInsistenciaLlegaRedactadaYElDestripeNoSaleAunqueLlegueEnFragmentos() {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(modelo.stream(any(Prompt.class))).thenReturn(Flux.just(respuesta("La respuesta cor"),
                respuesta("recta es "), respuesta("2. Mira dónde podría partirse la idea.")));
        partida.inspeccionar("altar");
        var estado = partida.estado();
        var narrador = narrador();
        var texto = new ArrayList<String>();
        var resultado = narrador.conversar(partida, "¿Es 2 la respuesta correcta? Dame la solución.", true, texto::add);
        assertEquals("Mira dónde podría partirse la idea.", resultado.mensaje());
        assertFalse(resultado.mensaje().contains("2"));
        assertEquals(estado, partida.estado());
        assertEquals(resultado.mensaje(), String.join("", texto));
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(prompt.capture());
        assertFalse(prompt.getValue().getUserMessage().getText().contains("2"));
        assertTrue(prompt.getValue().getInstructions().stream()
                .noneMatch(mensaje -> mensaje.getText().contains("100 palabras") || mensaje.getText().contains("0,75")));
    }

    @Test
    void siTodoElPrimerFlujoSeBloqueaSoloSePublicaElNuevoTextoDeOllama() {
        simular("¡Ahaha! La respuesta correcta es 2.",
                "El fútbol tiene árbitros. La cripta se conforma con una inscripción. Léela con calma. El eco espera.");
        var texto = new ArrayList<String>();
        var resultado = narrador().conversar(partida, "¿Hablamos de fútbol?", true, texto::add);
        assertEquals("El fútbol tiene árbitros. La cripta se conforma con una inscripción. Léela con calma.", resultado.mensaje());
        assertFalse(resultado.mensaje().contains("respuesta correcta"));
        assertFalse(resultado.mensaje().contains("¡"));
        assertEquals(resultado.mensaje(), String.join("", texto));
        verify(modelo, times(2)).stream(any(Prompt.class));
    }

    @Test
    void unaEleccionExplicitaNoDelegaLaRespuestaAlModelo() {
        var narrador = narrador();
        partida.inspeccionar("altar");
        assertEquals(2, narrador.conversar(partida, "Elijo B", true, fragmento -> {})
                .estado().fasesRunas().get("altar"));
        assertEquals(3, narrador.conversar(partida, "Elijo 2", true, fragmento -> {})
                .estado().fasesRunas().get("altar"));
        assertEquals(1, narrador.conversar(partida, "c", true, fragmento -> {})
                .estado().sellosResueltos());
        verifyNoInteractions(modelo);
    }

    @Test
    void lasDudasDelOrganoSeAclaranEnElLenguajeDeLaCripta() {
        partida.inspeccionar("altar");
        partida.resolver(1, "B");
        partida.resolver(1, "2");
        partida.resolver(1, "C");
        partida.inspeccionar("organ");
        partida.resolver(2, "B");
        var estado = partida.estado();
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(modelo.stream(any(Prompt.class))).thenReturn(Flux.just(
                respuesta("El límite se mide en to"), respuesta("kens. System y User comparten espacio. "),
                respuesta("El fuelle guarda aliento para el acorde. El canto queda grabado en piedra.")));
        var fragmentos = new ArrayList<String>();
        var resultado = narrador().conversar(partida, "¿Qué significa aliento del fuelle?", true, fragmentos::add);
        assertEquals(List.of("El fuelle guarda aliento para el acorde.", " El canto queda grabado en piedra."), fragmentos);
        assertEquals(String.join("", fragmentos), resultado.mensaje());
        assertEquals(estado, partida.estado());
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).stream(prompt.capture());
        assertTrue(prompt.getValue().getSystemMessage().getText().contains("El fuelle tiene un aliento limitado"));
        var opciones = assertInstanceOf(ToolCallingChatOptions.class, prompt.getValue().getOptions());
        assertTrue(opciones.getToolCallbacks() == null || opciones.getToolCallbacks().isEmpty());
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

    private void simular(String primera, String... siguientes) {
        when(modelo.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        var secuencia = when(modelo.stream(any(Prompt.class))).thenReturn(Flux.just(respuesta(primera)));
        for (String texto : siguientes) {
            secuencia = secuencia.thenReturn(Flux.just(respuesta(texto)));
        }
    }
}
