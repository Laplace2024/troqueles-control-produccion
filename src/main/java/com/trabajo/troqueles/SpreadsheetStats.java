package com.trabajo.troqueles;

import java.util.Locale;
import javax.swing.table.DefaultTableModel;

public final class SpreadsheetStats {
    private SpreadsheetStats() {
    }

    public static Double tryParseDouble(String valueText) {
        if (valueText == null) {
            return null;
        }
        String normalized = valueText.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static NumericStats calculateColumnStats(DefaultTableModel model, int columnIndex) {
        double sum = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        int validCount = 0;

        for (int row = 0; row < model.getRowCount(); row++) {
            Object cellValue = model.getValueAt(row, columnIndex);
            if (cellValue == null) {
                continue;
            }
            Double numericValue = tryParseDouble(cellValue.toString());
            if (numericValue == null) {
                continue;
            }
            sum += numericValue;
            validCount++;
            if (numericValue > max) {
                max = numericValue;
            }
        }

        if (validCount == 0) {
            return null;
        }
        double average = sum / validCount;
        double maxOverSumPercent = sum == 0.0 ? 0.0 : (max / sum) * 100.0;
        return new NumericStats(sum, average, max, maxOverSumPercent, validCount);
    }

    public static final class NumericStats {
        private final double sum;
        private final double average;
        private final double max;
        private final double maxOverSumPercent;
        private final int count;

        public NumericStats(double sum, double average, double max, double maxOverSumPercent, int count) {
            this.sum = sum;
            this.average = average;
            this.max = max;
            this.maxOverSumPercent = maxOverSumPercent;
            this.count = count;
        }

        public String toLabel() {
            return String.format(
                Locale.US,
                "Resultado: suma=%.2f | promedio=%.2f | max=%.2f | max/suma=%.2f%% | n=%d",
                sum, average, max, maxOverSumPercent, count
            );
        }
    }
}
