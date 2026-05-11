package com.trabajo.troqueles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * Historial deshacer/rehacer con instantaneas completas del modelo (todas las columnas y
 * encabezados), coherente con tablas dinamicas.
 */
public class SpreadsheetHistory {
    private final Deque<TableSnapshot> undoStack = new ArrayDeque<TableSnapshot>();
    private final Deque<TableSnapshot> redoStack = new ArrayDeque<TableSnapshot>();
    private final int maxSnapshots;

    public SpreadsheetHistory(int maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }

    public void push(DefaultTableModel model) {
        TableSnapshot snapshot = snapshot(model);
        if (!undoStack.isEmpty() && undoStack.peekLast().equalsContent(snapshot)) {
            return;
        }
        undoStack.addLast(snapshot);
        if (undoStack.size() > maxSnapshots) {
            undoStack.removeFirst();
        }
        redoStack.clear();
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo(DefaultTableModel model) {
        if (!canUndo()) {
            return;
        }
        TableSnapshot current = undoStack.removeLast();
        redoStack.addLast(current);
        TableSnapshot previous = undoStack.peekLast();
        if (previous != null) {
            restore(model, previous);
        }
    }

    public void redo(DefaultTableModel model) {
        if (!canRedo()) {
            return;
        }
        TableSnapshot next = redoStack.removeLast();
        undoStack.addLast(next);
        restore(model, next);
    }

    private TableSnapshot snapshot(DefaultTableModel model) {
        int columnCount = model.getColumnCount();
        String[] headers = new String[columnCount];
        for (int c = 0; c < columnCount; c++) {
            headers[c] = model.getColumnName(c);
        }
        List<Object[]> rows = new ArrayList<Object[]>();
        for (int row = 0; row < model.getRowCount(); row++) {
            Object[] cells = new Object[columnCount];
            for (int col = 0; col < columnCount; col++) {
                cells[col] = model.getValueAt(row, col);
            }
            rows.add(cells);
        }
        return new TableSnapshot(headers, rows);
    }

    private void restore(DefaultTableModel model, TableSnapshot snap) {
        model.setColumnCount(0);
        for (String header : snap.headers) {
            model.addColumn(header);
        }
        model.setRowCount(0);
        for (Object[] row : snap.rows) {
            model.addRow(row);
        }
    }

    private static final class TableSnapshot {
        private final String[] headers;
        private final List<Object[]> rows;

        private TableSnapshot(String[] headers, List<Object[]> rows) {
            this.headers = headers.clone();
            this.rows = deepCopyRows(rows);
        }

        private static List<Object[]> deepCopyRows(List<Object[]> rows) {
            List<Object[]> copy = new ArrayList<Object[]>(rows.size());
            for (Object[] row : rows) {
                copy.add(row.clone());
            }
            return copy;
        }

        private boolean equalsContent(TableSnapshot other) {
            if (other == null || !Arrays.equals(headers, other.headers) || rows.size() != other.rows.size()) {
                return false;
            }
            for (int i = 0; i < rows.size(); i++) {
                if (!Arrays.deepEquals(rows.get(i), other.rows.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
