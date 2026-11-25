package com.escaperoom.ia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.escaperoom.ia.model.Idioma;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class VistaJuegoControllerTest {
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new VistaJuegoController())
            .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
            .build();

    @Test
    void sirveLaPantallaDeInicio() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("inicio"));
    }

    @Test
    void sirveElJuegoConIdiomaPorDefectoYSeleccionExplicita() throws Exception {
        mvc.perform(get("/juego"))
                .andExpect(status().isOk())
                .andExpect(view().name("juego"))
                .andExpect(model().attribute("idioma", Idioma.ES));
        mvc.perform(get("/juego").param("idioma", "EN"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("idioma", Idioma.EN));
        mvc.perform(get("/juego").param("idioma", "FR")).andExpect(status().isBadRequest());
    }
}
