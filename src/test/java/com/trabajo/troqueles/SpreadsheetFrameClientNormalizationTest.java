package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las utilidades de normalizacion de nombres y codigos de cliente.
 * No dependen de un mapa de clientes especifico (el listado real no forma parte de este repositorio):
 * solo se valida la transformacion sobre cadenas de ejemplo neutras.
 */
class SpreadsheetFrameClientNormalizationTest {

    @Test
    void normalizaNombreClienteIgnoraMayusculasTildesYSufijosLegales() throws Exception {
        assertEquals("cliente ejemplo", invokeNormalizeName("  Cliente Éjemplo S.L.  "));
        assertEquals("empresa demo", invokeNormalizeName("EMPRESA DEMO, S.A."));
        assertEquals("cano", invokeNormalizeName("Caño C.B."));
    }

    @Test
    void normalizaCodigoClienteQuitaBasuraYCerosALaIzquierda() throws Exception {
        assertEquals("7", invokeNormalizeCode("0007"));
        assertEquals("15", invokeNormalizeCode("  C-0015  "));
        assertEquals("0", invokeNormalizeCode("0000"));
    }

    @Test
    void normalizarValorVacioDevuelveCadenaVacia() throws Exception {
        assertEquals("", invokeNormalizeName(""));
        assertEquals("", invokeNormalizeCode(""));
    }

    private static String invokeNormalizeName(String input) throws Exception {
        Method method = SpreadsheetFrame.class.getDeclaredMethod("normalizeClientNameForLookup", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, input);
    }

    private static String invokeNormalizeCode(String input) throws Exception {
        Method method = SpreadsheetFrame.class.getDeclaredMethod("normalizeClientCode", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, input);
    }
}
