package com.escaperoom.ia.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EscapeSessionTest {
    private static final Pattern JERGA_ORGANO = Pattern.compile("(?i)\\b(?:tokens?|system|user|assistant|chatmemory)\\b");
    private final EscapeSession partida = new EscapeSession("prueba", Idioma.ES);

    @Test
    void elAltarExigeDescubrimientoAjusteYRangoSemantico() {
        assertEquals(1, fase("altar"));
        assertTrue(partida.inspeccionar("room").mensaje().contains("Examina el Altar"));
        fallaSinAvanzar(1, "altar", 1, "B");
        assertTrue(partida.inspeccionar("altar").mensaje().startsWith("Fase 1/3 —"));
        assertEquals(1, fase("altar"));
        fallaSinAvanzar(1, "altar", 1, "A");
        assertTrue(partida.resolver(1, "B").mensaje().startsWith("Fase 2/3 —"));
        assertEquals(2, fase("altar"));
        assertEquals("altar", partida.resolver(1, "1").runaFallida());
        var fallo = partida.resolver(1, "3");
        assertEquals("altar", fallo.runaFallida());
        assertNotNull(fallo.pista());
        assertEquals(2, fase("altar"));
        assertTrue(partida.resolver(1, "2").mensaje().startsWith("Fase 3/3 —"));
        assertEquals(3, fase("altar"));
        assertEquals("LOCKED", partida.estado().escena().get("organ"));
        assertEquals(0, partida.estado().turnosEstancados());
        fallaSinAvanzar(1, "altar", 3, "B");
        assertEquals(1, partida.resolver(1, "c").estado().sellosResueltos());
    }

    @Test
    void elOrganoDescubreElOlvidoMideElAlientoYPurgaElEcoViejo() {
        assertEquals("LOCKED", partida.resolver(2, "3").estado().escena().get("organ"));
        abrirAltar();
        assertEquals(1, fase("organ"));
        var descubrimiento = partida.inspeccionar("organ");
        assertEquals(1, fase("organ"));
        assertTrue(descubrimiento.mensaje().startsWith("Fase 1/3 —"));
        assertTrue(descubrimiento.mensaje().contains("cantos más antiguos"));
        compruebaAmbientacionOrgano(descubrimiento.mensaje());
        fallaSinAvanzar(2, "organ", 1, "C");
        var ajuste = partida.resolver(2, "B");
        assertTrue(ajuste.mensaje().startsWith("Fase 2/3 —"));
        assertTrue(ajuste.mensaje().contains("120 suspiros"));
        compruebaAmbientacionOrgano(ajuste.mensaje());
        assertEquals(2, fase("organ"));
        fallaSinAvanzar(2, "organ", 2, "2");
        var cierre = partida.resolver(2, "3");
        assertTrue(cierre.mensaje().startsWith("Fase 3/3 —"));
        assertEquals(3, fase("organ"));
        assertEquals("LOCKED", cierre.estado().escena().get("chandelier"));
        compruebaAmbientacionOrgano(cierre.mensaje());
        fallaSinAvanzar(2, "organ", 3, "B");
        assertEquals(2, partida.resolver(2, "A").estado().sellosResueltos());
    }

    @Test
    void laCompuertaValidaMecanismoYVeredictoAntesDeExigirElCaliz() {
        abrirPrimerasDosRunas();
        partida.inspeccionar("chandelier");
        assertEquals(1, fase("chandelier"));
        fallaSinAvanzar(3, "chandelier", 1, "C");
        assertTrue(partida.usar("stake", "chandelier").mensaje().startsWith("Fase 1/3 —"));
        fallaSinAvanzar(3, "chandelier", 1, "A");
        assertTrue(partida.resolver(3, "C").mensaje().startsWith("Fase 2/3 —"));
        fallaSinAvanzar(3, "chandelier", 2, "2");
        fallaSinAvanzar(3, "chandelier", 2, "3");
        assertTrue(partida.resolver(3, "1").mensaje().startsWith("Fase 3/3 —"));
        fallaSinAvanzar(3, "chandelier", 3, "C");

        var salida = partida.resolver(3, "B");
        assertEquals(3, salida.estado().sellosResueltos());
        assertEquals("PLAYING", salida.estado().resultado());
        assertEquals(java.util.List.of("chalice"), salida.estado().reliquiasPendientes());
        assertNull(salida.diagnostico());

        var fin = partida.recoger("chalice");
        assertEquals("VICTORY", fin.estado().resultado());
        assertTrue(fin.estado().reliquiasPendientes().isEmpty());
        for (String concepto : java.util.List.of("VectorStore", "MessageWindowChatMemory", "BeanOutputConverter",
                "ChatClient.stream()")) {
            assertTrue(fin.diagnostico().contains(concepto));
        }
        assertEquals(fin.estado(), partida.resolver(3, "A").estado());
        for (String termino : java.util.List.of("tokens", "System", "User")) {
            assertTrue(partida.diagnostico(true).contains(termino));
        }
    }

    @Test
    void conInventarioCompletoGanaAlValidarElUltimoVeredicto() {
        assertEquals("PLAYING", partida.recoger("chalice").estado().resultado());
        abrirPrimerasDosRunas();
        partida.usar("stake", "chandelier");
        assertEquals("PLAYING", partida.resolver(3, "C").estado().resultado());
        assertEquals("PLAYING", partida.resolver(3, "1").estado().resultado());
        assertEquals("VICTORY", partida.resolver(3, "B").estado().resultado());
    }

    @Test
    void losIntentosRepetidosNoTerminanLaPartidaNiImpidenAvanzar() {
        abrirAltar();
        partida.inspeccionar("organ");
        partida.resolver(2, "B");
        for (int i = 0; i < 100; i++) {
            var fallo = partida.resolver(2, "2");
            assertEquals("organ", fallo.runaFallida());
            assertEquals("PLAYING", fallo.estado().resultado());
            assertNull(fallo.diagnostico());
        }
        assertNull(partida.diagnostico(false));
        assertEquals(3, partida.resolver(2, "3").estado().fasesRunas().get("organ"));
        assertEquals(2, partida.resolver(2, "A").estado().sellosResueltos());
    }

    @Test
    void aplicarUnaReliquiaALaRunaBloqueadaMarcaEsaRunaYConservaLaFase() {
        var fallo = partida.usar("stake", "chandelier");
        assertEquals("chandelier", fallo.runaFallida());
        assertEquals("LOCKED", fallo.estado().escena().get("chandelier"));
        assertEquals(1, fallo.estado().fasesRunas().get("chandelier"));
        assertEquals(0, fallo.estado().turnosEstancados());
    }

    private int fase(String elemento) {
        return partida.estado().fasesRunas().get(elemento);
    }

    private void fallaSinAvanzar(int sello, String elemento, int fase, String intento) {
        var fallo = partida.resolver(sello, intento);
        assertEquals(elemento, fallo.runaFallida());
        assertEquals(fase, fallo.estado().fasesRunas().get(elemento));
        assertEquals(sello - 1, fallo.estado().sellosResueltos());
    }

    private void compruebaAmbientacionOrgano(String enunciado) {
        assertFalse(JERGA_ORGANO.matcher(enunciado).find(), enunciado);
        assertFalse(JERGA_ORGANO.matcher(partida.contextoConversacional()).find());
        assertFalse(JERGA_ORGANO.matcher(partida.solicitarPista().mensaje()).find());
    }

    private void abrirAltar() {
        partida.inspeccionar("altar");
        partida.resolver(1, "B");
        partida.resolver(1, "2");
        partida.resolver(1, "C");
    }

    private void abrirPrimerasDosRunas() {
        abrirAltar();
        partida.inspeccionar("organ");
        partida.resolver(2, "B");
        partida.resolver(2, "3");
        partida.resolver(2, "A");
    }

    @Test
    void sincronizaCofreInventarioSeleccionEIdiomaSinAlterarInstantaneas() {
        var antes = partida.estado();
        assertEquals("CLOSED", antes.escena().get("chest"));
        assertNull(partida.seleccionar("chalice").estado().reliquiaSeleccionada());
        partida.recoger("chalice");
        partida.recoger("chalice");
        assertEquals(3, partida.estado().inventario().size());
        assertFalse(antes.inventario().contains("chalice"));
        assertEquals("OPEN", partida.estado().escena().get("chest"));
        assertEquals("chalice", partida.seleccionar("chalice").estado().reliquiaSeleccionada());
        assertNull(partida.seleccionar("chalice").estado().reliquiaSeleccionada());
        partida.seleccionar("chalice");
        assertNull(partida.usar("chalice", "organ").estado().reliquiaSeleccionada());
        partida.cambiarIdioma(Idioma.EN);
        assertTrue(partida.inspeccionar("chest").mensaje().contains("inventory"));
    }

}
