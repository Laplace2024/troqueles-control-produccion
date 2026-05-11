package com.trabajo.troqueles;

import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;

/**
 * Registro de acciones compartidas entre botones de toolbar y atajos de teclado.
 */
final class ToolbarActionRegistry {
    private final Map<String, Action> actions = new HashMap<String, Action>();

    Action register(String key, String label, Runnable runnable) {
        Action action = new AbstractAction(label) {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                runnable.run();
            }
        };
        actions.put(key, action);
        return action;
    }

    JButton newButton(String key, String label, Runnable runnable) {
        Action action = register(key, label, runnable);
        JButton button = new JButton(action);
        button.setText(label);
        return button;
    }

    Action get(String key) {
        return actions.get(key);
    }
}
