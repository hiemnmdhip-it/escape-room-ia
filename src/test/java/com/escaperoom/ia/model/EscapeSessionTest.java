package com.escaperoom.ia.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EscapeSessionTest {
    @Test
    void encuentraCualquieraDeLosCienPesosEnSieteLecturas() {
        for (int peso = 1; peso <= 100; peso++) {
            var partida = partida(peso);
            for (int lecturas = 0; lecturas < 7 && partida.estado().sellosResueltos() == 0; lecturas++) {
                var rango = partida.estado().calibracion();
                int centro = rango.minimo() + (rango.maximo() - rango.minimo()) / 2;
                partida.resolver(1, Integer.toString(centro));
            }
            assertEquals(1, partida.estado().sellosResueltos(), "Peso " + peso);
            assertTrue(partida.estado().vitalidad() >= 4);
        }
    }

    @Test
    void limitaLecturasYGuardaDiagnosticoDeDerrota() {
        var partida = partida(70);
        assertEquals(7, partida.resolver(1, "fuego").estado().calibracion().intentosRestantes());
        assertEquals(7, partida.resolver(1, "101").estado().calibracion().intentosRestantes());
        assertEquals(51, partida.resolver(1, "50").estado().calibracion().minimo());
        assertNull(partida.resolver(1, "50").pista());
        assertNotNull(partida.resolver(1, "50").pista());
        for (int i = 0; i < 3; i++) {
            partida.resolver(1, "50");
        }
        var fin = partida.resolver(1, "50");
        assertEquals("DEFEAT", fin.estado().resultado());
        assertEquals(3, fin.estado().vitalidad());
        assertTrue(fin.diagnostico().contains("/diagnostico"));
        assertNull(partida.resolver(1, "70").diagnostico());
        assertTrue(partida.diagnostico(true).contains("Arrays.binarySearch"));
    }

    @Test
    void apagaEnOrdenLifoConRunasRepetidasYActivaPistaTrasDosFallos() {
        var partida = partida(50);
        assertEquals(10, partida.resolver(2, "A").estado().vitalidad());
        partida.resolver(1, "50");
        assertNull(partida.resolver(2, "Z").pista());
        assertNotNull(partida.resolver(2, "R").pista());
        var anterior = partida.estado();
        partida.solicitarPista();
        assertEquals(anterior, partida.estado());
        for (String runa : new String[] {"A", "R", "Z", "A"}) {
            assertNull(partida.resolver(2, runa).pista());
        }
        assertEquals(2, partida.estado().sellosResueltos());
        assertEquals(8, partida.estado().vitalidad());
    }

    @Test
    void rechazaSaltosYCiclosYAbreLaCompuertaFinal() {
        var partida = partida(50);
        partida.resolver(1, "50");
        for (String runa : new String[] {"A", "R", "Z", "A"}) {
            partida.resolver(2, runa);
        }
        assertEquals("A", partida.resolver(3, "F").estado().camara());
        assertEquals("B", partida.resolver(3, "B").estado().camara());
        assertEquals("B", partida.resolver(3, "A").estado().camara());
        assertEquals("D", partida.resolver(3, "D").estado().camara());
        var fin = partida.resolver(3, "F");
        assertEquals("VICTORY", fin.estado().resultado());
        assertEquals(8, fin.estado().vitalidad());
        assertTrue(fin.diagnostico().contains("ArrayDeque"));
        assertEquals(fin.estado(), partida.resolver(3, "A").estado());
    }

    @Test
    void agotarVitalidadCierraLaPartidaAunqueQuedenCompuertas() {
        var partida = partida(50);
        partida.resolver(1, "50");
        for (int intento = 0; intento < 10; intento++) {
            partida.resolver(2, "R");
        }
        assertEquals("DEFEAT", partida.estado().resultado());
        assertEquals(0, partida.estado().vitalidad());
        assertNotNull(partida.diagnostico(false));
    }

    @Test
    void sincronizaCofreInventarioSeleccionEIdiomaSinAlterarInstantaneas() {
        var partida = partida(50);
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

    private EscapeSession partida(int peso) {
        return new EscapeSession("prueba", Idioma.ES, peso);
    }
}
