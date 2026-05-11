package com.trabajo.troqueles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public final class SpreadsheetReport {
    private static final String HTML_HEAD =
        "<!doctype html><html><head><meta charset=\"utf-8\">"
            + "<title>Reporte Troqueles</title>"
            + "<style>body{font-family:Segoe UI,Arial,sans-serif;padding:24px;background:#f6f8fb;}"
            + "h1{margin:0 0 6px;} .meta{color:#5f6675;margin-bottom:16px;}"
            + "table{border-collapse:collapse;width:100%;background:#fff;border-radius:8px;overflow:hidden;}"
            + "th,td{padding:10px;border-bottom:1px solid #e8ecf3;text-align:left;}"
            + "th{background:#1f2937;color:#fff;} .card{background:#fff;border:1px solid #e8ecf3;padding:12px 14px;"
            + "border-radius:8px;display:inline-block;margin-right:10px;margin-bottom:12px;}</style></head><body>";

    private SpreadsheetReport() {
    }

    public static String buildHtml(
        JTable table,
        DefaultTableModel model,
        String resultText,
        String filterText,
        String totalsText
    ) {
        String headerRow = buildHeaderRow(model);
        String tableRows = buildRowsFromSingleTable(table, model);
        return buildHtmlPage(headerRow, tableRows, resultText, filterText, totalsText);
    }

    public static String buildHtmlFromTables(
        List<JTable> tables,
        DefaultTableModel model,
        String resultText,
        String filterText,
        String totalsText
    ) {
        String headerRow = buildHeaderRow(model);
        String tableRows = buildRowsFromTables(tables, model);
        return buildHtmlPage(headerRow, tableRows, resultText, filterText, totalsText);
    }

    private static String buildHeaderRow(DefaultTableModel model) {
        int columnCount = model.getColumnCount();
        StringBuilder headerRow = new StringBuilder(columnCount * 24);
        for (int c = 0; c < columnCount; c++) {
            headerRow.append("<th>").append(htmlEscape(model.getColumnName(c))).append("</th>");
        }
        return headerRow.toString();
    }

    private static String buildRowsFromSingleTable(JTable table, DefaultTableModel model) {
        int columnCount = model.getColumnCount();
        StringBuilder tableRows = new StringBuilder(Math.max(128, table.getRowCount() * columnCount * 18));
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            appendModelRow(tableRows, model, modelRow, columnCount);
        }
        return tableRows.toString();
    }

    private static String buildRowsFromTables(List<JTable> tables, DefaultTableModel model) {
        int columnCount = model.getColumnCount();
        int expectedRows = 0;
        for (JTable table : tables) {
            expectedRows += table.getRowCount();
        }
        StringBuilder tableRows = new StringBuilder(Math.max(128, expectedRows * columnCount * 18));
        Set<Integer> seen = new HashSet<Integer>(Math.max(16, expectedRows * 2));
        for (JTable table : tables) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (!seen.add(modelRow)) {
                    continue;
                }
                appendModelRow(tableRows, model, modelRow, columnCount);
            }
        }
        return tableRows.toString();
    }

    private static void appendModelRow(StringBuilder out, DefaultTableModel model, int modelRow, int columnCount) {
        out.append("<tr>");
        for (int c = 0; c < columnCount; c++) {
            out.append("<td>")
                .append(htmlEscape(String.valueOf(model.getValueAt(modelRow, c))))
                .append("</td>");
        }
        out.append("</tr>\n");
    }

    private static String buildHtmlPage(String headerRow, String tableRows, String resultText, String filterText, String totalsText) {
        return HTML_HEAD
            + "<h1>Reporte Trabajo Troqueles</h1>"
            + "<div class=\"meta\">Generado desde la aplicacion de escritorio</div>"
            + "<div class=\"card\">" + htmlEscape(resultText) + "</div>"
            + "<div class=\"card\">" + htmlEscape(filterText) + "</div>"
            + "<div class=\"card\">" + htmlEscape(totalsText) + "</div>"
            + "<table><thead><tr>"
            + headerRow
            + "</tr></thead><tbody>"
            + tableRows
            + "</tbody></table></body></html>";
    }

    private static String htmlEscape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
