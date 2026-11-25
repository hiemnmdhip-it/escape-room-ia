package com.escaperoom.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Profiles;

class EscapeRoomIaApplicationTests {
    @Test
    void arrancaConOllamaPorDefectoSinInvocarElModelo() {
        try (var contexto = aplicacion().run()) {
            assertTrue(contexto.getEnvironment().acceptsProfiles(Profiles.of("ollama")));
            assertInstanceOf(OllamaChatModel.class, contexto.getBean(ChatModel.class));
        }
    }

    @Test
    void unPerfilAdicionalMantieneOllamaLocalComoUnicoModeloDeChat() {
        try (var contexto = aplicacion().run("--spring.profiles.active=pruebas")) {
            var modelo = assertInstanceOf(OllamaChatModel.class, contexto.getBean(ChatModel.class));
            assertEquals(1, contexto.getBeansOfType(ChatModel.class).size());
            assertEquals("qwen3:8b", modelo.getOptions().getModel());
            assertEquals("http://127.0.0.1:11434", contexto.getEnvironment().getProperty("spring.ai.ollama.base-url"));
        }
    }

    private SpringApplicationBuilder aplicacion() {
        return new SpringApplicationBuilder(EscapeRoomIaApplication.class).web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off");
    }
}
