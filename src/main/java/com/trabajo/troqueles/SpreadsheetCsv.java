package com.trabajo.troqueles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public final class SpreadsheetCsv {
    private SpreadsheetCsv() {
    }

    public static void exportModel(DefaultTableModel model, File file) throws IOException {
        try (BufferedWriter writer = createUtf8Writer(normalizeCsvPath(file))) {
            writer.write(csvHeader(model) + "\n");
            for (int row = 0; row < model.getRowCount(); row++) {
                writer.write(csvRow(model, row) + "\n");
            }
        }
    }

    public static void exportVisible(DefaultTableModel model, JTable table, File file) throws IOException {
        try (BufferedWriter writer = createUtf8Writer(normalizeCsvPath(file))) {
            writer.write(csvHeader(model) + "\n");
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                writer.write(csvRow(model, modelRow) + "\n");
            }
        }
    }

    /**
     * Exporta las filas visibles en cualquiera de las tablas (union sin duplicar por fila de modelo).
     */
    public static void exportVisibleAll(DefaultTableModel model, List<JTable> tables, File file) throws IOException {
        try (BufferedWriter writer = createUtf8Writer(normalizeCsvPath(file))) {
            writer.write(csvHeader(model) + "\n");
            Set<Integer> seen = new HashSet<Integer>();
            for (JTable table : tables) {
                for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (seen.add(modelRow)) {
                        writer.write(csvRow(model, modelRow) + "\n");
                    }
                }
            }
        }
    }

    public static CsvData loadRows(File file) throws IOException {
        IOException last = null;
        Charset[] charsets = new Charset[]{StandardCharsets.UTF_8, Charset.forName("windows-1252")};
        for (Charset charset : charsets) {
            try {
                return loadRowsWithCharset(file, charset);
            } catch (IOException ex) {
                last = ex;
            }
        }
        throw last == null ? new IOException("No se pudo leer el CSV.") : last;
    }

    private static CsvData loadRowsWithCharset(File file, Charset charset) throws IOException {
        List<String> headers = new ArrayList<String>();
        List<Object[]> rows = new ArrayList<Object[]>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    String[] headerValues = CsvUtils.parseCsvLine(stripBom(line));
                    for (String value : headerValues) {
                        headers.add(value);
                    }
                    // Si la primera linea no aporta cabeceras (CSV sin cabecera o vacia),
                    // se generan nombres neutros "Columna 1", "Columna 2"... Antes se usaban
                    // "Concepto/Valor/Categoria" pero esas columnas ya no forman parte del modelo
                    // de la hoja de troqueles, por lo que la carga rompia el esquema.
                    if (headers.isEmpty()) {
                        headers.add("Columna 1");
                    }
                    continue;
                }
                String[] values = CsvUtils.parseCsvLine(line);
                if (values.length == 0) {
                    continue;
                }
                while (headers.size() < values.length) {
                    headers.add("Columna " + (headers.size() + 1));
                }
                Object[] row = new Object[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    row[i] = i < values.length ? values[i] : "";
                }
                rows.add(row);
            }
        }
        return new CsvData(headers, rows);
    }

    public static File normalizeCsvPath(File selectedFile) {
        String path = selectedFile.getAbsolutePath();
        if (!path.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            path += ".csv";
        }
        return new File(path);
    }

    private static BufferedWriter createUtf8Writer(File file) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
    }

    private static String stripBom(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        return line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }

    private static String csvEscape(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String csvHeader(DefaultTableModel model) {
        StringBuilder builder = new StringBuilder();
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (col > 0) {
                builder.append(",");
            }
            builder.append(csvEscape(model.getColumnName(col)));
        }
        return builder.toString();
    }

    private static String csvRow(DefaultTableModel model, int row) {
        StringBuilder builder = new StringBuilder();
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (col > 0) {
                builder.append(",");
            }
            builder.append(csvEscape(formatCellValue(model.getValueAt(row, col))));
        }
        return builder.toString();
    }

    /**
     * Formatea el valor de una celda para CSV.
     * Los booleanos se exportan como "terminado" / "no terminado" para que coincidan con el reporte PDF/HTML.
     * Al recargar el CSV, esos textos se reconvierten a Boolean para columnas Boolean.
     */
    private static String formatCellValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "terminado" : "no terminado";
        }
        return String.valueOf(value);
    }

    public static final class CsvData {
        private final List<String> headers;
        private final List<Object[]> rows;

        public CsvData(List<String> headers, List<Object[]> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Object[]> getRows() {
            return rows;
        }
    }
}
