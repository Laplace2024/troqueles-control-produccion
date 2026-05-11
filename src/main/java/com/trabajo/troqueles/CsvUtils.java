package com.trabajo.troqueles;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades compartidas para parseo CSV simple (coma + comillas dobles escapadas).
 */
final class CsvUtils {
    private CsvUtils() {
    }

    static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}
