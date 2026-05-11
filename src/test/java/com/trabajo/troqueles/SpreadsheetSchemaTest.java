package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class SpreadsheetSchemaTest {

    @Test
    void resuelveIndicesPorNombreDeCabecera() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Cod. cliente", "Nombre", "Nº", "X", "Y", "Madera"},
            0
        );
        assertEquals(0, SpreadsheetSchema.columnIndex(model, "Cod. cliente"));
        assertEquals(3, SpreadsheetSchema.columnIndex(model, "X"));
        assertEquals(5, SpreadsheetSchema.columnIndex(model, "Madera"));
    }

    @Test
    void resolucionEsCaseInsensitive() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Cod. Cliente", "Nombre"}, 0);
        assertEquals(0, SpreadsheetSchema.columnIndex(model, "cod. cliente"));
        assertEquals(1, SpreadsheetSchema.columnIndex(model, "NOMBRE"));
    }

    @Test
    void devuelveMenosUnoSiFaltaColumna() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"A", "B"}, 0);
        assertEquals(-1, SpreadsheetSchema.columnIndex(model, "Madera"));
    }

    @Test
    void devuelveMenosUnoSiModeloEsNulo() {
        assertEquals(-1, SpreadsheetSchema.columnIndex(null, "X"));
    }
}
