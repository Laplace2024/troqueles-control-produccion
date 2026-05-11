package com.trabajo.troqueles;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // Si no esta disponible Nimbus, se usa el look por defecto.
            }
            SpreadsheetFrame frame = new SpreadsheetFrame();
            frame.setVisible(true);
        });
    }
}
