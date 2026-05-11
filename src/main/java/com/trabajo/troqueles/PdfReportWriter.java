package com.trabajo.troqueles;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Generador PDF simple sin dependencias externas.
 */
public final class PdfReportWriter {
    private static final Logger LOGGER = Logger.getLogger(PdfReportWriter.class.getName());
    private PdfReportWriter() {
    }

    public static void writeReportFromTables(
        File outputFile,
        List<JTable> tables,
        DefaultTableModel model,
        String resultText,
        String filterText,
        String totalsText
    ) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("Reporte Trabajo Troqueles");
        lines.add("");
        lines.add(safeLine(resultText));
        lines.add(safeLine(filterText));
        lines.add(safeLine(totalsText));
        lines.add("");

        int colCount = model.getColumnCount();
        StringBuilder header = new StringBuilder();
        for (int c = 0; c < colCount; c++) {
            if (c > 0) {
                header.append(" | ");
            }
            header.append(model.getColumnName(c));
        }
        lines.add(safeLine(header.toString()));
        lines.add(repeat('-', Math.min(140, Math.max(20, header.length()))));

        Set<Integer> seenRows = new HashSet<Integer>();
        for (JTable table : tables) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (!seenRows.add(modelRow)) {
                    continue;
                }
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < colCount; c++) {
                    if (c > 0) {
                        row.append(" | ");
                    }
                    Object value = model.getValueAt(modelRow, c);
                    row.append(formatCellForRow(model.getColumnName(c), value));
                }
                lines.add(safeLine(row.toString()));
            }
        }

        byte[] pdf = buildPdf(lines);
        Files.write(outputFile.toPath(), pdf);
    }

    private static byte[] buildPdf(List<String> allLines) {
        final int pageHeight = 842;
        final int topMargin = 50;
        final int bottomMargin = 50;
        final int lineHeight = 14;
        final int maxCharsPerLine = 95;
        List<String> wrappedLines = wrapLines(allLines, maxCharsPerLine);
        final int maxLinesPerPage = Math.max(1, (pageHeight - topMargin - bottomMargin) / lineHeight);

        List<String> pagesContent = new ArrayList<String>();
        for (int i = 0; i < wrappedLines.size(); i += maxLinesPerPage) {
            int end = Math.min(wrappedLines.size(), i + maxLinesPerPage);
            pagesContent.add(buildPageContent(wrappedLines.subList(i, end), topMargin, lineHeight, pageHeight));
        }
        if (pagesContent.isEmpty()) {
            pagesContent.add(buildPageContent(new ArrayList<String>(), topMargin, lineHeight, pageHeight));
        }

        List<String> objects = new ArrayList<String>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>"); // 1

        int pageCount = pagesContent.size();
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            int pageObj = 4 + (i * 2);
            kids.append(pageObj).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>"); // 2
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"); // 3

        for (int i = 0; i < pageCount; i++) {
            int contentObj = 5 + (i * 2);
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObj + " 0 R >>");
            String stream = pagesContent.get(i);
            byte[] streamBytes = stream.getBytes(StandardCharsets.ISO_8859_1);
            objects.add("<< /Length " + streamBytes.length + " >>\nstream\n" + stream + "\nendstream");
        }

        return buildPdfDocument(objects);
    }

    private static String buildPageContent(List<String> lines, int topMargin, int lineHeight, int pageHeight) {
        StringBuilder sb = new StringBuilder();
        sb.append("BT\n/F1 10 Tf\n");
        int y = pageHeight - topMargin;
        for (String raw : lines) {
            String line = escapePdfText(raw);
            sb.append("1 0 0 1 40 ").append(y).append(" Tm (").append(line).append(") Tj\n");
            y -= lineHeight;
        }
        sb.append("ET");
        return sb.toString();
    }

    private static byte[] buildPdfDocument(List<String> objects) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<Integer>();
            offsets.add(0);

            writeIso(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(out.size());
                writeIso(out, (i + 1) + " 0 obj\n");
                writeIso(out, objects.get(i));
                writeIso(out, "\nendobj\n");
            }

            int xrefOffset = out.size();
            writeIso(out, "xref\n");
            writeIso(out, "0 " + (objects.size() + 1) + "\n");
            writeIso(out, "0000000000 65535 f \n");
            for (int i = 1; i < offsets.size(); i++) {
                writeIso(out, String.format(Locale.US, "%010d 00000 n \n", offsets.get(i)));
            }
            writeIso(out, "trailer\n");
            writeIso(out, "<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
            writeIso(out, "startxref\n");
            writeIso(out, String.valueOf(xrefOffset));
            writeIso(out, "\n%%EOF");
            return out.toByteArray();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo construir el PDF en memoria.", ex);
            throw new IllegalStateException("No se pudo construir el PDF en memoria.", ex);
        }
    }

    private static List<String> wrapLines(List<String> input, int maxCharsPerLine) {
        List<String> wrapped = new ArrayList<String>();
        for (String line : input) {
            String safe = safeLine(line);
            if (safe.length() <= maxCharsPerLine) {
                wrapped.add(safe);
                continue;
            }
            int start = 0;
            while (start < safe.length()) {
                int end = Math.min(safe.length(), start + maxCharsPerLine);
                if (end < safe.length()) {
                    int split = safe.lastIndexOf(' ', end);
                    if (split > start + 10) {
                        end = split;
                    }
                }
                String piece = safe.substring(start, end).trim();
                wrapped.add(piece);
                start = end;
                while (start < safe.length() && safe.charAt(start) == ' ') {
                    start++;
                }
            }
        }
        return wrapped;
    }

    private static void writeIso(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escapePdfText(String line) {
        String safe = line == null ? "" : line;
        safe = safe.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        StringBuilder cleaned = new StringBuilder(safe.length());
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                cleaned.append(' ');
            } else if (ch >= 32 && ch <= 255) {
                cleaned.append(ch);
            } else {
                cleaned.append('?');
            }
        }
        return cleaned.toString();
    }

    private static String safeLine(String text) {
        return text == null ? "" : text;
    }

    /**
     * Formatea el valor de una celda para el reporte PDF.
     * Los booleanos se traducen a "terminado" / "no terminado" en lugar de mostrar "true" / "false".
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

    /**
     * Formato de celda al imprimir cada fila. Para las columnas X / Y se antepone el nombre
     * de la medida ("X = 30", "Y = 40") para que en el PDF se sepa que cifra corresponde a
     * cada eje incluso cuando varias columnas aparezcan en la misma linea.
     * Si la celda esta vacia no se anade el prefijo para no llenar el reporte de cabeceras sueltas.
     */
    private static String formatCellForRow(String columnName, Object value) {
        String formatted = formatCellValue(value);
        if (formatted.isEmpty()) {
            return formatted;
        }
        if ("X".equalsIgnoreCase(columnName) || "Y".equalsIgnoreCase(columnName)) {
            return columnName + " = " + formatted;
        }
        return formatted;
    }

    private static String repeat(char ch, int count) {
        StringBuilder sb = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
