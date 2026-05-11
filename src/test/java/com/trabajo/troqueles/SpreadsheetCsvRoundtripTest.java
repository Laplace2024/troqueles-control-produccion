package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class SpreadsheetCsvRoundtripTest {

    @Test
    void exportarEImportarConservaCabecerasYValores() throws Exception {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Cod. cliente", "Nombre", "Nº", "X", "Y", "Hecho"},
            0
        );
        model.addRow(new Object[]{"100", "Cliente, Demo \"A\"", "T-001", "30", "40", Boolean.TRUE});
        model.addRow(new Object[]{"101", "Cliente B", "T-002", "31", "41", Boolean.FALSE});

        File tempCsv = Files.createTempFile("troqueles-roundtrip-", ".csv").toFile();
        SpreadsheetCsv.exportModel(model, tempCsv);

        SpreadsheetCsv.CsvData loaded = SpreadsheetCsv.loadRows(tempCsv);
        assertEquals(6, loaded.getHeaders().size());
        assertEquals("Cod. cliente", loaded.getHeaders().get(0));
        assertEquals("Hecho", loaded.getHeaders().get(5));

        assertEquals(2, loaded.getRows().size());
        Object[] row1 = loaded.getRows().get(0);
        Object[] row2 = loaded.getRows().get(1);
        assertEquals("100", row1[0]);
        assertEquals("Cliente, Demo \"A\"", row1[1]);
        assertEquals("T-001", row1[2]);
        assertEquals("30", row1[3]);
        assertEquals("40", row1[4]);
        assertEquals("terminado", row1[5]);

        assertEquals("101", row2[0]);
        assertEquals("Cliente B", row2[1]);
        assertEquals("T-002", row2[2]);
        assertEquals("31", row2[3]);
        assertEquals("41", row2[4]);
        assertEquals("no terminado", row2[5]);

        tempCsv.delete();
    }
}
