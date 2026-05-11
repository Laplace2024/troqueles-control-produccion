package com.trabajo.troqueles;

import javax.swing.table.DefaultTableModel;

/**
 * Utilidad para resolver indices de columna del modelo por su nombre de cabecera.
 * Antes existian constantes COLUMN_CONCEPTO/VALOR/CATEGORIA que ya no corresponden al modelo
 * actual de la hoja de troqueles; se eliminaron para evitar acoplamientos obsoletos.
 */
public final class SpreadsheetSchema {

    private SpreadsheetSchema() {
    }

    /**
     * Indice del modelo cuyo nombre coincide con {@code logicalName} (ignora mayusculas).
     *
     * @return indice &gt;= 0, o -1 si no existe la columna
     */
    public static int columnIndex(DefaultTableModel model, String logicalName) {
        if (model == null || logicalName == null) {
            return -1;
        }
        for (int c = 0; c < model.getColumnCount(); c++) {
            if (logicalName.equalsIgnoreCase(model.getColumnName(c))) {
                return c;
            }
        }
        return -1;
    }
}
