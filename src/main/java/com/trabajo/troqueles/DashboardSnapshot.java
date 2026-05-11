package com.trabajo.troqueles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Genera la vista que consume el dashboard web a partir del modelo de la hoja de troqueles.
 * <p>
 * Mapeo a la estructura {@link DashboardServer.RowData} (que el front espera con campos
 * {@code concepto}, {@code valor}, {@code categoria}):
 * <ul>
 *   <li><b>concepto</b>: codigo + nombre del cliente; si solo hay uno, se usa el que exista.
 *       Si no hay cliente, se rellena con "Troquel {numero}".</li>
 *   <li><b>valor</b>: medida X de la pieza (numerico). Sirve como serie principal para los graficos.</li>
 *   <li><b>categoria</b>: grosor de madera (columna "Madera"); si no hay valor se agrupa como "Sin clasificar".</li>
 * </ul>
 * Las resoluciones se hacen por nombre de cabecera con {@link SpreadsheetSchema#columnIndex(DefaultTableModel, String)}
 * para no acoplarse al orden actual del modelo.
 */
public final class DashboardSnapshot {

    private static final String COL_COD_CLIENTE = "Cod. cliente";
    private static final String COL_NOMBRE = "Nombre";
    private static final String COL_NUM = "Nº";
    private static final String COL_X = "X";
    private static final String COL_MADERA = "Madera";

    private DashboardSnapshot() {
    }

    public static List<DashboardServer.RowData> buildVisibleRows(JTable table, DefaultTableModel model) {
        ColumnRefs refs = ColumnRefs.from(model);
        List<DashboardServer.RowData> rows = new ArrayList<DashboardServer.RowData>(table.getRowCount());
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            rows.add(buildRow(model, modelRow, refs, viewRow + 1));
        }
        return rows;
    }

    /**
     * Igual que {@link #buildVisibleRows(JTable, DefaultTableModel)} pero unificando varias vistas filtradas
     * y deduplicando por fila de modelo (cada troquel aparece una sola vez en el dashboard).
     */
    public static List<DashboardServer.RowData> buildVisibleRowsFromTables(List<JTable> tables, DefaultTableModel model) {
        ColumnRefs refs = ColumnRefs.from(model);
        int expectedRows = 0;
        for (JTable table : tables) {
            expectedRows += table.getRowCount();
        }
        List<DashboardServer.RowData> rows = new ArrayList<DashboardServer.RowData>(Math.max(16, expectedRows));
        Set<Integer> seenModelRows = new HashSet<Integer>(Math.max(16, expectedRows * 2));
        int sequence = 0;
        for (JTable table : tables) {
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (!seenModelRows.add(modelRow)) {
                    continue;
                }
                sequence++;
                rows.add(buildRow(model, modelRow, refs, sequence));
            }
        }
        return rows;
    }

    private static DashboardServer.RowData buildRow(DefaultTableModel model, int modelRow, ColumnRefs refs, int fallbackIndex) {
        String concepto = buildConceptoLabel(model, modelRow, refs, fallbackIndex);
        double valor = parseValor(model, modelRow, refs.xColumn);
        String categoria = cellText(model, modelRow, refs.maderaColumn, "Sin clasificar");
        return new DashboardServer.RowData(concepto, valor, categoria);
    }

    private static String buildConceptoLabel(DefaultTableModel model, int modelRow, ColumnRefs refs, int fallbackIndex) {
        String codigo = cellText(model, modelRow, refs.codColumn, "");
        String nombre = cellText(model, modelRow, refs.nombreColumn, "");
        String num = cellText(model, modelRow, refs.numColumn, "");

        StringBuilder builder = new StringBuilder();
        if (!codigo.isEmpty()) {
            builder.append(codigo);
        }
        if (!nombre.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" - ");
            }
            builder.append(nombre);
        }
        if (builder.length() == 0) {
            builder.append("Troquel ").append(fallbackIndex);
        }
        if (!num.isEmpty()) {
            builder.append(" (Nº ").append(num).append(")");
        }
        return builder.toString();
    }

    private static double parseValor(DefaultTableModel model, int modelRow, int column) {
        String raw = cellText(model, modelRow, column, "");
        Double parsed = SpreadsheetStats.tryParseDouble(raw);
        return parsed == null ? 0.0 : parsed;
    }

    public static String buildDataScript(List<DashboardServer.RowData> rows) {
        StringBuilder builder = new StringBuilder(Math.max(128, rows.size() * 72));
        builder.append("window.TROQUELES_DATA = [\n");
        for (int i = 0; i < rows.size(); i++) {
            DashboardServer.RowData row = rows.get(i);
            builder.append("  { concepto: \"")
                .append(escapeJsString(row.getConcepto()))
                .append("\", valor: \"")
                .append(String.format(Locale.US, "%.2f", row.getValor()))
                .append("\", categoria: \"")
                .append(escapeJsString(row.getCategoria()))
                .append("\" }");
            if (i < rows.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("];\n");
        return builder.toString();
    }

    private static String cellText(DefaultTableModel model, int modelRow, int modelColumn, String fallback) {
        if (modelColumn < 0 || modelColumn >= model.getColumnCount()) {
            return fallback;
        }
        Object value = model.getValueAt(modelRow, modelColumn);
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private static String escapeJsString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Indices resueltos por nombre de cabecera, calculados una vez por snapshot. */
    private static final class ColumnRefs {
        final int codColumn;
        final int nombreColumn;
        final int numColumn;
        final int xColumn;
        final int maderaColumn;

        private ColumnRefs(int codColumn, int nombreColumn, int numColumn, int xColumn, int maderaColumn) {
            this.codColumn = codColumn;
            this.nombreColumn = nombreColumn;
            this.numColumn = numColumn;
            this.xColumn = xColumn;
            this.maderaColumn = maderaColumn;
        }

        static ColumnRefs from(DefaultTableModel model) {
            return new ColumnRefs(
                SpreadsheetSchema.columnIndex(model, COL_COD_CLIENTE),
                SpreadsheetSchema.columnIndex(model, COL_NOMBRE),
                SpreadsheetSchema.columnIndex(model, COL_NUM),
                SpreadsheetSchema.columnIndex(model, COL_X),
                SpreadsheetSchema.columnIndex(model, COL_MADERA)
            );
        }
    }
}
