package com.trabajo.troqueles;

import java.awt.Component;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Coordinador de busqueda y filtrado de filas visibles.
 */
final class SearchAndFilter {
    private static final Logger LOGGER = Logger.getLogger(SearchAndFilter.class.getName());
    private final DefaultTableModel tableModel;
    private final List<TableRowSorter<DefaultTableModel>> dataSorters;
    private final List<JTable> dataTables;
    private final Function<String, Integer> columnIndexResolver;
    private final Runnable refreshTotalsAction;
    private final Component dialogParent;

    private JTextField searchField;
    private JComboBox<String> searchScopeCombo;
    private JLabel filterLabel;

    SearchAndFilter(
        DefaultTableModel tableModel,
        List<TableRowSorter<DefaultTableModel>> dataSorters,
        List<JTable> dataTables,
        Function<String, Integer> columnIndexResolver,
        Runnable refreshTotalsAction,
        Component dialogParent
    ) {
        this.tableModel = tableModel;
        this.dataSorters = dataSorters;
        this.dataTables = dataTables;
        this.columnIndexResolver = columnIndexResolver;
        this.refreshTotalsAction = refreshTotalsAction;
        this.dialogParent = dialogParent;
    }

    void bindControls(JTextField searchField, JComboBox<String> searchScopeCombo, JLabel filterLabel) {
        this.searchField = searchField;
        this.searchScopeCombo = searchScopeCombo;
        this.filterLabel = filterLabel;
    }

    void installDefaultListeners() {
        if (searchField != null) {
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    applyCombinedFilter();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    applyCombinedFilter();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    applyCombinedFilter();
                }
            });
        }
        if (searchScopeCombo != null) {
            searchScopeCombo.addActionListener(event -> applyCombinedFilter());
        }
    }

    void clearFilters() {
        if (searchField != null) {
            searchField.setText("");
        }
        applyCombinedFilter();
    }

    void applyCombinedFilter() {
        if (dataSorters.isEmpty()) {
            return;
        }
        String searchText = searchField == null ? "" : searchField.getText().trim();
        String searchScope = searchScopeCombo == null ? "Todo" : (String) searchScopeCombo.getSelectedItem();
        if (searchText.isEmpty()) {
            dataSorters.get(0).setRowFilter(null);
        } else {
            dataSorters.get(0).setRowFilter(buildSearchFilter(searchScope, searchText));
        }
        if (filterLabel != null) {
            filterLabel.setText(
                "Busqueda[" + (searchScope == null ? "Todo" : searchScope) + "]: "
                    + (searchText.isEmpty() ? "(vacia)" : searchText)
            );
        }
        if (!dataTables.isEmpty()) {
            dataTables.get(0).repaint();
        }
        refreshTotalsAction.run();
    }

    void showSearchHelp() {
        JOptionPane.showMessageDialog(
            dialogParent,
            "Busqueda:\n\n"
                + "- 'En: Todo' busca en toda la fila.\n"
                + "- 'En: Cod. cliente' filtra por codigo de cliente.\n"
                + "- 'En: Nombre' filtra por nombre del cliente.\n"
                + "- 'En: Madera' filtra por el tamano de madera.",
            "Ayuda de busqueda",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private RowFilter<Object, Object> buildSearchFilter(String scope, String query) {
        final String selectedScope = scope == null ? "Todo" : scope;
        final String normalizedQuery = query == null ? "" : query.trim();
        return new RowFilter<Object, Object>() {
            @Override
            public boolean include(Entry<? extends Object, ? extends Object> entry) {
                int modelRow = (Integer) entry.getIdentifier();
                return rowMatchesSearch(modelRow, selectedScope, normalizedQuery);
            }
        };
    }

    private boolean rowMatchesSearch(int modelRow, String scope, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String normalizedScope = scope == null ? "Todo" : scope;
        String q = query.toLowerCase(Locale.ROOT);

        if (!"Todo".equalsIgnoreCase(normalizedScope)) {
            int targetColumn = columnIndexResolver.apply(normalizedScope);
            if (targetColumn < 0) {
                LOGGER.log(Level.FINE, "Filtro con ambito desconocido: {0}", normalizedScope);
                return false;
            }
            Object valueObj = tableModel.getValueAt(modelRow, targetColumn);
            String valueText = valueObj == null ? "" : valueObj.toString().toLowerCase(Locale.ROOT);
            return valueText.contains(q);
        }

        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            Object valueObj = tableModel.getValueAt(modelRow, col);
            String valueText = valueObj == null ? "" : valueObj.toString().toLowerCase(Locale.ROOT);
            if (valueText.contains(q)) {
                return true;
            }
        }
        return false;
    }
}
