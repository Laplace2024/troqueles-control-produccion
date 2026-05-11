package com.trabajo.troqueles;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Instalador de atajos de teclado basado en acciones compartidas.
 */
final class ShortcutBindingsInstaller {
    private ShortcutBindingsInstaller() {
    }

    static void install(JComponent root, ToolbarActionRegistry registry) {
        if (root == null || registry == null) {
            return;
        }
        bind(root, "control N", "add-row", "add-row", registry);
        bind(root, "DELETE", "delete-row", "delete-row", registry);
        bind(root, "control Z", "undo", "undo", registry);
        bind(root, "control Y", "redo", "redo", registry);
        bind(root, "control S", "save-all", "save-all", registry);
        bind(root, "control shift S", "save-visible", "save-visible", registry);
        bind(root, "control O", "load-csv", "load-csv", registry);
        bind(root, "control E", "export-html", "export-html", registry);
        bind(root, "control F", "focus-search", "focus-search", registry);
        bind(root, "F2", "rename-sheet", "rename-sheet", registry);
        bind(root, "F1", "user-manual", "user-manual", registry);
        bind(root, "control D", "open-dashboard", "open-dashboard", registry);
    }

    private static void bind(
        JComponent root,
        String keyStroke,
        String bindingId,
        String actionKey,
        ToolbarActionRegistry registry
    ) {
        javax.swing.Action action = registry.get(actionKey);
        if (action == null) {
            return;
        }
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyStroke), bindingId);
        root.getActionMap().put(bindingId, action);
    }
}
