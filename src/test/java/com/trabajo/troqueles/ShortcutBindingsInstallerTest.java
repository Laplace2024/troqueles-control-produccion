package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

class ShortcutBindingsInstallerTest {

    @Test
    void instalaAtajosConLasMismasAccionesCompartidas() {
        ToolbarActionRegistry registry = new ToolbarActionRegistry();
        AtomicInteger addRowRuns = new AtomicInteger();
        AtomicInteger undoRuns = new AtomicInteger();

        Action addRowAction = registry.register("add-row", "Add row", addRowRuns::incrementAndGet);
        Action undoAction = registry.register("undo", "Undo", undoRuns::incrementAndGet);

        JPanel root = new JPanel();
        ShortcutBindingsInstaller.install(root, registry);

        Object addRowBinding = root.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control N"));
        Object undoBinding = root.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control Z"));
        assertNotNull(addRowBinding);
        assertNotNull(undoBinding);
        assertSame(addRowAction, root.getActionMap().get(addRowBinding));
        assertSame(undoAction, root.getActionMap().get(undoBinding));

        root.getActionMap().get(addRowBinding).actionPerformed(null);
        root.getActionMap().get(undoBinding).actionPerformed(null);
        assertEquals(1, addRowRuns.get());
        assertEquals(1, undoRuns.get());
    }
}
