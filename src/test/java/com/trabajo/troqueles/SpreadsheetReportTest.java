package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class SpreadsheetReportTest {

    @Test
    void htmlIncluyeTodasLasColumnasDelModelo() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Cod. cliente", "Nombre", "Nº", "X", "Y", "Madera"},
            0
        );
        model.addRow(new Object[]{"100", "Cliente Demo", "T-001", "30", "40", "15"});
        JTable table = new JTable(model);

        String html = SpreadsheetReport.buildHtml(table, model, "res", "fil", "tot");

        assertTrue(html.contains("Cod. cliente"));
        assertTrue(html.contains("Cliente Demo"));
        assertTrue(html.contains("Madera"));
        assertTrue(html.contains("<th>"));
    }
}
