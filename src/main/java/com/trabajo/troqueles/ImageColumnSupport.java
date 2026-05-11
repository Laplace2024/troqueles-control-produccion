package com.trabajo.troqueles;

import java.awt.Image;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Soporte de columna de imagen: miniaturas, sizing y ajuste de altura de filas.
 */
final class ImageColumnSupport {
    private static final Logger LOGGER = Logger.getLogger(ImageColumnSupport.class.getName());
    static final String COLUMN_NAME = "Imagen";
    private static final int THUMB_HEIGHT = 56;
    private static final int THUMB_MAX_WIDTH = 96;
    private static final int ROW_HEIGHT = THUMB_HEIGHT + 8;
    private static final int COLUMN_WIDTH = THUMB_MAX_WIDTH + 16;

    private final DefaultTableModel tableModel;
    private final List<JTable> dataTables;
    private final Function<String, Integer> columnIndexResolver;
    private final Map<String, ImageIcon> thumbCache = new HashMap<String, ImageIcon>();

    ImageColumnSupport(
        DefaultTableModel tableModel,
        List<JTable> dataTables,
        Function<String, Integer> columnIndexResolver
    ) {
        this.tableModel = tableModel;
        this.dataTables = dataTables;
        this.columnIndexResolver = columnIndexResolver;
    }

    ImageIcon loadImageThumb(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        ImageIcon cached = thumbCache.get(path);
        if (cached != null) {
            return cached;
        }
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }
        try {
            ImageIcon raw = new ImageIcon(file.getAbsolutePath());
            Image image = raw.getImage();
            int srcW = image.getWidth(null);
            int srcH = image.getHeight(null);
            if (srcW <= 0 || srcH <= 0) {
                return null;
            }
            double scale = (double) THUMB_HEIGHT / (double) srcH;
            int targetW = (int) Math.round(srcW * scale);
            if (targetW > THUMB_MAX_WIDTH) {
                targetW = THUMB_MAX_WIDTH;
            }
            Image scaled = image.getScaledInstance(targetW, THUMB_HEIGHT, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            thumbCache.put(path, icon);
            return icon;
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "No se pudo generar miniatura para: " + path, ex);
            return null;
        }
    }

    void removeFromCache(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            thumbCache.remove(imagePath);
        }
    }

    void applyImageColumnSizing() {
        int modelCol = columnIndexResolver.apply(COLUMN_NAME);
        if (modelCol < 0) {
            return;
        }
        for (JTable table : dataTables) {
            int viewCol = table.convertColumnIndexToView(modelCol);
            if (viewCol < 0) {
                continue;
            }
            TableColumn tc = table.getColumnModel().getColumn(viewCol);
            tc.setPreferredWidth(COLUMN_WIDTH);
            tc.setMinWidth(COLUMN_WIDTH);
        }
    }

    void adjustRowHeightForImage(int modelRow) {
        for (JTable table : dataTables) {
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow < 0) {
                continue;
            }
            if (table.getRowHeight(viewRow) < ROW_HEIGHT) {
                table.setRowHeight(viewRow, ROW_HEIGHT);
            }
        }
    }

    void resetRowHeightForRow(int modelRow) {
        for (JTable table : dataTables) {
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow < 0) {
                continue;
            }
            table.setRowHeight(viewRow, defaultRowHeightForTable(table));
        }
    }

    void adjustRowHeightsForExistingImages() {
        int modelCol = columnIndexResolver.apply(COLUMN_NAME);
        if (modelCol < 0) {
            return;
        }
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object value = tableModel.getValueAt(row, modelCol);
            String path = value == null ? "" : String.valueOf(value).trim();
            if (!path.isEmpty()) {
                adjustRowHeightForImage(row);
            }
        }
    }

    void handleImageCellUpdate(TableModelEvent event) {
        if (event == null || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int col = event.getColumn();
        if (col == TableModelEvent.ALL_COLUMNS) {
            return;
        }
        int imageCol = columnIndexResolver.apply(COLUMN_NAME);
        if (imageCol < 0 || col != imageCol) {
            return;
        }
        int row = event.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        Object raw = tableModel.getValueAt(row, imageCol);
        String path = raw == null ? "" : String.valueOf(raw).trim();
        if (path.isEmpty()) {
            resetRowHeightForRow(row);
        } else {
            removeFromCache(path);
            adjustRowHeightForImage(row);
        }
    }

    private int defaultRowHeightForTable(JTable table) {
        return table == null ? 24 : Math.max(20, table.getFont().getSize() + 12);
    }
}
