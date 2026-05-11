package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class PdfReportWriterTest {

    @Test
    void pdfIncluyeTextoPrincipalDelReporte() throws Exception {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Cod. cliente", "Nombre", "Nº", "X", "Y", "Madera", "Hecho"},
            0
        );
        model.addRow(new Object[]{"100", "Cliente Demo", "T-001", "30", "40", "15", Boolean.TRUE});
        JTable table = new JTable(model);

        File output = Files.createTempFile("troqueles-report-", ".pdf").toFile();
        PdfReportWriter.writeReportFromTables(output, List.of(table), model, "res", "fil", "tot");

        byte[] pdfBytes = Files.readAllBytes(output.toPath());
        String pdfText = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        assertTrue(pdfText.contains("%PDF-1.4"));
        assertTrue(pdfText.contains("Reporte Trabajo Troqueles"));
        assertTrue(pdfText.contains("Cod. cliente"));
        assertTrue(pdfText.contains("X = 30"));
        assertTrue(pdfText.contains("Y = 40"));
        assertTrue(pdfText.contains("terminado"));

        output.delete();
    }
}
