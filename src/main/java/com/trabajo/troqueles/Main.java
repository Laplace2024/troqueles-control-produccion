package com.trabajo.troqueles;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        if (containsArg(args, "--lan-server")) {
            int exitCode = LanServerLauncher.run(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            return;
        }
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

    private static boolean containsArg(String[] args, String expected) {
        if (args == null || expected == null) {
            return false;
        }
        for (String arg : args) {
            if (expected.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
