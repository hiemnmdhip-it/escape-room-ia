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
    void elPerfilGeminiInstanciaSoloSuModeloSinRealizarSolicitudes() {
        try (var contexto = aplicacion().run("--spring.profiles.active=gemini",
                "--spring.ai.google.genai.api-key=solo-pruebas")) {
            assertEquals("GoogleGenAiChatModel", contexto.getBean(ChatModel.class).getClass().getSimpleName());
        }
    }

    private SpringApplicationBuilder aplicacion() {
        return new SpringApplicationBuilder(EscapeRoomIaApplication.class).web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off");
    }
}
