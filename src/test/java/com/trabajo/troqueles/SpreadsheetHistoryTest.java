package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class SpreadsheetHistoryTest {

    @Test
    void undoPreservaEstructuraCompletaTrasAnadirColumna() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Cod. cliente", "Nombre", "Nº"},
            0
        );
        model.addRow(new Object[]{"100", "Cliente Demo", "T-001"});

        SpreadsheetHistory history = new SpreadsheetHistory(10);
        history.push(model);

        model.addColumn("Imagen");
        model.setValueAt("/tmp/foto.png", 0, 3);
        history.push(model);

        history.undo(model);

        assertEquals(3, model.getColumnCount());
        assertEquals("Cod. cliente", model.getColumnName(0));
        assertEquals("100", model.getValueAt(0, 0).toString());
        assertEquals("Cliente Demo", model.getValueAt(0, 1));
        assertEquals("T-001", model.getValueAt(0, 2));
    }
}
