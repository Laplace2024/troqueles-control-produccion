package com.trabajo.troqueles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.CellEditor;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

public class SpreadsheetFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    /**
     * Esquema del formulario en pantalla (estilo papel de troqueles).
     * Orden e indices fijos: el modelo se construye con estos nombres y `getColumnClass` se basa en ellos.
     */
    private static final String COL_CLIENTE = "Cod. cliente";
    /** Nombre del cliente; va dentro del grupo CLIENTE, a la derecha del codigo. */
    private static final String COL_NOMBRE = "Nombre";
    private static final String COL_NUM = "Nº";
    private static final String COL_X = "X";
    private static final String COL_Y = "Y";
    private static final String COL_MADERA = "Madera";
    /** Tamaño de corte (2P / 3P / vacio); una sola columna con desplegable. */
    private static final String COL_CORTE_TAM = "C.tamaño";
    private static final String COL_CORTE_NOTA = "Corte";
    private static final String COL_HENDIDO_TAM = "H.tamaño";
    private static final String COL_HENDIDO_NOTA = "Hendido";
    private static final String COL_GOMA = "Goma";
    private static final String COL_GTAM = "G.tamaño";
    private static final String COL_HECHO = "Hecho";

    private static final String[] GOMA_OPCIONES = {
        "Amarillo", "Negro", "Blanco", "Rosa",
        "Blanco + Negro", "Roja", "Roja + Negro", "Roja + Amarillo", "Plancha negra"
    };
    private static final String[] GTAM_OPCIONES = { "7", "10" };
    private static final String[] MADERA_OPCIONES = { "18", "15", "12", "10" };
    /** Opciones del desplegable unico de tamaño (corte y hendido). */
    private static final String[] P2P3P_OPCIONES = { "2P", "3P" };

    private static final String[] FORM_COLUMNS = {
        COL_CLIENTE, COL_NOMBRE,
        COL_NUM, COL_X, COL_Y,
        COL_MADERA,
        COL_CORTE_TAM, COL_CORTE_NOTA,
        COL_HENDIDO_TAM, COL_HENDIDO_NOTA,
        COL_GOMA, COL_GTAM, COL_HECHO
    };

    private static final int W_CLIENTE = 110;
    private static final int W_NOMBRE = 160;
    private static final int W_NUM = 100;
    private static final int W_XY = 70;
    private static final int W_TAM_P2P3P = 100;
    private static final int W_MADERA = 90;
    private static final int W_NOTE = 130;
    private static final int W_GOMA = 200;
    private static final int W_GTAM = 80;
    private static final int W_HECHO = 70;

    private static final int[] FORM_COL_WIDTHS = {
        W_CLIENTE, W_NOMBRE,
        W_NUM,
        W_XY, W_XY,
        W_MADERA,
        W_TAM_P2P3P, W_NOTE,
        W_TAM_P2P3P, W_NOTE,
        W_GOMA, W_GTAM, W_HECHO
    };

    /** Servicio de lookup/autocompletado de cliente (carga opcional desde CSV local). */
    private static final ClientLookup CLIENT_LOOKUP = ClientLookup.fromDefaultFiles();

    private final DefaultTableModel tableModel;
    private final transient List<JTable> dataTables = new ArrayList<JTable>();
    private final transient List<TableRowSorter<DefaultTableModel>> dataSorters = new ArrayList<TableRowSorter<DefaultTableModel>>();
    private JTable activeDataTable;
    private final transient SpreadsheetHistory history;
    private final transient ChangeLog changeLog = new ChangeLog(resolveChangeLogFile(), 1000);
    private final transient SearchAndFilter searchAndFilter;
    private final transient ToolbarActionRegistry toolbarActions;
    private final transient DbWorkbookRepository dbWorkbookRepository;
    private transient DashboardServer dashboardServer;

    private JComboBox<String> searchScopeCombo;
    private JTextField searchField;
    private JTextField columnField;
    private JTextField cellEditorField;
    private JLabel resultLabel;
    private JLabel clientHintLabel;
    private JLabel filterLabel;
    private JLabel totalsLabel;
    private JLabel validationLabel;
    private JLabel kpiRowsLabel;
    private JLabel kpiSumLabel;
    private JLabel kpiAvgLabel;
    private JPanel topPanel;
    private JPanel bottomPanel;

    private boolean historyOperationInProgress;
    /** Detalle del proximo DELETE (p. ej. Nº troquel) capturado antes de removeRow. */
    private String pendingRowDeleteLogDetail;
    private int extraColumnCounter = 1;
    private int pendingSuggestionModelRow = -1;
    private int pendingSuggestionModelCol = -1;
    private String pendingSuggestionCode;
    private String pendingSuggestionName;
    private final transient Map<String, Integer> columnIndexCache = new HashMap<String, Integer>();
    private final transient Map<String, Long> dbSheetVersionByName = new HashMap<String, Long>();
    private final transient DbAutoSyncService dbAutoSync;
    private transient Map<Integer, String> remoteRowLocksByModelRow = new HashMap<Integer, String>();
    private transient JLabel dbSyncStatusLabel;
    private transient JCheckBoxMenuItem autoSyncMenuItem;

    private Color colorEvenRow = Color.WHITE;
    private Color colorOddRow = new Color(245, 247, 250);
    private Color customBaseBackground = Color.WHITE;
    private Color customTextColor = Color.BLACK;

    private final transient Map<Integer, String[]> dropdownOptionsByColumnIndex = new HashMap<Integer, String[]>();

    private static final String IMAGE_COLUMN_NAME = ImageColumnSupport.COLUMN_NAME;
    private final transient ImageColumnSupport imageColumnSupport;
    private static final String WINDOW_TITLE_PREFIX = "Trabajo Troqueles - ";
    private static final String DEFAULT_SHEET_TITLE = "Hoja de Calculo";
    private static final String PREF_SHEET_TITLE_KEY = "sheetTitle";
    private static final String PREF_EXPORT_NAME_TEMPLATE_KEY = "exportNameTemplate";
    private static final Color TOOLBAR_BACKGROUND = new Color(242, 246, 252);
    private static final Color BUTTON_BACKGROUND = new Color(232, 238, 248);
    private static final Color BUTTON_TEXT = new Color(28, 41, 66);
    private static final DateTimeFormatter TITLE_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final String WORKER_NAME = resolveWorkerName();

    private String exportNameTemplate = "titulo_tipo";

    @SuppressWarnings("this-escape")
    public SpreadsheetFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setJMenuBar(buildAppMenuBar());

        tableModel = new DefaultTableModel(FORM_COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex < 0 || columnIndex >= getColumnCount()) {
                    return String.class;
                }
                String name = getColumnName(columnIndex);
                if (COL_HECHO.equals(name)) {
                    return Boolean.class;
                }
                return String.class;
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
                    if (column >= 0 && column < getColumnCount()) {
                        return getColumnClass(column) == Boolean.class ? Boolean.FALSE : "";
                    }
                    return "";
                }
                Object value = super.getValueAt(row, column);
                if (getColumnClass(column) == Boolean.class) {
                    if (value == null) {
                        return Boolean.FALSE;
                    }
                    if (value instanceof Boolean) {
                        return value;
                    }
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue() != 0.0;
                    }
                    String s = String.valueOf(value).trim();
                    if (s.isEmpty()) {
                        return Boolean.FALSE;
                    }
                    String normalized = s.toLowerCase(Locale.ROOT).replace("á", "a").replace("é", "e")
                        .replace("í", "i").replace("ó", "o").replace("ú", "u");
                    if ("true".equals(normalized) || "1".equals(normalized)
                        || "s".equals(normalized) || "si".equals(normalized) || "yes".equals(normalized)
                        || "terminado".equals(normalized)) {
                        return Boolean.TRUE;
                    }
                    if ("no terminado".equals(normalized)) {
                        return Boolean.FALSE;
                    }
                    return Boolean.FALSE;
                }
                if (value == null) {
                    return "";
                }
                return value;
            }
        };
        addInitialRows();

        JComponent center = buildDashboardTablesPanel();
        searchAndFilter = new SearchAndFilter(
            tableModel,
            dataSorters,
            dataTables,
            this::getColumnIndexByName,
            this::refreshTotalsLabel,
            this
        );
        toolbarActions = new ToolbarActionRegistry();
        imageColumnSupport = new ImageColumnSupport(
            tableModel,
            dataTables,
            this::getColumnIndexByName
        );
        dbWorkbookRepository = new DbWorkbookRepository(DbSettings.loadDefault());
        dbAutoSync = new DbAutoSyncService(dbWorkbookRepository, WORKER_NAME);

        history = new SpreadsheetHistory(50);

        topPanel = buildTopActions();
        bottomPanel = buildBottomActions();
        styleControlPanel(topPanel);
        styleControlPanel(bottomPanel);

        configureTableColumns();

        add(center, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

        installStatusUpdates();
        installKeyboardShortcuts();
        installClickShortcuts();
        installSelectionSync();
        installDashboardShutdownHook();
        startDashboardServer();
        history.push(tableModel);
        applyCombinedFilter();
        refreshTotalsLabel();
        updateValidationSummaryLabel();
        applyTheme();
        updateValidationSummaryLabel();
        applySheetTitle(loadSavedSheetTitle());
        startDbAutoSync();
        changeLog.record("Aplicacion abierta", "Filas iniciales: " + tableModel.getRowCount());
    }

    private JTable getDataTable() {
        if (activeDataTable != null) {
            return activeDataTable;
        }
        return dataTables.isEmpty() ? null : dataTables.get(0);
    }

    private void repaintAllDataTables() {
        for (JTable t : dataTables) {
            t.repaint();
        }
    }

    /**
     * Etiquetas mostradas en la cabecera fina del JTable (no son los nombres internos del modelo).
     */
    private static final String[] FORM_HEADER_LABELS = {
        "Cod.", "Nombre",
        "Nº",
        "X", "Y",
        "Tamaño",
        "Tamaño", "C.nota",
        "Tamaño", "N.hendido",
        "Color",
        "Tam.",
        "Hecho"
    };

    /**
     * Mapea grupos del banner a columnas: CLIENTE=2 (Cod.+Nombre), Nº TROQUEL=1, MEDIDAS=2,
     * MADERAS=1, CORTE=2, HENDIDO=2, GOMA=1, G.TAMAÑO=1, HECHO=1.
     */
    private static final int[] BANNER_GROUP_SPANS = {2, 1, 2, 1, 2, 2, 1, 1, 1};
    private static final int BANNER_ROW_HEIGHT = 36;
    private final transient List<JLabel> bannerLabels = new ArrayList<JLabel>();

    /**
     * Construye solo el banner azul de agrupacion (CLIENTE, MEDIDAS, ...). NO mete la cabecera
     * nativa del {@link JTable} dentro: la cabecera la sigue gestionando el {@link JScrollPane}.
     * El banner se coloca en el padre del scroll para que sea siempre visible.
     */
    private JComponent buildGroupBannerOnly(JTable jtable) {
        Color bg = new Color(220, 232, 248);
        Color border = new Color(150, 168, 195);
        JPanel banner = new JPanel();
        banner.setLayout(new BoxLayout(banner, BoxLayout.X_AXIS));
        banner.setOpaque(true);
        banner.setBackground(bg);

        bannerLabels.clear();
        addBannerCell(banner, "CLIENTE",     FORM_COL_WIDTHS[0] + FORM_COL_WIDTHS[1],                                           bg, border);
        addBannerCell(banner, "Nº TROQUEL", FORM_COL_WIDTHS[2],                                                                 bg, border);
        addBannerCell(banner, "MEDIDAS",     FORM_COL_WIDTHS[3] + FORM_COL_WIDTHS[4],                                           bg, border);
        addBannerCell(banner, "MADERAS",     FORM_COL_WIDTHS[5],                                                                bg, border);
        addBannerCell(banner, "CORTE",       FORM_COL_WIDTHS[6] + FORM_COL_WIDTHS[7],                                           bg, border);
        addBannerCell(banner, "HENDIDO",     FORM_COL_WIDTHS[8] + FORM_COL_WIDTHS[9],                                           bg, border);
        addBannerCell(banner, "GOMA",        FORM_COL_WIDTHS[10],                                                               bg, border);
        addBannerCell(banner, "G.TAMAÑO",    FORM_COL_WIDTHS[11],                                                               bg, border);
        addBannerCell(banner, "HECHO",       FORM_COL_WIDTHS[12],                                                               bg, border);

        int bannerWidth = 0;
        for (int w : FORM_COL_WIDTHS) {
            bannerWidth += w;
        }
        Dimension bannerSize = new Dimension(bannerWidth, BANNER_ROW_HEIGHT);
        banner.setPreferredSize(bannerSize);
        banner.setMinimumSize(new Dimension(0, BANNER_ROW_HEIGHT));
        return banner;
    }

    private void addBannerCell(JPanel banner, String text, int width, Color bg, Color borderColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setOpaque(true);
        label.setBackground(bg);
        label.setForeground(new Color(28, 41, 66));
        label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, borderColor));
        Dimension dim = new Dimension(width, BANNER_ROW_HEIGHT);
        label.setPreferredSize(dim);
        label.setMinimumSize(dim);
        label.setMaximumSize(dim);
        banner.add(label);
        bannerLabels.add(label);
    }

    /**
     * Recalcula los anchos del banner sumando los anchos reales por grupo de columnas del JTable.
     * Permite que el banner se mantenga alineado cuando alguna columna del JTable cambia de ancho
     * (por ejemplo cuando las columnas elasticas se expanden al maximizar la ventana).
     */
    private void refreshBannerWidths(JTable jt) {
        if (bannerLabels.isEmpty() || jt == null) return;
        int colIdx = 0;
        int columnCount = jt.getColumnModel().getColumnCount();
        for (int g = 0; g < BANNER_GROUP_SPANS.length && g < bannerLabels.size(); g++) {
            int span = BANNER_GROUP_SPANS[g];
            int width = 0;
            for (int s = 0; s < span && colIdx + s < columnCount; s++) {
                width += jt.getColumnModel().getColumn(colIdx + s).getWidth();
            }
            JLabel label = bannerLabels.get(g);
            Dimension d = new Dimension(width, BANNER_ROW_HEIGHT);
            label.setPreferredSize(d);
            label.setMinimumSize(d);
            label.setMaximumSize(d);
            colIdx += span;
        }
        Container parent = bannerLabels.get(0).getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private boolean bannerRefreshScheduled = false;

    /** Encola una sola actualizacion del banner por ciclo del EDT, evitando cascadas de revalidados. */
    private void scheduleBannerRefresh(JTable jt) {
        if (bannerRefreshScheduled || jt == null) return;
        bannerRefreshScheduled = true;
        SwingUtilities.invokeLater(() -> {
            bannerRefreshScheduled = false;
            refreshBannerWidths(jt);
        });
    }

    private boolean redistributeScheduled = false;

    /** Encola una sola redistribucion por ciclo del EDT. */
    private void scheduleRedistribute(JTable jt, javax.swing.JViewport viewport) {
        if (redistributeScheduled || jt == null || viewport == null) return;
        redistributeScheduled = true;
        SwingUtilities.invokeLater(() -> {
            redistributeScheduled = false;
            redistributeFormColumnWidths(jt, viewport.getWidth());
        });
    }

    private boolean redistributingWidths = false;

    private void applyFormColumnWidths(JTable jt) {
        if (jt == null) return;
        redistributingWidths = true;
        try {
            int columnCount = jt.getColumnModel().getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                int width = i < FORM_COL_WIDTHS.length ? FORM_COL_WIDTHS[i] : 100;
                TableColumn col = jt.getColumnModel().getColumn(i);
                col.setMinWidth(width);
                col.setMaxWidth(width);
                col.setPreferredWidth(width);
                if (i < FORM_HEADER_LABELS.length) {
                    col.setHeaderValue(FORM_HEADER_LABELS[i]);
                }
            }
        } finally {
            redistributingWidths = false;
        }
        if (jt.getTableHeader() != null) {
            jt.getTableHeader().repaint();
        }
    }

    private void setFixedWidth(JTable jt, int viewIdx, int width) {
        if (viewIdx < 0 || viewIdx >= jt.getColumnModel().getColumnCount()) return;
        TableColumn col = jt.getColumnModel().getColumn(viewIdx);
        col.setMinWidth(width);
        col.setMaxWidth(width);
        col.setPreferredWidth(width);
    }

    /**
     * Reparto proporcional del ancho disponible en el viewport.
     * Las columnas fijas mantienen su tamaño base; el extra se reparte entre las elasticas:
     * `Corte/Nota` (1), `Hendido/Nota` (1) y `Goma` (2).
     * Se protege con `redistributingWidths` para evitar bucles con los listeners del column model.
     */
    private void redistributeFormColumnWidths(JTable jt, int viewportWidth) {
        if (jt == null || redistributingWidths) return;
        int corteIdx = columnIndexOf(COL_CORTE_NOTA);
        int hendidoIdx = columnIndexOf(COL_HENDIDO_NOTA);
        int gomaIdx = columnIndexOf(COL_GOMA);
        if (corteIdx < 0 || hendidoIdx < 0 || gomaIdx < 0) return;

        int viewCorte = jt.convertColumnIndexToView(corteIdx);
        int viewHendido = jt.convertColumnIndexToView(hendidoIdx);
        int viewGoma = jt.convertColumnIndexToView(gomaIdx);
        if (viewCorte < 0 || viewHendido < 0 || viewGoma < 0) return;

        redistributingWidths = true;
        try {
            int sumFixed = 0;
            int columnCount = jt.getColumnModel().getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                if (i == viewCorte || i == viewHendido || i == viewGoma) continue;
                sumFixed += i < FORM_COL_WIDTHS.length ? FORM_COL_WIDTHS[i] : 100;
            }
            int baseElastic = W_NOTE + W_NOTE + W_GOMA;
            int available = Math.max(viewportWidth, sumFixed + baseElastic);
            int elasticTotal = available - sumFixed;
            int corteW = Math.round(elasticTotal * (W_NOTE / (float) baseElastic));
            int hendidoW = Math.round(elasticTotal * (W_NOTE / (float) baseElastic));
            int gomaW = elasticTotal - corteW - hendidoW;

            setFixedWidth(jt, viewCorte, corteW);
            setFixedWidth(jt, viewHendido, hendidoW);
            setFixedWidth(jt, viewGoma, gomaW);
        } finally {
            redistributingWidths = false;
        }
        scheduleBannerRefresh(jt);
    }

    private JComponent buildDashboardTablesPanel() {
        JTable jt = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                if (!super.isCellEditable(row, column)) {
                    return false;
                }
                int modelRow = convertRowIndexToModel(row);
                return !isRowLockedByOtherWorker(modelRow);
            }
        };
        jt.setRowHeight(28);
        jt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        jt.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jt.setShowGrid(true);
        jt.setIntercellSpacing(new Dimension(1, 1));
        jt.setGridColor(new Color(150, 168, 195));
        jt.setFillsViewportHeight(true);
        jt.setDefaultRenderer(Object.class, new FormCellRenderer());
        jt.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        jt.getTableHeader().setReorderingAllowed(false);
        jt.getTableHeader().setResizingAllowed(false);
        jt.getTableHeader().setBackground(new Color(238, 245, 254));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        jt.setRowSorter(sorter);
        dataTables.clear();
        dataSorters.clear();
        dataTables.add(jt);
        dataSorters.add(sorter);
        activeDataTable = jt;
        installDbSyncCellEditorListener(jt);

        jt.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activeDataTable = jt;
            }
        });

        applyFormColumnWidths(jt);

        JScrollPane scroll = new JScrollPane(jt,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (redistributingWidths) return;
                scheduleRedistribute(jt, scroll.getViewport());
            }
        });
        jt.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
                scheduleBannerRefresh(jt);
                scheduleRedistribute(jt, scroll.getViewport());
            }
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
                scheduleBannerRefresh(jt);
                scheduleRedistribute(jt, scroll.getViewport());
            }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
                scheduleBannerRefresh(jt);
            }
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                /* no-op para evitar bucles al ajustar widths programaticamente */
            }
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
                /* no-op */
            }
        });

        // Banner de agrupacion: panel azul con etiquetas (CLIENTE, MEDIDAS, ...) que vive
        // ENCIMA del JScrollPane. La cabecera nativa del JTable se queda dentro del scroll.
        JComponent banner = buildGroupBannerOnly(jt);

        JPanel container = new JPanel(new BorderLayout());
        container.add(banner, BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    /** Fondo suave para filas bloqueadas por otro trabajador en BD. */
    private static final Color ROW_LOCKED_BG = new Color(225, 236, 252);
    private static final Color ROW_LOCKED_BG_ALT = new Color(210, 226, 246);
    /** Color de confirmacion aplicado a toda la fila cuando la columna `Hecho` esta marcada. */
    private static final Color ROW_DONE_BG = new Color(255, 205, 205);
    /** Variante mas oscura para filas alternas, manteniendo el aviso visible. */
    private static final Color ROW_DONE_BG_ALT = new Color(252, 188, 188);
    /** Color de texto sobre la fila confirmada. */
    private static final Color ROW_DONE_FG = new Color(120, 18, 18);
    /** Aviso suave para dato incompleto (cliente/codigo faltante). */
    private static final Color ROW_WARN_BG = new Color(255, 245, 210);
    /** Aviso fuerte para dato invalido (X/Y no numerico). */
    private static final Color ROW_INVALID_BG = new Color(255, 222, 222);

    /** Devuelve true si la fila (en indice de vista) tiene `Hecho = true` en el modelo. */
    private boolean isRowDone(JTable table, int viewRow) {
        if (viewRow < 0) return false;
        int modelRow;
        try {
            modelRow = table.convertRowIndexToModel(viewRow);
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return false;
        int hechoIdx = columnIndexOf(COL_HECHO);
        if (hechoIdx < 0) return false;
        return Boolean.TRUE.equals(tableModel.getValueAt(modelRow, hechoIdx));
    }

    private boolean isInvalidXyCell(JTable table, int viewRow, int viewCol) {
        int modelRow;
        int modelCol;
        try {
            modelRow = table.convertRowIndexToModel(viewRow);
            modelCol = table.convertColumnIndexToModel(viewCol);
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return false;
        }
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        if (modelCol != colX && modelCol != colY) {
            return false;
        }
        Object value = tableModel.getValueAt(modelRow, modelCol);
        String text = value == null ? "" : String.valueOf(value).trim();
        return !text.isEmpty() && SpreadsheetStats.tryParseDouble(text) == null;
    }

    private boolean isMissingClientCell(JTable table, int viewRow, int viewCol) {
        int modelRow;
        int modelCol;
        try {
            modelRow = table.convertRowIndexToModel(viewRow);
            modelCol = table.convertColumnIndexToModel(viewCol);
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return false;
        }
        int codIdx = columnIndexOf(COL_CLIENTE);
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        if (codIdx < 0 || nombreIdx < 0) {
            return false;
        }
        if (modelCol != codIdx && modelCol != nombreIdx) {
            return false;
        }
        String code = String.valueOf(tableModel.getValueAt(modelRow, codIdx)).trim();
        String name = String.valueOf(tableModel.getValueAt(modelRow, nombreIdx)).trim();
        return code.isEmpty() || name.isEmpty();
    }

    /**
     * Renderer del formulario: alinea por tipo de columna, aplica tachado al `Nº` y pinta la fila
     * completa en rojo cuando la columna `Hecho` esta marcada (confirmacion visual de trabajo terminado).
     * Las columnas booleanas usan {@link FormBooleanRenderer} para que el checkbox tambien adopte el rojo.
     */
    private class FormCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int modelCol = table.convertColumnIndexToModel(column);
            String name = tableModel.getColumnName(modelCol);
            boolean done = isRowDone(table, row);

            if (IMAGE_COLUMN_NAME.equalsIgnoreCase(name)) {
                String path = value == null ? "" : String.valueOf(value).trim();
                ImageIcon icon = loadImageThumb(path);
                if (icon != null) {
                    setIcon(icon);
                    setText("");
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setIcon(null);
                    setText(path.isEmpty() ? "" : "(no encontrada)");
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.ITALIC, 11));
                }
                applyRowBackground(this, table, row, column, isSelected, done);
                applyRowLockTooltip(this, table, row);
                if (isSelected) {
                    setForeground(Color.WHITE);
                } else {
                    setForeground(new Color(95, 102, 117));
                }
                return component;
            } else {
                setIcon(null);
            }

            if (COL_X.equals(name) || COL_Y.equals(name) || COL_NUM.equals(name)
                || COL_CLIENTE.equals(name) || COL_MADERA.equals(name)
                || COL_GOMA.equals(name) || COL_GTAM.equals(name)
                || COL_CORTE_TAM.equals(name) || COL_HENDIDO_TAM.equals(name)) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
            if (COL_CLIENTE.equals(name)) {
                baseFont = baseFont.deriveFont(Font.BOLD);
            } else if (COL_NUM.equals(name)) {
                baseFont = baseFont.deriveFont(Font.BOLD);
                if (done) {
                    java.util.Map<java.awt.font.TextAttribute, Object> attrs = new java.util.HashMap<java.awt.font.TextAttribute, Object>();
                    attrs.put(java.awt.font.TextAttribute.STRIKETHROUGH, java.awt.font.TextAttribute.STRIKETHROUGH_ON);
                    baseFont = baseFont.deriveFont(attrs);
                }
            }
            setFont(baseFont);

            if (isSelected) {
                setForeground(Color.WHITE);
            } else if (done) {
                setForeground(ROW_DONE_FG);
            } else if (COL_CLIENTE.equals(name)) {
                setForeground(new Color(64, 86, 132));
            } else {
                setForeground(new Color(35, 45, 65));
            }

            applyRowBackground(this, table, row, column, isSelected, done);
            applyRowLockTooltip(this, table, row);
            return component;
        }
    }

    private void applyRowBackground(JLabel target, JTable table, int row, int column, boolean isSelected, boolean done) {
        if (isSelected) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String lockHolder = remoteRowLocksByModelRow.get(Integer.valueOf(modelRow));
        if (lockHolder != null && !lockHolder.trim().isEmpty()) {
            target.setBackground(row % 2 == 0 ? ROW_LOCKED_BG : ROW_LOCKED_BG_ALT);
            return;
        }
        if (done) {
            target.setBackground(row % 2 == 0 ? ROW_DONE_BG : ROW_DONE_BG_ALT);
        } else if (isInvalidXyCell(table, row, column)) {
            target.setBackground(ROW_INVALID_BG);
        } else if (isMissingClientCell(table, row, column)) {
            target.setBackground(ROW_WARN_BG);
        } else {
            target.setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 250, 254));
        }
    }

    /**
     * Carga la imagen del path y la cachea como miniatura escalada (manteniendo aspecto).
     * Devuelve null si el path es vacio o el archivo no existe / no se puede leer.
     */
    private ImageIcon loadImageThumb(String path) {
        return imageColumnSupport.loadImageThumb(path);
    }

    /**
     * Renderer para columnas booleanas que respeta el aviso de fila confirmada: cuando `Hecho=true`
     * el checkbox se pinta sobre fondo rojo igual que el resto de la fila.
     */
    private class FormBooleanRenderer extends javax.swing.JCheckBox implements javax.swing.table.TableCellRenderer {
        private static final long serialVersionUID = 1L;

        FormBooleanRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            boolean done = isRowDone(table, row);
            int modelRow = table.convertRowIndexToModel(row);
            String lockHolder = remoteRowLocksByModelRow.get(Integer.valueOf(modelRow));
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else if (lockHolder != null && !lockHolder.trim().isEmpty()) {
                setBackground(row % 2 == 0 ? ROW_LOCKED_BG : ROW_LOCKED_BG_ALT);
                setForeground(new Color(35, 45, 65));
            } else if (done) {
                setBackground(row % 2 == 0 ? ROW_DONE_BG : ROW_DONE_BG_ALT);
                setForeground(ROW_DONE_FG);
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 250, 254));
                setForeground(new Color(35, 45, 65));
            }
            return this;
        }
    }

    private int columnIndexOf(String name) {
        if (name == null) {
            return -1;
        }
        Integer cached = columnIndexCache.get(name);
        if (cached != null && cached >= 0 && cached < tableModel.getColumnCount()
            && name.equals(tableModel.getColumnName(cached))) {
            return cached;
        }
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (name.equals(tableModel.getColumnName(i))) {
                columnIndexCache.put(name, i);
                return i;
            }
        }
        columnIndexCache.put(name, -1);
        return -1;
    }

    private void invalidateColumnIndexCache() {
        columnIndexCache.clear();
    }

    private JMenuBar buildAppMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu archivoMenu = new JMenu("Archivo");
        JMenuItem guardar = new JMenuItem("Guardar tabla CSV");
        guardar.addActionListener(event -> saveAllRowsToCsv());
        JMenuItem cargar = new JMenuItem("Cargar tabla CSV");
        cargar.addActionListener(event -> loadRowsFromCsv());
        JMenuItem guardarBd = new JMenuItem("Guardar tabla BD");
        guardarBd.addActionListener(event -> saveAllRowsToDatabase());
        JMenuItem cargarBd = new JMenuItem("Cargar tabla BD");
        cargarBd.addActionListener(event -> loadRowsFromDatabase());
        JMenuItem exportarHtml = new JMenuItem("Exportar reporte HTML");
        exportarHtml.addActionListener(event -> exportHtmlReport());
        JMenuItem exportarPdf = new JMenuItem("Exportar reporte PDF");
        exportarPdf.addActionListener(event -> exportPdfReport());
        JMenuItem renombrarHoja = new JMenuItem("Renombrar titulo de hoja...");
        renombrarHoja.addActionListener(event -> promptRenameSheetTitle());
        JMenuItem tituloConFecha = new JMenuItem("Titulo con fecha/hora");
        tituloConFecha.addActionListener(event -> applySheetTitleWithDateTime());
        JMenuItem restaurarTitulo = new JMenuItem("Restaurar titulo por defecto");
        restaurarTitulo.addActionListener(event -> resetSheetTitleToDefault());
        JMenuItem plantillaExport = new JMenuItem("Plantilla nombre exportacion...");
        plantillaExport.addActionListener(event -> configureExportNameTemplate());
        archivoMenu.add(guardar);
        archivoMenu.add(cargar);
        archivoMenu.add(guardarBd);
        archivoMenu.add(cargarBd);
        autoSyncMenuItem = new JCheckBoxMenuItem("Sincronizacion automatica BD", true);
        autoSyncMenuItem.addActionListener(event -> {
            dbAutoSync.setEnabled(autoSyncMenuItem.isSelected());
            updateDbSyncStatusLabel(autoSyncMenuItem.isSelected()
                ? "Sync BD activado"
                : "Sync BD desactivado");
        });
        JMenuItem comprobarBdAhora = new JMenuItem("Comprobar BD ahora");
        comprobarBdAhora.addActionListener(event -> dbAutoSync.pollNow());
        archivoMenu.addSeparator();
        archivoMenu.add(autoSyncMenuItem);
        archivoMenu.add(comprobarBdAhora);
        archivoMenu.addSeparator();
        archivoMenu.add(exportarHtml);
        archivoMenu.add(exportarPdf);
        archivoMenu.addSeparator();
        archivoMenu.add(renombrarHoja);
        archivoMenu.add(tituloConFecha);
        archivoMenu.add(restaurarTitulo);
        archivoMenu.addSeparator();
        archivoMenu.add(plantillaExport);

        JMenu insertarMenu = new JMenu("Insertar");
        JMenuItem filaArriba = new JMenuItem("Fila arriba");
        filaArriba.addActionListener(event -> insertRowRelativeToSelection(true));
        JMenuItem filaAbajo = new JMenuItem("Fila abajo");
        filaAbajo.addActionListener(event -> insertRowRelativeToSelection(false));
        JMenuItem columnaIzquierda = new JMenuItem("Columna izquierda");
        columnaIzquierda.addActionListener(event -> addColumnRelativeToSelection(true));
        JMenuItem columnaDerecha = new JMenuItem("Columna derecha");
        columnaDerecha.addActionListener(event -> addColumnRelativeToSelection(false));
        insertarMenu.add(filaArriba);
        insertarMenu.add(filaAbajo);
        insertarMenu.add(columnaIzquierda);
        insertarMenu.add(columnaDerecha);

        JMenu edicionMenu = new JMenu("Edicion");
        JMenuItem deshacer = new JMenuItem("Deshacer");
        deshacer.addActionListener(event -> undoHistory());
        JMenuItem rehacer = new JMenuItem("Rehacer");
        rehacer.addActionListener(event -> redoHistory());
        JMenuItem borrarCelda = new JMenuItem("Borrar celda");
        borrarCelda.addActionListener(event -> clearSelectedCell());
        JMenuItem duplicarFila = new JMenuItem("Duplicar fila");
        duplicarFila.addActionListener(event -> duplicateSelectedRow());
        JMenuItem historialMenu = new JMenuItem("Ver historial de cambios...");
        historialMenu.addActionListener(event -> showChangeLogDialog());
        edicionMenu.add(deshacer);
        edicionMenu.add(rehacer);
        edicionMenu.addSeparator();
        edicionMenu.add(borrarCelda);
        edicionMenu.add(duplicarFila);
        edicionMenu.addSeparator();
        edicionMenu.add(historialMenu);

        JMenu vistaMenu = new JMenu("Vista");
        JMenuItem coloresPersonalizados = new JMenuItem("Personalizar colores...");
        coloresPersonalizados.addActionListener(event -> chooseCustomColors());
        JMenuItem resetColores = new JMenuItem("Restablecer colores");
        resetColores.addActionListener(event -> resetAllColors());
        JMenuItem autoAjuste = new JMenuItem("Autoajustar todas las columnas");
        autoAjuste.addActionListener(event -> autoFitAllColumns());
        vistaMenu.add(coloresPersonalizados);
        vistaMenu.add(resetColores);
        vistaMenu.add(autoAjuste);

        JMenu datosMenu = new JMenu("Datos");
        JMenuItem exportarVisible = new JMenuItem("Exportar CSV visible");
        exportarVisible.addActionListener(event -> exportVisibleRowsToCsv());
        JMenuItem abrirDashboard = new JMenuItem("Abrir dashboard web");
        abrirDashboard.addActionListener(event -> openDashboardWithCurrentData());
        JMenuItem imagenFila = new JMenuItem("Añadir imagen a fila");
        imagenFila.addActionListener(event -> setImageForSelectedRow());
        JMenuItem configurarDropdown = new JMenuItem("Configurar desplegable");
        configurarDropdown.addActionListener(event -> configureDropdownForSelectedColumn());
        JMenuItem verDesplegables = new JMenuItem("Ver desplegables activos");
        verDesplegables.addActionListener(event -> showActiveDropdowns());
        JMenuItem exportarDesplegables = new JMenuItem("Exportar desplegables CSV");
        exportarDesplegables.addActionListener(event -> exportDropdownsSummaryToCsv());
        JMenuItem estadisticasClientes = new JMenuItem("Estadisticas por cliente (visible)");
        estadisticasClientes.addActionListener(event -> showClientDistributionStats());
        JMenuItem rankingBd = new JMenuItem("Ranking clientes BD");
        rankingBd.addActionListener(event -> showClientRankingFromDatabase());
        datosMenu.add(exportarVisible);
        datosMenu.add(abrirDashboard);
        datosMenu.add(imagenFila);
        datosMenu.add(configurarDropdown);
        datosMenu.add(verDesplegables);
        datosMenu.add(exportarDesplegables);
        datosMenu.addSeparator();
        datosMenu.add(estadisticasClientes);
        datosMenu.add(rankingBd);

        menuBar.add(archivoMenu);
        menuBar.add(insertarMenu);
        menuBar.add(edicionMenu);
        menuBar.add(vistaMenu);
        menuBar.add(datosMenu);
        JMenu ayudaMenu = new JMenu("Ayuda");
        JMenuItem manualUso = new JMenuItem("Manual rapido");
        manualUso.addActionListener(event -> showUserManual());
        JMenuItem guiaFormulas = new JMenuItem("Guia de formulas");
        guiaFormulas.addActionListener(event -> showFormulasGuide());
        ayudaMenu.add(manualUso);
        ayudaMenu.add(guiaFormulas);
        menuBar.add(ayudaMenu);
        return menuBar;
    }

    private JPanel buildTopActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addRowButton = toolbarActions.newButton("add-row", "Agregar fila", () -> {
            stopEditingBeforeTableMutation();
            int modelRow = tableModel.getRowCount();
            tableModel.addRow(buildEmptyRowForCurrentModel());
            selectAndScrollToModelRow(modelRow);
        });

        JButton deleteRowButton = toolbarActions.newButton("delete-row", "Eliminar fila seleccionada", this::deleteSelectedRow);

        JButton resetTableButton = new JButton("Reset tabla");
        resetTableButton.setToolTipText(
            "Vacia el contenido de todas las celdas. Mantiene columnas, filas y desplegables."
        );
        resetTableButton.addActionListener(event -> resetSpreadsheetTable());

        JButton addColumnLeftButton = new JButton("Columna izquierda");
        addColumnLeftButton.addActionListener(event -> addColumnRelativeToSelection(true));

        JButton addColumnRightButton = new JButton("Columna derecha");
        addColumnRightButton.addActionListener(event -> addColumnRelativeToSelection(false));

        searchField = new JTextField(12);
        searchScopeCombo = new JComboBox<String>(new String[]{"Todo", "Cod. cliente", "Nombre", "Madera"});
        searchAndFilter.bindControls(searchField, searchScopeCombo, filterLabel);
        searchAndFilter.installDefaultListeners();

        JButton searchHelpButton = new JButton("Ayuda busqueda");
        searchHelpButton.addActionListener(event -> searchAndFilter.showSearchHelp());

        JButton clearFilterButton = new JButton("Limpiar filtros");
        clearFilterButton.addActionListener(event -> searchAndFilter.clearFilters());

        JButton undoButton = toolbarActions.newButton("undo", "Deshacer", this::undoHistory);

        JButton redoButton = toolbarActions.newButton("redo", "Rehacer", this::redoHistory);

        JButton historyButton = new JButton("Historial");
        historyButton.setToolTipText("Ver el historial de cambios realizados (con fecha y hora).");
        historyButton.addActionListener(event -> showChangeLogDialog());

        JButton customColorsButton = new JButton("Colores personalizados");
        customColorsButton.addActionListener(event -> chooseCustomColors());
        JButton renameSheetButton = toolbarActions.newButton("rename-sheet", "Renombrar hoja", this::promptRenameSheetTitle);
        JButton sheetDateTimeButton = new JButton("Titulo + fecha");
        sheetDateTimeButton.addActionListener(event -> applySheetTitleWithDateTime());
        JButton resetSheetTitleButton = new JButton("Titulo por defecto");
        resetSheetTitleButton.addActionListener(event -> resetSheetTitleToDefault());
        JButton exportTemplateButton = new JButton("Plantilla export");
        exportTemplateButton.addActionListener(event -> configureExportNameTemplate());

        JButton imageButton = new JButton("Añadir imagen a fila");
        imageButton.addActionListener(event -> setImageForSelectedRow());

        JButton dropdownConfigButton = new JButton("Configurar desplegable");
        dropdownConfigButton.addActionListener(event -> configureDropdownForSelectedColumn());

        JButton dropdownClearButton = new JButton("Quitar desplegable");
        dropdownClearButton.addActionListener(event -> clearDropdownForSelectedColumn());
        JButton dropdownStatusButton = new JButton("Ver desplegables");
        dropdownStatusButton.addActionListener(event -> showActiveDropdowns());
        JButton dropdownExportButton = new JButton("Exportar desplegables");
        dropdownExportButton.addActionListener(event -> exportDropdownsSummaryToCsv());

        JButton exportVisibleButton = toolbarActions.newButton("save-visible", "Exportar CSV", this::exportVisibleRowsToCsv);

        JButton openDashboardButton = toolbarActions.newButton("open-dashboard", "Abrir dashboard", this::openDashboardWithCurrentData);

        JButton saveAllButton = toolbarActions.newButton("save-all", "Guardar tabla", this::saveAllRowsToCsv);

        JButton loadButton = toolbarActions.newButton("load-csv", "Cargar tabla", this::loadRowsFromCsv);
        JButton saveDbButton = new JButton("Guardar BD");
        saveDbButton.addActionListener(event -> saveAllRowsToDatabase());
        JButton loadDbButton = new JButton("Cargar BD");
        loadDbButton.addActionListener(event -> loadRowsFromDatabase());

        JButton htmlReportButton = toolbarActions.newButton("export-html", "Exportar reporte HTML", this::exportHtmlReport);
        JButton pdfReportButton = new JButton("Exportar reporte PDF");
        pdfReportButton.addActionListener(event -> exportPdfReport());
        JButton clientStatsButton = new JButton("Stats clientes");
        clientStatsButton.addActionListener(event -> showClientDistributionStats());
        JButton dbRankingButton = new JButton("Ranking BD");
        dbRankingButton.addActionListener(event -> showClientRankingFromDatabase());

        JComboBox<String> quickInsertMenu = new JComboBox<String>(new String[]{
            "Insertar rapido...",
            "Fila arriba",
            "Fila abajo",
            "Columna izquierda",
            "Columna derecha"
        });
        quickInsertMenu.addActionListener(event -> handleQuickInsertSelection(quickInsertMenu));

        JComboBox<String> quickDataMenu = new JComboBox<String>(new String[]{
            "Datos rapido...",
            "Exportar CSV visible",
            "Guardar tabla BD",
            "Cargar tabla BD",
            "Exportar reporte PDF",
            "Estadisticas por cliente (visible)",
            "Ranking clientes BD",
            "Abrir dashboard web",
            "Añadir imagen a fila",
            "Configurar desplegable",
            "Ver desplegables activos",
            "Exportar desplegables CSV"
        });
        quickDataMenu.addActionListener(event -> handleQuickDataSelection(quickDataMenu));

        // Acciones sin boton directo en toolbar pero compartidas por atajos.
        toolbarActions.register("focus-search", "Focus search", () -> {
            if (searchField != null) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });
        toolbarActions.register("duplicate-row", "Duplicate row", this::duplicateSelectedRow);
        toolbarActions.register("user-manual", "User manual", this::showUserManual);

        panel.add(addRowButton);
        panel.add(deleteRowButton);
        panel.add(resetTableButton);
        addToolbarSeparator(panel);
        panel.add(addColumnLeftButton);
        panel.add(addColumnRightButton);
        addToolbarSeparator(panel);
        panel.add(new JLabel("Buscar:"));
        panel.add(searchField);
        panel.add(new JLabel("En:"));
        panel.add(searchScopeCombo);
        panel.add(searchHelpButton);
        panel.add(clearFilterButton);
        addToolbarSeparator(panel);
        panel.add(undoButton);
        panel.add(redoButton);
        panel.add(historyButton);
        addToolbarSeparator(panel);
        panel.add(customColorsButton);
        addToolbarSeparator(panel);
        panel.add(renameSheetButton);
        panel.add(sheetDateTimeButton);
        panel.add(resetSheetTitleButton);
        panel.add(exportTemplateButton);
        addToolbarSeparator(panel);
        panel.add(imageButton);
        addToolbarSeparator(panel);
        panel.add(dropdownConfigButton);
        panel.add(dropdownClearButton);
        panel.add(dropdownStatusButton);
        panel.add(dropdownExportButton);
        addToolbarSeparator(panel);
        panel.add(exportVisibleButton);
        panel.add(openDashboardButton);
        panel.add(saveAllButton);
        panel.add(loadButton);
        panel.add(saveDbButton);
        panel.add(loadDbButton);
        panel.add(htmlReportButton);
        panel.add(pdfReportButton);
        panel.add(clientStatsButton);
        panel.add(dbRankingButton);
        addToolbarSeparator(panel);
        panel.add(quickInsertMenu);
        panel.add(quickDataMenu);
        return panel;
    }

    /**
     * Muestra el historial de cambios en un dialogo modal con texto desplazable.
     * Permite limpiar el historial (con confirmacion) y refrescar la vista.
     */
    private void showChangeLogDialog() {
        javax.swing.JTextArea area = new javax.swing.JTextArea(20, 70);
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setText(buildChangeLogText());
        area.setCaretPosition(area.getDocument().getLength());

        JScrollPane scroll = new JScrollPane(area);

        Object[] options = {"Limpiar historial", "Refrescar", "Cerrar"};
        int choice = JOptionPane.showOptionDialog(
            this,
            scroll,
            "Historial de cambios",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[2]
        );
        if (choice == 0) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Se vaciara todo el historial de cambios (incluido el archivo cambios.log).\n"
                    + "Esta accion no se puede deshacer.\n"
                    + "\u00bfContinuar?",
                "Limpiar historial",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.OK_OPTION) {
                changeLog.clear();
                changeLog.record("Historial limpiado", null);
                JOptionPane.showMessageDialog(this, "Historial vaciado.");
            }
        } else if (choice == 1) {
            showChangeLogDialog();
        }
    }

    private String buildChangeLogText() {
        java.util.List<ChangeLog.Entry> entries = changeLog.getEntries();
        if (entries.isEmpty()) {
            return "(sin cambios registrados)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(entries.size()).append(" cambios\n\n");
        for (ChangeLog.Entry entry : entries) {
            sb.append(ChangeLog.formatTimestamp(entry.getTimestamp()))
              .append("  ")
              .append(entry.getAccion());
            if (entry.getDetalle() != null && !entry.getDetalle().isEmpty()) {
                sb.append(": ").append(entry.getDetalle());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Linea vertical fina como separador visual entre grupos de controles en una toolbar horizontal. */
    private void addToolbarSeparator(JPanel panel) {
        javax.swing.JSeparator sep = new javax.swing.JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 24));
        sep.setForeground(new Color(180, 192, 212));
        panel.add(sep);
    }

    private void handleQuickInsertSelection(JComboBox<String> combo) {
        String action = (String) combo.getSelectedItem();
        if (action == null || "Insertar rapido...".equals(action)) {
            return;
        }
        if ("Fila arriba".equals(action)) {
            insertRowRelativeToSelection(true);
        } else if ("Fila abajo".equals(action)) {
            insertRowRelativeToSelection(false);
        } else if ("Columna izquierda".equals(action)) {
            addColumnRelativeToSelection(true);
        } else if ("Columna derecha".equals(action)) {
            addColumnRelativeToSelection(false);
        }
        combo.setSelectedIndex(0);
    }

    private void handleQuickDataSelection(JComboBox<String> combo) {
        String action = (String) combo.getSelectedItem();
        if (action == null || "Datos rapido...".equals(action)) {
            return;
        }
        if ("Exportar CSV visible".equals(action)) {
            exportVisibleRowsToCsv();
        } else if ("Guardar tabla BD".equals(action)) {
            saveAllRowsToDatabase();
        } else if ("Cargar tabla BD".equals(action)) {
            loadRowsFromDatabase();
        } else if ("Exportar reporte PDF".equals(action)) {
            exportPdfReport();
        } else if ("Estadisticas por cliente (visible)".equals(action)) {
            showClientDistributionStats();
        } else if ("Ranking clientes BD".equals(action)) {
            showClientRankingFromDatabase();
        } else if ("Abrir dashboard web".equals(action)) {
            openDashboardWithCurrentData();
        } else if ("Añadir imagen a fila".equals(action)) {
            setImageForSelectedRow();
        } else if ("Configurar desplegable".equals(action)) {
            configureDropdownForSelectedColumn();
        } else if ("Ver desplegables activos".equals(action)) {
            showActiveDropdowns();
        } else if ("Exportar desplegables CSV".equals(action)) {
            exportDropdownsSummaryToCsv();
        }
        combo.setSelectedIndex(0);
    }

    private void showClientDistributionStats() {
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        int clienteIdx = columnIndexOf(COL_CLIENTE);
        int pedidoIdx = columnIndexOf(COL_NUM);
        if (nombreIdx < 0 && clienteIdx < 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron las columnas de cliente.");
            return;
        }
        JTable table = getDataTable();
        if (table == null) {
            JOptionPane.showMessageDialog(this, "No hay tabla activa.");
            return;
        }
        Map<String, ClientStats> statsByClient = new HashMap<String, ClientStats>();
        int totalVisible = 0;
        int totalCantidad = 0;
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
                continue;
            }
            String name = nombreIdx < 0 ? "" : String.valueOf(tableModel.getValueAt(modelRow, nombreIdx)).trim();
            String code = clienteIdx < 0 ? "" : String.valueOf(tableModel.getValueAt(modelRow, clienteIdx)).trim();
            String key = !name.isEmpty() ? name : (!code.isEmpty() ? "COD " + code : "(Sin cliente)");
            ClientStats current = statsByClient.get(key);
            if (current == null) {
                current = new ClientStats();
                statsByClient.put(key, current);
            }
            current.rows++;
            String pedido = pedidoIdx < 0 ? "" : String.valueOf(tableModel.getValueAt(modelRow, pedidoIdx)).trim();
            if (!pedido.isEmpty()) {
                current.pedidos.add(pedido);
            }
            totalVisible++;
            totalCantidad++;
        }
        if (totalVisible == 0) {
            JOptionPane.showMessageDialog(this, "No hay filas visibles para calcular estadisticas.");
            return;
        }

        List<Map.Entry<String, ClientStats>> entries = new ArrayList<Map.Entry<String, ClientStats>>(statsByClient.entrySet());
        entries.sort((a, b) -> {
            int byPedidos = Integer.compare(b.getValue().pedidosCount(), a.getValue().pedidosCount());
            if (byPedidos != 0) {
                return byPedidos;
            }
            int byCantidad = Integer.compare(b.getValue().cantidadCount(), a.getValue().cantidadCount());
            if (byCantidad != 0) {
                return byCantidad;
            }
            return a.getKey().compareToIgnoreCase(b.getKey());
        });

        int top = Math.min(10, entries.size());
        StringBuilder sb = new StringBuilder();
        sb.append("Filas visibles: ").append(totalVisible).append('\n');
        sb.append("Clientes distintos: ").append(entries.size()).append('\n');
        sb.append("Cantidad total (filas): ").append(totalCantidad).append("\n\n");
        sb.append("Top ").append(top).append(" clientes por pedidos/cantidad:\n");
        for (int i = 0; i < top; i++) {
            Map.Entry<String, ClientStats> entry = entries.get(i);
            int pedidos = entry.getValue().pedidosCount();
            int cantidad = entry.getValue().cantidadCount();
            sb.append(i + 1)
                .append(". ")
                .append(entry.getKey())
                .append(" -> pedidos=")
                .append(pedidos)
                .append(", cantidad=")
                .append(cantidad)
                .append('\n');
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Estadisticas por cliente", JOptionPane.INFORMATION_MESSAGE);
    }

    private static final class ClientStats {
        private int rows;
        private final Set<String> pedidos = new HashSet<String>();

        private int pedidosCount() {
            if (!pedidos.isEmpty()) {
                return pedidos.size();
            }
            return rows;
        }

        private int cantidadCount() {
            return rows;
        }
    }

    private JPanel buildBottomActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        columnField = new JTextField("1", 4);
        JButton statsButton = new JButton("Calcular estadisticas");
        statsButton.addActionListener(event -> calculateStats());

        resultLabel = new JLabel("Resultado: pendiente");
        resultLabel.setHorizontalAlignment(SwingConstants.LEFT);
        clientHintLabel = new JLabel("Cliente: sugerencias activas");
        clientHintLabel.setHorizontalAlignment(SwingConstants.LEFT);
        filterLabel = new JLabel("Busqueda: (vacia)");
        filterLabel.setHorizontalAlignment(SwingConstants.LEFT);
        searchAndFilter.bindControls(searchField, searchScopeCombo, filterLabel);
        totalsLabel = new JLabel("Totales: pendientes");
        totalsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        validationLabel = new JLabel("Validacion: sin revisar");
        validationLabel.setHorizontalAlignment(SwingConstants.LEFT);
        kpiRowsLabel = new JLabel("Filas visibles: 0");
        kpiRowsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        kpiSumLabel = new JLabel("Suma visible: 0.00");
        kpiSumLabel.setHorizontalAlignment(SwingConstants.LEFT);
        kpiAvgLabel = new JLabel("Promedio visible: 0.00");
        kpiAvgLabel.setHorizontalAlignment(SwingConstants.LEFT);
        cellEditorField = new JTextField(20);
        JButton applyCellValueButton = new JButton("Aplicar a celda");
        applyCellValueButton.addActionListener(event -> applyFormulaFieldToSelectedCell());

        panel.add(new JLabel("Columna numerica (0..n):"));
        panel.add(columnField);
        panel.add(statsButton);
        addToolbarSeparator(panel);
        panel.add(new JLabel("Celda actual:"));
        panel.add(cellEditorField);
        panel.add(applyCellValueButton);
        addToolbarSeparator(panel);
        panel.add(resultLabel);
        addToolbarSeparator(panel);
        panel.add(clientHintLabel);
        addToolbarSeparator(panel);
        panel.add(filterLabel);
        addToolbarSeparator(panel);
        panel.add(totalsLabel);
        addToolbarSeparator(panel);
        panel.add(validationLabel);
        addToolbarSeparator(panel);
        panel.add(kpiRowsLabel);
        panel.add(kpiSumLabel);
        panel.add(kpiAvgLabel);
        addToolbarSeparator(panel);
        dbSyncStatusLabel = new JLabel("Sync BD: iniciando...");
        dbSyncStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(dbSyncStatusLabel);

        return panel;
    }

    private void addInitialRows() {
        tableModel.addRow(buildFormRow("C-001", "Cliente 1", "48074", "232", "82",  "15", "3P", "N",   "", "",         "Blanco",   "7",  true));
        tableModel.addRow(buildFormRow("C-001", "Cliente 1", "48075", "232", "112", "15", "3P", "N",   "", "",         "Blanco",   "7",  true));
        tableModel.addRow(buildFormRow("C-002", "Cliente 2", "48076", "517", "422", "18", "2P", "N",   "2P", "N",        "Rosa",     "10", true));
        tableModel.addRow(buildFormRow("C-002", "Cliente 2", "48077", "228", "168", "18", "2P", "N",   "2P", "N pe...",  "Rosa",     "10", true));
        tableModel.addRow(buildFormRow("C-003", "Cliente 3", "48108", "730", "310", "18", "2P", "N",   "2P", "N",        "Negro",    "10", false));
        tableModel.addRow(buildFormRow("C-003", "Cliente 3", "48127", "223", "259", "18", "2P", "N",   "", "",         "Amarillo", "7",  false));
        tableModel.addRow(buildFormRow("C-004", "Cliente 4", "48128", "671", "496", "18", "2P", "N",   "2P", "23'30",    "Negro",    "10", false));
        tableModel.addRow(buildFormRow("C-004", "Cliente 4", "48129", "338", "220", "18", "2P", "",    "2P", "",         "",         "",   false));
    }

    private Object[] buildEmptyFormRow() {
        return buildFormRow("", "", "", "", "", "", "", "", "", "", "", "", false);
    }

    /**
     * Fila vacia coherente con el numero actual de columnas (12 base + columnas dinamicas).
     */
    private Object[] buildEmptyRowForCurrentModel() {
        int n = tableModel.getColumnCount();
        Object[] base = buildEmptyFormRow();
        if (n <= base.length) {
            return Arrays.copyOf(base, n);
        }
        Object[] row = Arrays.copyOf(base, n);
        for (int c = base.length; c < n; c++) {
            row[c] = tableModel.getColumnClass(c) == Boolean.class ? Boolean.FALSE : "";
        }
        return row;
    }

    private void stopEditingBeforeTableMutation() {
        JTable jt = getDataTable();
        if (jt == null || !jt.isEditing()) {
            return;
        }
        CellEditor editor = jt.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    /**
     * Tras insertar en el modelo, el RowSorter puede actualizarse en el siguiente ciclo EDT.
     */
    private void selectAndScrollToModelRow(int modelRow) {
        JTable t = getDataTable();
        if (t == null || modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            int viewRow = t.convertRowIndexToView(modelRow);
            if (viewRow < 0) {
                return;
            }
            int viewCol = t.getSelectedColumn();
            if (viewCol < 0) {
                viewCol = 0;
            }
            t.setRowSelectionInterval(viewRow, viewRow);
            t.setColumnSelectionInterval(viewCol, viewCol);
            t.scrollRectToVisible(t.getCellRect(viewRow, viewCol, true));
        });
    }

    private Object[] buildFormRow(
        String cliente,
        String nombre,
        String num,
        String x, String y,
        String madera,
        String corteTam, String corteNota,
        String hendidoTam, String hendidoNota,
        String goma,
        String gtam,
        boolean hecho
    ) {
        return new Object[]{
            cliente, nombre, num, x, y, madera,
            corteTam, corteNota, hendidoTam, hendidoNota,
            goma, gtam, hecho
        };
    }

    private String buildRowDeleteLogDetail(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return "fila desconocida";
        }
        String troquel = "";
        int colNum = columnIndexOf(COL_NUM);
        if (colNum >= 0) {
            Object value = tableModel.getValueAt(modelRow, colNum);
            if (value != null) {
                troquel = value.toString().trim();
            }
        }
        String detalle = "fila " + (modelRow + 1);
        if (!troquel.isEmpty()) {
            detalle += ", Nº troquel " + troquel;
        }
        return detalle;
    }

    private void deleteSelectedRow() {
        stopEditingBeforeTableMutation();
        int selectedViewRow = getDataTable().getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para eliminar.");
            return;
        }
        int modelRow = getDataTable().convertRowIndexToModel(selectedViewRow);
        pendingRowDeleteLogDetail = buildRowDeleteLogDetail(modelRow);
        tableModel.removeRow(modelRow);
    }

    /**
     * Vacia el contenido de cada celda manteniendo las filas existentes y la estructura de
     * columnas (incluidas columnas adicionales). No elimina filas; tras la operacion, todas
     * las filas siguen en su sitio pero con celdas vacias (booleanos a false, resto a "").
     * La operacion va bajo {@link #historyOperationInProgress} para no contaminar el historial.
     */
    private void resetSpreadsheetTable() {
        stopEditingBeforeTableMutation();
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "La tabla no tiene filas que vaciar.");
            return;
        }
        int option = JOptionPane.showConfirmDialog(
            this,
            "Se vaciara el contenido de todas las celdas.\n"
                + "Las filas y las columnas se mantienen.\n"
                + "\u00bfContinuar?",
            "Reset tabla",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (option != JOptionPane.OK_OPTION) {
            return;
        }
        historyOperationInProgress = true;
        try {
            int columnCount = tableModel.getColumnCount();
            for (int r = 0; r < rowCount; r++) {
                for (int c = 0; c < columnCount; c++) {
                    Object empty = tableModel.getColumnClass(c) == Boolean.class ? Boolean.FALSE : "";
                    tableModel.setValueAt(empty, r, c);
                }
            }
            if (searchField != null) {
                searchField.setText("");
            }
            history.push(tableModel);
            applyCombinedFilter();
            refreshTotalsLabel();
            repaintAllDataTables();
            if (!dataTables.isEmpty()) {
                scheduleBannerRefresh(dataTables.get(0));
            }
            changeLog.record("Reset tabla", "celdas vaciadas en " + rowCount + " filas");
        } finally {
            historyOperationInProgress = false;
        }
    }

    private void insertRowRelativeToSelection(boolean above) {
        JTable t = getDataTable();
        if (t == null) {
            return;
        }
        int selectedViewRow = t.getSelectedRow();
        int targetModelRow = tableModel.getRowCount();
        if (selectedViewRow >= 0) {
            targetModelRow = t.convertRowIndexToModel(selectedViewRow);
            if (!above) {
                targetModelRow++;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para insertar arriba o abajo.");
            return;
        }
        insertRowAtModelRow(targetModelRow);
    }

    private void insertRowAtModelRow(int targetModelRow) {
        stopEditingBeforeTableMutation();
        if (targetModelRow < 0) {
            targetModelRow = 0;
        }
        if (targetModelRow > tableModel.getRowCount()) {
            targetModelRow = tableModel.getRowCount();
        }

        Object[] newRow = buildEmptyRowForCurrentModel();
        tableModel.insertRow(targetModelRow, newRow);
        selectAndScrollToModelRow(targetModelRow);
    }

    private void duplicateSelectedRow() {
        stopEditingBeforeTableMutation();
        int selectedViewRow = getDataTable().getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para duplicar.");
            return;
        }
        int modelRow = getDataTable().convertRowIndexToModel(selectedViewRow);
        int columns = tableModel.getColumnCount();
        Object[] copy = new Object[columns];
        for (int col = 0; col < columns; col++) {
            copy[col] = tableModel.getValueAt(modelRow, col);
        }
        tableModel.insertRow(modelRow + 1, copy);
        selectAndScrollToModelRow(modelRow + 1);
    }

    private void clearSelectedCell() {
        int selectedViewRow = getDataTable().getSelectedRow();
        int selectedViewCol = getDataTable().getSelectedColumn();
        if (selectedViewRow == -1 || selectedViewCol == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una celda para borrar.");
            return;
        }
        int modelRow = getDataTable().convertRowIndexToModel(selectedViewRow);
        int modelCol = getDataTable().convertColumnIndexToModel(selectedViewCol);
        tableModel.setValueAt("", modelRow, modelCol);
    }

    private void addColumnRelativeToSelection(boolean toLeft) {
        JTable jt = getDataTable();
        if (jt == null) return;
        if (jt.isEditing()) {
            jt.getCellEditor().stopCellEditing();
        }
        int selectedViewColumn = jt.getSelectedColumn();
        String defaultName = "Campo " + extraColumnCounter;
        String columnName = JOptionPane.showInputDialog(this, "Nombre de la nueva columna:", defaultName);
        if (columnName == null) {
            return;
        }
        columnName = columnName.trim();
        if (columnName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de columna no puede estar vacio.");
            return;
        }
        if (getColumnIndexByName(columnName) >= 0) {
            JOptionPane.showMessageDialog(this, "Ya existe una columna con ese nombre.");
            return;
        }
        extraColumnCounter++;
        final String finalColumnName = columnName;
        executeHistoryOperation(() -> appendAndPlaceColumn(finalColumnName, selectedViewColumn, toLeft));
        history.push(tableModel);
        applyCombinedFilter();
        scheduleBannerRefresh(getDataTable());
        changeLog.record(
            "Anadir columna",
            "'" + finalColumnName + "' (" + (toLeft ? "izquierda" : "derecha") + " de la seleccion)"
        );
    }

    private void appendAndPlaceColumn(String columnName, int selectedViewColumn, boolean toLeft) {
        tableModel.addColumn(columnName);
        configureTableColumns();

        int newViewIndex = getDataTable().getColumnModel().getColumnCount() - 1;
        if (selectedViewColumn < 0) {
            return;
        }
        int targetViewIndex = toLeft ? selectedViewColumn : selectedViewColumn + 1;
        if (targetViewIndex < 0) {
            targetViewIndex = 0;
        }
        if (targetViewIndex >= getDataTable().getColumnModel().getColumnCount()) {
            targetViewIndex = getDataTable().getColumnModel().getColumnCount() - 1;
        }
        getDataTable().moveColumn(newViewIndex, targetViewIndex);
    }

    private void configureTableColumns() {
        for (JTable t : dataTables) {
            configureTableColumnsFor(t);
            applyFormColumnWidths(t);
        }
    }

    private void configureTableColumnsFor(JTable jtable) {
        int valX = columnIndexOf(COL_X);
        int valY = columnIndexOf(COL_Y);
        int colCliente = columnIndexOf(COL_CLIENTE);
        int colNombre = columnIndexOf(COL_NOMBRE);
        int colGoma = columnIndexOf(COL_GOMA);
        int colGtam = columnIndexOf(COL_GTAM);
        int colMadera = columnIndexOf(COL_MADERA);
        int colCorteTam = columnIndexOf(COL_CORTE_TAM);
        int colHendidoTam = columnIndexOf(COL_HENDIDO_TAM);

        for (int col = 0; col < jtable.getColumnModel().getColumnCount(); col++) {
            int modelCol = jtable.convertColumnIndexToModel(col);
            Class<?> colClass = tableModel.getColumnClass(modelCol);
            TableColumn tc = jtable.getColumnModel().getColumn(col);
            if (colClass == Boolean.class) {
                tc.setCellEditor(jtable.getDefaultEditor(Boolean.class));
                tc.setCellRenderer(new FormBooleanRenderer());
            } else if (modelCol == colCliente || modelCol == colNombre) {
                tc.setCellEditor(new ClientSuggestCellEditor(modelCol));
            } else if (modelCol == valX || modelCol == valY) {
                tc.setCellEditor(new NumericCellEditor());
            } else if (modelCol == colMadera) {
                tc.setCellEditor(buildEditableComboEditor(MADERA_OPCIONES));
            } else if (modelCol == colGoma) {
                tc.setCellEditor(buildEditableComboEditor(GOMA_OPCIONES));
            } else if (modelCol == colGtam) {
                tc.setCellEditor(buildEditableComboEditor(GTAM_OPCIONES));
            } else if (modelCol == colCorteTam || modelCol == colHendidoTam) {
                tc.setCellEditor(buildEditableComboEditor(P2P3P_OPCIONES));
            } else {
                tc.setCellEditor(new DefaultCellEditor(new JTextField()));
            }
        }
        jtable.setDefaultRenderer(Object.class, new FormCellRenderer());
    }

    private void updateClientSuggestionHintFromText(int modelRow, int modelCol, String text) {
        if (clientHintLabel == null) {
            return;
        }
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        int codIdx = columnIndexOf(COL_CLIENTE);
        clearPendingClientSuggestion();

        String typed = text == null ? "" : text.trim();
        if (typed.isEmpty()) {
            clientHintLabel.setText("Cliente: sugerencias activas");
            return;
        }
        if (modelCol == nombreIdx) {
            String exactCode = findClientCodeByName(typed);
            if (exactCode != null) {
                clientHintLabel.setText("Cliente reconocido: codigo " + exactCode);
                return;
            }
            String suggestion = suggestClientByName(typed);
            if (suggestion != null) {
                clientHintLabel.setText("Sugerencia nombre: " + suggestion + " (Ctrl+B para aplicar)");
                pendingSuggestionModelRow = modelRow;
                pendingSuggestionModelCol = nombreIdx;
                pendingSuggestionName = suggestion;
                pendingSuggestionCode = findClientCodeByName(suggestion);
            } else {
                clientHintLabel.setText("Sugerencia nombre: sin coincidencias");
            }
            return;
        }
        if (modelCol == codIdx) {
            String exactName = findClientNameByCode(typed);
            if (exactName != null) {
                clientHintLabel.setText("Cliente reconocido: " + exactName);
                return;
            }
            String suggestion = suggestClientByCode(typed);
            if (suggestion != null) {
                clientHintLabel.setText("Sugerencia codigo: " + suggestion + " (Ctrl+B para aplicar)");
                int sep = suggestion.indexOf(" - ");
                pendingSuggestionModelRow = modelRow;
                pendingSuggestionModelCol = codIdx;
                pendingSuggestionCode = sep > 0 ? suggestion.substring(0, sep).trim() : null;
                pendingSuggestionName = sep > 0 ? suggestion.substring(sep + 3).trim() : null;
            } else {
                clientHintLabel.setText("Sugerencia codigo: sin coincidencias");
            }
        }
    }

    /**
     * Editor combobox editable: permite elegir de la lista o teclear un valor libre.
     */
    private DefaultCellEditor buildEditableComboEditor(String[] options) {
        JComboBox<String> combo = new JComboBox<String>(options);
        combo.setEditable(true);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return new DefaultCellEditor(combo);
    }

    private int getColumnIndexByName(String columnName) {
        return SpreadsheetSchema.columnIndex(tableModel, columnName);
    }

    /**
     * Calcula la ruta del fichero {@code cambios.log} de forma estable, independiente del directorio
     * de trabajo desde el que se lance la aplicacion. Se intenta primero {@code %USERPROFILE%/.troqueles/}
     * (en Windows) o {@code $HOME/.troqueles/}; si no se puede crear, se recurre al directorio actual.
     * Asi un usuario que arranque la app desde distintos accesos directos sigue viendo el mismo historial.
     */
    private static java.io.File resolveChangeLogFile() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            java.io.File dir = new java.io.File(userHome, ".troqueles");
            if (dir.exists() || dir.mkdirs()) {
                return new java.io.File(dir, "cambios.log");
            }
        }
        return new java.io.File("cambios.log");
    }

    private void setImageForSelectedRow() {
        int selectedViewRow = getDataTable().getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para añadir imagen.");
            return;
        }
        int modelRow = getDataTable().convertRowIndexToModel(selectedViewRow);
        setImageForModelRow(modelRow);
    }

    private void setImageForModelRow(int modelRow) {
        int imageColumnIndex = ensureImageColumnExists();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar imagen");
        chooser.setFileFilter(new FileNameExtensionFilter("Imagenes", "png", "jpg", "jpeg", "gif", "webp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String imagePath = chooser.getSelectedFile().getAbsolutePath();
        imageColumnSupport.removeFromCache(imagePath);
        tableModel.setValueAt(imagePath, modelRow, imageColumnIndex);
        applyImageColumnSizing();
        adjustRowHeightForImage(modelRow);
        repaintAllDataTables();
    }

    private int ensureImageColumnExists() {
        int imageColumnIndex = getColumnIndexByName(IMAGE_COLUMN_NAME);
        if (imageColumnIndex >= 0) {
            return imageColumnIndex;
        }
        executeHistoryOperation(() -> tableModel.addColumn(IMAGE_COLUMN_NAME));
        history.push(tableModel);
        // Configurar SOLO la nueva columna en lugar de llamar a configureTableColumns(),
        // que reasigna anchos del resto y descarta cualquier ajuste manual del usuario.
        installImageColumnRenderer();
        applyImageColumnSizing();
        return getColumnIndexByName(IMAGE_COLUMN_NAME);
    }

    /**
     * Reinstala el renderer por defecto en cada JTable. {@link FormCellRenderer} detecta la columna
     * Imagen para mostrar miniaturas, asi que basta con asegurar que sea el renderer por defecto.
     */
    private void installImageColumnRenderer() {
        for (JTable t : dataTables) {
            t.setDefaultRenderer(Object.class, new FormCellRenderer());
        }
    }

    /** Asegura ancho minimo de la columna Imagen en todas las JTable activas. */
    private void applyImageColumnSizing() {
        imageColumnSupport.applyImageColumnSizing();
    }

    /** Ajusta la altura de la fila concreta para que quepa la miniatura. */
    private void adjustRowHeightForImage(int modelRow) {
        imageColumnSupport.adjustRowHeightForImage(modelRow);
    }

    /** Recorre la columna Imagen y ajusta la altura de las filas que tengan path no vacio. */
    private void adjustRowHeightsForExistingImages() {
        imageColumnSupport.adjustRowHeightsForExistingImages();
    }

    private void configureDropdownForSelectedColumn() {
        int selectedViewColumn = getDataTable().getSelectedColumn();
        if (selectedViewColumn == -1) {
            Integer modelColumn = promptSelectColumnIndex("Selecciona la columna para configurar desplegable:");
            if (modelColumn == null) {
                return;
            }
            configureDropdownForColumn(modelColumn);
            return;
        }
        int modelColumn = getDataTable().convertColumnIndexToModel(selectedViewColumn);
        configureDropdownForColumn(modelColumn);
    }

    private void configureDropdownForColumn(int modelColumn) {
        String input = JOptionPane.showInputDialog(
            this,
            "Escribe opciones separadas por coma (ej: A,B,C):",
            "Configurar desplegable para " + tableModel.getColumnName(modelColumn),
            JOptionPane.PLAIN_MESSAGE
        );
        if (input == null) {
            return;
        }

        String[] raw = input.split(",");
        List<String> cleaned = new ArrayList<String>();
        for (String option : raw) {
            String trimmed = option.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        if (cleaned.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se definieron opciones validas.");
            return;
        }

        dropdownOptionsByColumnIndex.put(modelColumn, cleaned.toArray(new String[0]));
        configureTableColumns();
    }

    private void clearDropdownForSelectedColumn() {
        int selectedViewColumn = getDataTable().getSelectedColumn();
        if (selectedViewColumn == -1) {
            Integer modelColumn = promptSelectColumnIndex("Selecciona la columna para quitar desplegable:");
            if (modelColumn == null) {
                return;
            }
            clearDropdownForColumn(modelColumn);
            return;
        }
        int modelColumn = getDataTable().convertColumnIndexToModel(selectedViewColumn);
        clearDropdownForColumn(modelColumn);
    }

    private void clearDropdownForColumn(int modelColumn) {
        dropdownOptionsByColumnIndex.remove(modelColumn);
        configureTableColumns();
    }

    private void showActiveDropdowns() {
        if (dropdownOptionsByColumnIndex.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay desplegables configurados.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Desplegables activos:\n\n");
        int activeCount = 0;
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            String columnName = tableModel.getColumnName(col);
            String[] options = dropdownOptionsByColumnIndex.get(col);
            if (options == null || options.length == 0) {
                continue;
            }
            activeCount++;
            builder.append("- ").append(columnName).append(": ");
            for (int i = 0; i < options.length; i++) {
                builder.append(options[i]);
                if (i < options.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append("\n");
        }
        if (activeCount == 0) {
            JOptionPane.showMessageDialog(this, "No hay desplegables activos en las columnas actuales.");
            return;
        }
        JOptionPane.showMessageDialog(this, builder.toString(), "Estado de desplegables", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportDropdownsSummaryToCsv() {
        if (dropdownOptionsByColumnIndex.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay desplegables configurados para exportar.");
            return;
        }
        JFileChooser chooser = buildCsvChooser(
            "Exportar resumen de desplegables",
            buildSheetBasedFileName("desplegables", "csv")
        );
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File output = SpreadsheetCsv.normalizeCsvPath(chooser.getSelectedFile());
        output = ensureVersionedOutputFile(output);
        try (BufferedWriter writer = createUtf8Writer(output)) {
            writer.write("columna,opciones,total_opciones\n");
            int exportedRows = 0;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                String columnName = tableModel.getColumnName(col);
                String[] options = dropdownOptionsByColumnIndex.get(col);
                if (options == null || options.length == 0) {
                    continue;
                }
                exportedRows++;
                writer.write(escapeCsv(columnName));
                writer.write(",");
                writer.write(escapeCsv(String.join("|", options)));
                writer.write(",");
                writer.write(String.valueOf(options.length));
                writer.write("\n");
            }
            if (exportedRows == 0) {
                JOptionPane.showMessageDialog(this, "No hay desplegables activos en las columnas actuales.");
                return;
            }
            JOptionPane.showMessageDialog(this, "Resumen de desplegables exportado: " + output.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar desplegables: " + ex.getMessage());
        }
    }

    private String escapeCsv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Integer promptSelectColumnIndex(String message) {
        if (tableModel.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay columnas disponibles.");
            return null;
        }
        String[] options = new String[tableModel.getColumnCount()];
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            options[i] = String.format(Locale.US, "%d - %s", i, tableModel.getColumnName(i));
        }
        String selected = (String) JOptionPane.showInputDialog(
            this,
            message,
            "Seleccion de columna",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );
        if (selected == null) {
            return null;
        }
        int dashIndex = selected.indexOf(" - ");
        if (dashIndex <= 0) {
            return null;
        }
        try {
            return Integer.parseInt(selected.substring(0, dashIndex).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void applyCombinedFilter() {
        searchAndFilter.applyCombinedFilter();
    }

    private void showSearchHelp() {
        searchAndFilter.showSearchHelp();
    }

    private void calculateStats() {
        int columnIndex;
        try {
            columnIndex = Integer.parseInt(columnField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El indice de columna debe ser numerico.");
            return;
        }

        if (columnIndex < 0 || columnIndex >= tableModel.getColumnCount()) {
            JOptionPane.showMessageDialog(this, "Indice fuera de rango.");
            return;
        }

        SpreadsheetStats.NumericStats stats = SpreadsheetStats.calculateColumnStats(tableModel, columnIndex);
        if (stats == null) {
            resultLabel.setText("Resultado: sin valores numericos en la columna");
            return;
        }
        resultLabel.setText(stats.toLabel());
    }

    private void saveAllRowsToCsv() {
        JFileChooser chooser = buildCsvChooser(
            "Guardar tabla completa",
            buildSheetBasedFileName("tabla", "csv")
        );
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File output = SpreadsheetCsv.normalizeCsvPath(chooser.getSelectedFile());
        output = ensureVersionedOutputFile(output);
        try {
            SpreadsheetCsv.exportModel(tableModel, output);
            changeLog.record(
                "Guardar CSV completo",
                output.getName() + " (" + tableModel.getRowCount() + " filas)"
            );
            JOptionPane.showMessageDialog(this, "Tabla guardada correctamente: " + output.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar tabla: " + ex.getMessage());
        }
    }

    private void saveAllRowsToDatabase() {
        String sheetName = extractSheetTitle(getTitle());
        try {
            long expectedVersion = expectedDbVersion(sheetName);
            DbWorkbookRepository.SaveResult result = dbWorkbookRepository.saveSheetOptimistic(
                sheetName,
                tableModel,
                expectedVersion,
                WORKER_NAME
            );
            if (result.isConflict()) {
                int choice = JOptionPane.showOptionDialog(
                    this,
                    "Otro trabajador guardo esta hoja antes que tu.\n"
                        + "Version local esperada: " + expectedVersion + "\n"
                        + "Version actual en BD: " + result.version() + "\n\n"
                        + "¿Que quieres hacer?",
                    "Conflicto de guardado",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"Recargar BD", "Sobrescribir", "Cancelar"},
                    "Recargar BD"
                );
                if (choice == 0) {
                    loadRowsFromDatabase();
                } else if (choice == 1) {
                    long forcedVersion = dbWorkbookRepository.saveSheetForce(sheetName, tableModel, WORKER_NAME);
                    dbSheetVersionByName.put(sheetName, forcedVersion);
                    dbAutoSync.updateLocalVersion(forcedVersion);
                    changeLog.record(
                        "Guardar BD forzado",
                        sheetName + " v" + forcedVersion + " (" + tableModel.getRowCount() + " filas)"
                    );
                    JOptionPane.showMessageDialog(this, "Sobrescritura completada en BD (v" + forcedVersion + ").");
                }
                return;
            }
            dbSheetVersionByName.put(sheetName, result.version());
            dbAutoSync.updateLocalVersion(result.version());
            changeLog.record(
                "Guardar BD",
                sheetName + " v" + result.version() + " (" + tableModel.getRowCount() + " filas)"
            );
            JOptionPane.showMessageDialog(this, "Tabla guardada en BD para hoja: " + sheetName + " (v" + result.version() + ")");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar en BD: " + ex.getMessage());
        }
    }

    private void showClientRankingFromDatabase() {
        String sheetName = extractSheetTitle(getTitle());
        try {
            List<DbWorkbookRepository.ClientRankingEntry> ranking = dbWorkbookRepository.loadClientRanking(sheetName);
            if (ranking.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "No hay ranking en BD para la hoja actual.\nGuarda primero con 'Guardar BD'.",
                    "Ranking clientes BD",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            StringBuilder sb = new StringBuilder();
            DbWorkbookRepository.ClientRankingEntry top = ranking.get(0);
            DbWorkbookRepository.ClientRankingEntry bottom = ranking.get(ranking.size() - 1);
            sb.append("Hoja: ").append(sheetName).append('\n');
            sb.append("Trabajador: ").append(WORKER_NAME).append('\n');
            sb.append("Clientes en ranking: ").append(ranking.size()).append("\n\n");
            sb.append("Mayor compra: ")
                .append(top.clientLabel())
                .append(" (filas=").append(top.rowCount())
                .append(", pedidos=").append(top.pedidosCount())
                .append(")\n");
            sb.append("Menor compra: ")
                .append(bottom.clientLabel())
                .append(" (filas=").append(bottom.rowCount())
                .append(", pedidos=").append(bottom.pedidosCount())
                .append(")\n\n");
            sb.append("Top 10:\n");
            int topLimit = Math.min(10, ranking.size());
            for (int i = 0; i < topLimit; i++) {
                DbWorkbookRepository.ClientRankingEntry entry = ranking.get(i);
                sb.append(i + 1)
                    .append(". ")
                    .append(entry.clientLabel())
                    .append(" -> filas=")
                    .append(entry.rowCount())
                    .append(", pedidos=")
                    .append(entry.pedidosCount())
                    .append('\n');
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Ranking clientes BD", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error consultando ranking en BD: " + ex.getMessage());
        }
    }

    private void exportVisibleRowsToCsv() {
        JFileChooser chooser = buildCsvChooser(
            "Guardar CSV visible",
            buildSheetBasedFileName("visible", "csv")
        );
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File output = SpreadsheetCsv.normalizeCsvPath(chooser.getSelectedFile());
        output = ensureVersionedOutputFile(output);
        try {
            SpreadsheetCsv.exportVisibleAll(tableModel, dataTables, output);
            int visibles = dataTables.isEmpty() ? tableModel.getRowCount() : dataTables.get(0).getRowCount();
            changeLog.record(
                "Guardar CSV visible",
                output.getName() + " (" + visibles + " filas visibles)"
            );
            JOptionPane.showMessageDialog(this, "Exportacion completada: " + output.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar CSV: " + ex.getMessage());
        }
    }

    private void openDashboardWithCurrentData() {
        File dashboardDir = new File("dashboard");
        File indexFile = new File(dashboardDir, "index.html");
        if (!dashboardDir.exists() || !indexFile.exists()) {
            JOptionPane.showMessageDialog(this, "No se encontro el dashboard web en la carpeta esperada.");
            return;
        }

        exportDashboardSnapshotToJs(dashboardDir);
        if (dashboardServer == null) {
            startDashboardServer();
        }

        try {
            if (Desktop.isDesktopSupported()) {
                if (dashboardServer != null) {
                    String dashboardUrl = "http://127.0.0.1:" + dashboardServer.getPort() + "/index.html";
                    Desktop.getDesktop().browse(URI.create(dashboardUrl));
                } else {
                    Desktop.getDesktop().browse(indexFile.toURI());
                }
                return;
            }
            JOptionPane.showMessageDialog(this, "No se pudo abrir navegador automatico en este entorno.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir dashboard: " + ex.getMessage());
        }
    }

    private void exportDashboardSnapshotToJs(File dashboardDir) {
        File dataFile = new File(dashboardDir, "data.js");
        try (BufferedWriter writer = createUtf8Writer(dataFile)) {
            writer.write(
                DashboardSnapshot.buildDataScript(DashboardSnapshot.buildVisibleRowsFromTables(dataTables, tableModel))
            );
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error generando snapshot web: " + ex.getMessage());
        }
    }

    private void startDashboardServer() {
        File dashboardDir = new File("dashboard");
        if (!dashboardDir.exists()) {
            return;
        }
        try {
            dashboardServer = new DashboardServer(
                dashboardDir,
                () -> DashboardSnapshot.buildVisibleRowsFromTables(dataTables, tableModel)
            );
            dashboardServer.start();
        } catch (IOException ex) {
            dashboardServer = null;
            JOptionPane.showMessageDialog(this, "No se pudo iniciar servidor dashboard: " + ex.getMessage());
        }
    }

    private void stopDashboardServerQuietly() {
        if (dashboardServer != null) {
            dashboardServer.stop();
            dashboardServer = null;
        }
    }

    private void installDashboardShutdownHook() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                stopDbAutoSyncQuietly();
                stopDashboardServerQuietly();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopDbAutoSyncQuietly();
            stopDashboardServerQuietly();
        }, "troqueles-shutdown"));
    }

    private void startDbAutoSync() {
        dbAutoSync.setCallbacks(new DbAutoSyncService.Callbacks() {
            @Override
            public void onRemoteVersionNewer(long remoteVersion, long localVersion) {
                SwingUtilities.invokeLater(() -> handleRemoteVersionNewer(remoteVersion, localVersion));
            }

            @Override
            public void onStatusUpdate(String status) {
                SwingUtilities.invokeLater(() -> updateDbSyncStatusLabel(status));
            }

            @Override
            public void onForeignRowLocksChanged(Map<Integer, String> locksByOtherWorkers) {
                SwingUtilities.invokeLater(() -> applyForeignRowLocks(locksByOtherWorkers));
            }

            @Override
            public void onSyncError(String message) {
                SwingUtilities.invokeLater(() -> updateDbSyncStatusLabel(message));
            }
        });
        String sheetName = extractSheetTitle(getTitle());
        dbAutoSync.start(sheetName, expectedDbVersion(sheetName));
    }

    private void stopDbAutoSyncQuietly() {
        if (dbAutoSync != null) {
            dbAutoSync.stop();
        }
    }

    private void installDbSyncCellEditorListener(JTable table) {
        table.addPropertyChangeListener("tableCellEditor", event -> {
            Object newEditor = event.getNewValue();
            if (newEditor != null) {
                int viewRow = table.getEditingRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (!dbAutoSync.beginLocalRowEdit(modelRow)) {
                        SwingUtilities.invokeLater(() -> {
                            CellEditor editor = table.getCellEditor();
                            if (editor != null) {
                                editor.cancelCellEditing();
                            }
                        });
                    }
                }
            } else {
                dbAutoSync.endLocalRowEdit();
            }
        });
    }

    private void handleRemoteVersionNewer(long remoteVersion, long localVersion) {
        dbAutoSync.acknowledgeRemoteVersion(remoteVersion);
        int choice = JOptionPane.showOptionDialog(
            this,
            "Hay cambios en BD para esta hoja.\n"
                + "Version local: " + localVersion + "\n"
                + "Version remota: " + remoteVersion + "\n\n"
                + "¿Quieres recargar los datos?",
            "Cambios remotos detectados",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"Recargar BD", "Ignorar", "Desactivar sync"},
            "Recargar BD"
        );
        if (choice == 0) {
            loadRowsFromDatabase(true);
            updateDbSyncStatusLabel("Sync BD: recargado v" + remoteVersion);
            changeLog.record("Recarga auto BD", extractSheetTitle(getTitle()) + " v" + remoteVersion);
        } else if (choice == 2 && autoSyncMenuItem != null) {
            autoSyncMenuItem.setSelected(false);
            dbAutoSync.setEnabled(false);
            updateDbSyncStatusLabel("Sync BD desactivado");
        }
    }

    private void applyForeignRowLocks(Map<Integer, String> locksByOtherWorkers) {
        remoteRowLocksByModelRow = locksByOtherWorkers == null
            ? new HashMap<Integer, String>()
            : new HashMap<Integer, String>(locksByOtherWorkers);
        repaintAllDataTables();
        if (dbSyncStatusLabel != null) {
            int lockCount = remoteRowLocksByModelRow.size();
            dbSyncStatusLabel.setToolTipText(
                lockCount > 0 ? lockCount + " fila(s) bloqueada(s) por otros trabajadores" : null
            );
        }
    }

    private boolean isRowLockedByOtherWorker(int modelRow) {
        if (modelRow < 0) {
            return false;
        }
        String holder = remoteRowLocksByModelRow.get(Integer.valueOf(modelRow));
        return holder != null && !holder.trim().isEmpty();
    }

    private void applyRowLockTooltip(JLabel target, JTable table, int viewRow) {
        int modelRow = table.convertRowIndexToModel(viewRow);
        String holder = remoteRowLocksByModelRow.get(Integer.valueOf(modelRow));
        if (holder != null && !holder.trim().isEmpty()) {
            target.setToolTipText("Fila bloqueada por " + holder.trim());
        } else {
            target.setToolTipText(null);
        }
    }

    private void updateDbSyncStatusLabel(String status) {
        if (dbSyncStatusLabel != null) {
            dbSyncStatusLabel.setText("Sync BD: " + status);
        }
    }

    private void loadRowsFromCsv() {
        JFileChooser chooser = buildCsvChooser("Cargar tabla desde CSV", null);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            SpreadsheetCsv.CsvData data = SpreadsheetCsv.loadRows(chooser.getSelectedFile());
            List<Object[]> rows = data.getRows();
            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron filas validas para cargar.");
                return;
            }
            List<String> headers = data.getHeaders();
            applyLoadedRows(headers, rows);
            changeLog.record(
                "Cargar CSV",
                chooser.getSelectedFile().getName() + " (" + rows.size() + " filas)"
            );
            JOptionPane.showMessageDialog(this, "Carga completada. Filas importadas: " + rows.size());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar CSV: " + ex.getMessage());
        }
    }

    private void loadRowsFromDatabase() {
        loadRowsFromDatabase(false);
    }

    private void loadRowsFromDatabase(boolean silent) {
        String sheetName = extractSheetTitle(getTitle());
        try {
            DbWorkbookRepository.WorkbookData data = dbWorkbookRepository.loadSheet(sheetName);
            if (data == null) {
                if (!silent) {
                    JOptionPane.showMessageDialog(this, "No hay datos guardados en BD para la hoja: " + sheetName);
                }
                return;
            }
            applyLoadedRows(data.headers(), data.rows());
            dbSheetVersionByName.put(sheetName, data.version());
            dbAutoSync.updateLocalVersion(data.version());
            dbAutoSync.acknowledgeRemoteVersion(data.version());
            changeLog.record("Cargar BD", sheetName + " v" + data.version() + " (" + data.rows().size() + " filas)");
            if (!silent) {
                JOptionPane.showMessageDialog(
                    this,
                    "Carga desde BD completada. Filas importadas: " + data.rows().size() + " (v" + data.version() + ")"
                );
            }
        } catch (SQLException ex) {
            if (!silent) {
                JOptionPane.showMessageDialog(this, "Error al cargar desde BD: " + ex.getMessage());
            } else {
                updateDbSyncStatusLabel("Sync BD: error al recargar");
            }
        }
    }

    private long expectedDbVersion(String sheetName) {
        Long current = dbSheetVersionByName.get(sheetName);
        return current == null ? 0L : current.longValue();
    }

    private static String resolveWorkerName() {
        String user = System.getProperty("user.name");
        String host = System.getenv("COMPUTERNAME");
        String normalizedUser = (user == null || user.trim().isEmpty()) ? "desconocido" : user.trim();
        String normalizedHost = (host == null || host.trim().isEmpty()) ? "equipo" : host.trim();
        return normalizedUser + "@" + normalizedHost;
    }

    private void applyLoadedRows(List<String> headers, List<Object[]> rows) {
        executeHistoryOperation(() -> {
            tableModel.setColumnCount(0);
            for (String header : headers) {
                tableModel.addColumn(header);
            }
            tableModel.setRowCount(0);
            for (Object[] row : rows) {
                tableModel.addRow(row);
            }
        });
        invalidateColumnIndexCache();
        dropdownOptionsByColumnIndex.clear();
        history.push(tableModel);
        configureTableColumns();
        applyCombinedFilter();
        applyImageColumnSizing();
        adjustRowHeightsForExistingImages();
    }

    private void exportHtmlReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte HTML");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML (*.html)", "html"));
        chooser.setSelectedFile(new File(buildSheetBasedFileName("reporte", "html")));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        String path = selectedFile.getAbsolutePath().toLowerCase(Locale.ROOT).endsWith(".html")
            ? selectedFile.getAbsolutePath()
            : selectedFile.getAbsolutePath() + ".html";
        File outputFile = ensureVersionedOutputFile(new File(path));

        String html = SpreadsheetReport.buildHtmlFromTables(
            dataTables,
            tableModel,
            resultLabel.getText(),
            filterLabel.getText(),
            totalsLabel.getText()
        );

        try (BufferedWriter writer = createUtf8Writer(outputFile)) {
            writer.write(html);
            changeLog.record("Exportar HTML", outputFile.getName());
            JOptionPane.showMessageDialog(this, "Reporte HTML generado: " + outputFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar HTML: " + ex.getMessage());
        }
    }

    private BufferedWriter createUtf8Writer(File file) throws IOException {
        return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    }

    private void exportPdfReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File(buildSheetBasedFileName("reporte", "pdf")));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        String path = selectedFile.getAbsolutePath().toLowerCase(Locale.ROOT).endsWith(".pdf")
            ? selectedFile.getAbsolutePath()
            : selectedFile.getAbsolutePath() + ".pdf";
        File outputFile = ensureVersionedOutputFile(new File(path));

        try {
            PdfReportWriter.writeReportFromTables(
                outputFile,
                dataTables,
                tableModel,
                resultLabel.getText(),
                filterLabel.getText(),
                totalsLabel.getText()
            );
            changeLog.record("Exportar PDF", outputFile.getName());
            JOptionPane.showMessageDialog(this, "Reporte PDF generado: " + outputFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar PDF: " + ex.getMessage());
        }
    }

    private JFileChooser buildCsvChooser(String title, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (defaultName != null) {
            chooser.setSelectedFile(new File(defaultName));
        }
        return chooser;
    }

    private String buildSheetBasedFileName(String suffix, String extension) {
        exportNameTemplate = loadExportNameTemplate();
        String sheetTitle = extractSheetTitle(getTitle());
        String normalizedTitle = sanitizeForFileName(sheetTitle);
        String normalizedSuffix = sanitizeForFileName(suffix);
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
        String baseName;
        if ("tipo_titulo".equalsIgnoreCase(exportNameTemplate)) {
            baseName = normalizedSuffix + "_" + normalizedTitle;
        } else if ("titulo_fecha_tipo".equalsIgnoreCase(exportNameTemplate)) {
            baseName = normalizedTitle + "_" + timestamp + "_" + normalizedSuffix;
        } else if ("fecha_titulo_tipo".equalsIgnoreCase(exportNameTemplate)) {
            baseName = timestamp + "_" + normalizedTitle + "_" + normalizedSuffix;
        } else {
            baseName = normalizedTitle + "_" + normalizedSuffix;
        }
        return baseName + "." + extension;
    }

    private String sanitizeForFileName(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            return "hoja";
        }
        return normalized;
    }

    private File ensureVersionedOutputFile(File desiredFile) {
        if (desiredFile == null || !desiredFile.exists()) {
            return desiredFile;
        }
        String name = desiredFile.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        File parent = desiredFile.getParentFile();
        int version = 2;
        while (true) {
            String candidateName = baseName + "_v" + version + extension;
            File candidate = parent == null ? new File(candidateName) : new File(parent, candidateName);
            if (!candidate.exists()) {
                return candidate;
            }
            version++;
        }
    }

    // Wrappers de compatibilidad para tests existentes (reflexion) y puntos de uso internos.
    private static String normalizeClientCode(String raw) {
        return ClientLookup.normalizeClientCode(raw);
    }

    private static String normalizeClientNameForLookup(String raw) {
        return ClientLookup.normalizeClientNameForLookup(raw);
    }

    private String findClientCodeByName(String rawName) {
        return CLIENT_LOOKUP.findClientCodeByName(rawName);
    }

    private String findCanonicalClientNameByName(String rawName) {
        return CLIENT_LOOKUP.findCanonicalClientNameByName(rawName);
    }

    private String suggestClientByName(String rawName) {
        return CLIENT_LOOKUP.suggestClientByName(rawName);
    }

    private String suggestClientByCode(String rawCode) {
        return CLIENT_LOOKUP.suggestClientByCode(rawCode);
    }

    private void updateClientSuggestionHint(TableModelEvent event) {
        if (event == null || event.getType() != TableModelEvent.UPDATE || clientHintLabel == null) {
            return;
        }
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        int codIdx = columnIndexOf(COL_CLIENTE);
        int col = event.getColumn();
        int row = event.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        if (col == nombreIdx || col == codIdx) {
            Object value = tableModel.getValueAt(row, col);
            updateClientSuggestionHintFromText(row, col, value == null ? "" : String.valueOf(value));
        }
    }

    private void clearPendingClientSuggestion() {
        pendingSuggestionModelRow = -1;
        pendingSuggestionModelCol = -1;
        pendingSuggestionCode = null;
        pendingSuggestionName = null;
    }

    private boolean applyPendingClientSuggestionForCell(int modelRow, int modelCol) {
        if (modelRow < 0 || modelCol < 0 || pendingSuggestionModelRow < 0 || pendingSuggestionModelCol < 0) {
            return false;
        }
        if (modelRow != pendingSuggestionModelRow || modelCol != pendingSuggestionModelCol) {
            return false;
        }
        int codIdx = columnIndexOf(COL_CLIENTE);
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        boolean applied = false;
        boolean prev = historyOperationInProgress;
        historyOperationInProgress = true;
        try {
            // Aplicar ambos campos de forma sincronizada.
            // Prioridad: nombre sugerido (si existe), y codigo asociado.
            if (pendingSuggestionName != null && nombreIdx >= 0) {
                tableModel.setValueAt(pendingSuggestionName, modelRow, nombreIdx);
                applied = true;
            }
            if (pendingSuggestionCode != null && codIdx >= 0) {
                tableModel.setValueAt(pendingSuggestionCode, modelRow, codIdx);
                applied = true;
            }
        } finally {
            historyOperationInProgress = prev;
        }
        clearPendingClientSuggestion();
        if (applied && clientHintLabel != null) {
            clientHintLabel.setText("Sugerencia aplicada (Ctrl+B)");
        }
        if (applied) {
            history.push(tableModel);
        }
        return applied;
    }

    private String findClientNameByCode(String rawCode) {
        return CLIENT_LOOKUP.findClientNameByCode(rawCode);
    }

    private void tryAutoFillClientCode(TableModelEvent event) {
        if (event == null || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        int codIdx = columnIndexOf(COL_CLIENTE);
        if (nombreIdx < 0 || codIdx < 0 || event.getColumn() != nombreIdx) {
            return;
        }
        int row = event.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }

        Object rawName = tableModel.getValueAt(row, nombreIdx);
        String typedName = rawName == null ? "" : String.valueOf(rawName);
        String code = findClientCodeByName(typedName);
        if (code == null || code.trim().isEmpty()) {
            return;
        }

        String currentCode = String.valueOf(tableModel.getValueAt(row, codIdx)).trim();
        if (!code.equals(currentCode)) {
            tableModel.setValueAt(code, row, codIdx);
        }
        String canonicalName = findCanonicalClientNameByName(typedName);
        if (canonicalName != null && !canonicalName.equals(typedName.trim())) {
            tableModel.setValueAt(canonicalName, row, nombreIdx);
        }
    }

    private void tryAutoFillClientName(TableModelEvent event) {
        if (event == null || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int nombreIdx = columnIndexOf(COL_NOMBRE);
        int codIdx = columnIndexOf(COL_CLIENTE);
        if (nombreIdx < 0 || codIdx < 0 || event.getColumn() != codIdx) {
            return;
        }
        int row = event.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        Object rawCode = tableModel.getValueAt(row, codIdx);
        String name = findClientNameByCode(rawCode == null ? "" : String.valueOf(rawCode));
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String currentName = String.valueOf(tableModel.getValueAt(row, nombreIdx)).trim();
        if (!name.equals(currentName)) {
            tableModel.setValueAt(name, row, nombreIdx);
        }
    }

    /**
     * Mantiene coherente la columna Imagen ante cambios externos al flujo {@code setImageForModelRow}:
     * recalcula la altura de la fila (creciendo si hay path, encogiendo si se vacia) e invalida
     * el cache de miniaturas para que el renderer recargue desde disco si el path apunta a otro fichero.
     */
    private void handleImageCellUpdate(TableModelEvent event) {
        imageColumnSupport.handleImageCellUpdate(event);
    }

    /**
     * Suma +5 al valor introducido por el usuario en una celda X / Y cada vez que se edita.
     * Esta pensado para que el operario teclee siempre la medida real del troquel y la app
     * incremente automaticamente los 5 mm de margen, tambien cuando se corrige una medida ya escrita.
     * Para evitar recursion infinita, se usa la bandera {@link #historyOperationInProgress}
     * mientras se reescribe el valor ajustado.
     */
    private void tryAutoAddFiveOnXy(TableModelEvent event) {
        if (event == null || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        if (historyOperationInProgress) {
            return;
        }
        int col = event.getColumn();
        if (col == TableModelEvent.ALL_COLUMNS) {
            return;
        }
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        if (col != colX && col != colY) {
            return;
        }
        int row = event.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }

        Object raw = tableModel.getValueAt(row, col);
        String input = raw == null ? "" : raw.toString().trim();
        if (input.isEmpty()) {
            return;
        }
        Double parsed = SpreadsheetStats.tryParseDouble(input);
        if (parsed == null) {
            return;
        }

        String adjusted = formatFormulaNumber(parsed.doubleValue() + 5.0);
        if (adjusted.equals(input)) {
            return;
        }

        boolean prev = historyOperationInProgress;
        historyOperationInProgress = true;
        try {
            tableModel.setValueAt(adjusted, row, col);
        } finally {
            historyOperationInProgress = prev;
        }
    }

    private void installStatusUpdates() {
        tableModel.addTableModelListener(event -> {
            tryAutoAddFiveOnXy(event);
            tryAutoFillClientCode(event);
            tryAutoFillClientName(event);
            handleImageCellUpdate(event);
            updateClientSuggestionHint(event);
            updateValidationSummaryLabel();
            if (shouldRefreshTotals(event)) {
                refreshTotalsLabel();
            }
            if (shouldRefreshValidationPaint(event)) {
                repaintAllDataTables();
            }
            int hechoIdx = columnIndexOf(COL_HECHO);
            if (event.getType() == TableModelEvent.UPDATE
                && (event.getColumn() == TableModelEvent.ALL_COLUMNS || event.getColumn() == hechoIdx)) {
                repaintAllDataTables();
            }
            if (!historyOperationInProgress
                && (event.getType() == TableModelEvent.INSERT
                || event.getType() == TableModelEvent.UPDATE
                || event.getType() == TableModelEvent.DELETE)) {
                history.push(tableModel);
                logTableModelEvent(event);
            }
        });
        for (TableRowSorter<DefaultTableModel> sorter : dataSorters) {
            sorter.addRowSorterListener(event -> refreshTotalsLabel());
        }
    }

    private boolean shouldRefreshTotals(TableModelEvent event) {
        if (event == null) {
            return true;
        }
        int type = event.getType();
        if (type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
            return true;
        }
        if (type != TableModelEvent.UPDATE) {
            return false;
        }
        int col = event.getColumn();
        if (col == TableModelEvent.ALL_COLUMNS) {
            return true;
        }
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        int colHecho = columnIndexOf(COL_HECHO);
        if (col == colX || col == colY || col == colHecho) {
            return true;
        }
        // Con busqueda activa, cualquier columna puede afectar filas visibles.
        String searchText = searchField == null ? "" : searchField.getText().trim();
        return !searchText.isEmpty();
    }

    private void logTableModelEvent(TableModelEvent event) {
        if (event.getType() == TableModelEvent.UPDATE) {
            int col = event.getColumn();
            int row = event.getFirstRow();
            if (col == TableModelEvent.ALL_COLUMNS || row < 0 || row >= tableModel.getRowCount()) {
                return;
            }
            if (col < 0 || col >= tableModel.getColumnCount()) {
                return;
            }
            String columnName = tableModel.getColumnName(col);
            Object value = tableModel.getValueAt(row, col);
            String shown = value == null ? "" : value.toString();
            if (shown.length() > 60) {
                shown = shown.substring(0, 57) + "...";
            }
            changeLog.record(
                "Editada celda",
                "fila " + (row + 1) + ", columna '" + columnName + "' = '" + shown + "'"
            );
        } else if (event.getType() == TableModelEvent.INSERT) {
            int from = event.getFirstRow();
            int to = event.getLastRow();
            int count = Math.max(1, to - from + 1);
            String detalle = count == 1
                ? "fila " + (from + 1)
                : count + " filas (" + (from + 1) + ".." + (to + 1) + ")";
            changeLog.record("Fila(s) anadida(s)", detalle);
        } else if (event.getType() == TableModelEvent.DELETE) {
            String detalle;
            if (pendingRowDeleteLogDetail != null) {
                detalle = pendingRowDeleteLogDetail;
                pendingRowDeleteLogDetail = null;
            } else {
                int from = event.getFirstRow();
                int to = event.getLastRow();
                int count = Math.max(1, to - from + 1);
                detalle = count == 1
                    ? "fila " + (from + 1)
                    : count + " filas (" + (from + 1) + ".." + (to + 1) + ")";
            }
            changeLog.record("Fila(s) eliminada(s)", detalle);
        }
    }

    private boolean shouldRefreshValidationPaint(TableModelEvent event) {
        if (event == null) {
            return true;
        }
        int type = event.getType();
        if (type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
            return true;
        }
        if (type != TableModelEvent.UPDATE) {
            return false;
        }
        int col = event.getColumn();
        if (col == TableModelEvent.ALL_COLUMNS) {
            return true;
        }
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        int colCliente = columnIndexOf(COL_CLIENTE);
        int colNombre = columnIndexOf(COL_NOMBRE);
        return col == colX || col == colY || col == colCliente || col == colNombre;
    }

    private void updateValidationSummaryLabel() {
        if (validationLabel == null) {
            return;
        }
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        int colCliente = columnIndexOf(COL_CLIENTE);
        int colNombre = columnIndexOf(COL_NOMBRE);
        int invalidXy = 0;
        int missingClient = 0;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (colX >= 0) {
                String x = String.valueOf(tableModel.getValueAt(row, colX)).trim();
                if (!x.isEmpty() && SpreadsheetStats.tryParseDouble(x) == null) {
                    invalidXy++;
                }
            }
            if (colY >= 0) {
                String y = String.valueOf(tableModel.getValueAt(row, colY)).trim();
                if (!y.isEmpty() && SpreadsheetStats.tryParseDouble(y) == null) {
                    invalidXy++;
                }
            }
            String code = colCliente < 0 ? "" : String.valueOf(tableModel.getValueAt(row, colCliente)).trim();
            String name = colNombre < 0 ? "" : String.valueOf(tableModel.getValueAt(row, colNombre)).trim();
            if (code.isEmpty() || name.isEmpty()) {
                missingClient++;
            }
        }
        validationLabel.setText(
            String.format(Locale.US, "Validacion: XY invalidos=%d | Cliente incompleto=%d", invalidXy, missingClient)
        );
        validationLabel.setForeground((invalidXy > 0 || missingClient > 0) ? new Color(140, 46, 20) : new Color(28, 110, 60));
    }

    private void refreshTotalsLabel() {
        normalizeModelRowWidths();
        int totalRows = tableModel.getRowCount();
        int colX = columnIndexOf(COL_X);
        int colY = columnIndexOf(COL_Y);
        int colHecho = columnIndexOf(COL_HECHO);
        double sumX = 0.0;
        double sumY = 0.0;
        int visibleRows = 0;
        int hechos = 0;
        if (!dataTables.isEmpty()) {
            JTable t = dataTables.get(0);
            for (int viewRow = 0; viewRow < t.getRowCount(); viewRow++) {
                int modelRow = t.convertRowIndexToModel(viewRow);
                if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
                    continue;
                }
                visibleRows++;
                if (colX >= 0) {
                    Double vx = SpreadsheetStats.tryParseDouble(String.valueOf(tableModel.getValueAt(modelRow, colX)));
                    if (vx != null) sumX += vx;
                }
                if (colY >= 0) {
                    Double vy = SpreadsheetStats.tryParseDouble(String.valueOf(tableModel.getValueAt(modelRow, colY)));
                    if (vy != null) sumY += vy;
                }
                if (colHecho >= 0 && Boolean.TRUE.equals(tableModel.getValueAt(modelRow, colHecho))) {
                    hechos++;
                }
            }
        }
        totalsLabel.setText(String.format(Locale.US, "Visibles: %d/%d (hechos: %d)", visibleRows, totalRows, hechos));
        kpiRowsLabel.setText(String.format(Locale.US, "Troqueles visibles: %d", visibleRows));
        kpiSumLabel.setText(String.format(Locale.US, "ΣX: %.2f", sumX));
        kpiAvgLabel.setText(String.format(Locale.US, "ΣY: %.2f", sumY));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void normalizeModelRowWidths() {
        int columns = tableModel.getColumnCount();
        Vector data = tableModel.getDataVector();
        for (int i = 0; i < data.size(); i++) {
            Object rowObj = data.get(i);
            if (!(rowObj instanceof Vector)) {
                continue;
            }
            Vector row = (Vector) rowObj;
            while (row.size() < columns) {
                int nextCol = row.size();
                row.add(tableModel.getColumnClass(nextCol) == Boolean.class ? Boolean.FALSE : "");
            }
            while (row.size() > columns) {
                row.remove(row.size() - 1);
            }
        }
    }

    private void installKeyboardShortcuts() {
        ShortcutBindingsInstaller.install(getRootPane(), toolbarActions);
    }

    private void installClickShortcuts() {
        for (JTable jt : dataTables) {
            jt.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() != 1) {
                        return;
                    }
                    int viewColumn = jt.columnAtPoint(event.getPoint());
                    if (viewColumn < 0) {
                        return;
                    }
                    showColumnQuickMenu(jt, viewColumn, event.getX(), event.getY());
                }
            });

            jt.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() != 2) {
                        return;
                    }
                    int viewRow = jt.rowAtPoint(event.getPoint());
                    int viewColumn = jt.columnAtPoint(event.getPoint());
                    if (viewRow < 0 || viewColumn < 0) {
                        return;
                    }
                    int modelColumn = jt.convertColumnIndexToModel(viewColumn);
                    String columnName = tableModel.getColumnName(modelColumn);
                    if (IMAGE_COLUMN_NAME.equalsIgnoreCase(columnName)) {
                        int modelRow = jt.convertRowIndexToModel(viewRow);
                        setImageForModelRow(modelRow);
                    }
                }
            });

            jt.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    maybeShowTableContextMenu(jt, event);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    maybeShowTableContextMenu(jt, event);
                }
            });
        }
    }

    private void showColumnQuickMenu(JTable contextTable, int viewColumn, int x, int y) {
        int modelColumn = contextTable.convertColumnIndexToModel(viewColumn);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem addLeft = new JMenuItem("Añadir columna a la izquierda");
        addLeft.addActionListener(event -> addColumnRelativeToSelection(true));
        JMenuItem addRight = new JMenuItem("Añadir columna a la derecha");
        addRight.addActionListener(event -> addColumnRelativeToSelection(false));
        JMenuItem configDropdown = new JMenuItem("Configurar desplegable");
        configDropdown.addActionListener(event -> configureDropdownForColumn(modelColumn));
        JMenuItem removeDropdown = new JMenuItem("Quitar desplegable");
        removeDropdown.addActionListener(event -> clearDropdownForColumn(modelColumn));
        JMenuItem renameColumn = new JMenuItem("Renombrar columna");
        renameColumn.addActionListener(event -> renameColumn(modelColumn));
        JMenuItem autoFitColumn = new JMenuItem("Autoajustar columna");
        autoFitColumn.addActionListener(event -> autoFitColumnWidth(viewColumn));
        JMenuItem autoFitAll = new JMenuItem("Autoajustar todas");
        autoFitAll.addActionListener(event -> autoFitAllColumns());

        contextTable.setColumnSelectionInterval(viewColumn, viewColumn);
        menu.add(addLeft);
        menu.add(addRight);
        menu.addSeparator();
        menu.add(configDropdown);
        menu.add(removeDropdown);
        menu.add(renameColumn);
        menu.addSeparator();
        menu.add(autoFitColumn);
        menu.add(autoFitAll);
        menu.show(contextTable.getTableHeader(), x, y);
    }

    private void maybeShowTableContextMenu(JTable contextTable, MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int viewRow = contextTable.rowAtPoint(event.getPoint());
        int viewColumn = contextTable.columnAtPoint(event.getPoint());
        if (viewRow < 0 || viewColumn < 0) {
            return;
        }
        contextTable.setRowSelectionInterval(viewRow, viewRow);
        contextTable.setColumnSelectionInterval(viewColumn, viewColumn);
        activeDataTable = contextTable;
        showTableQuickMenu(contextTable, viewRow, viewColumn, event.getX(), event.getY());
    }

    private void showTableQuickMenu(JTable contextTable, int viewRow, int viewColumn, int x, int y) {
        int modelRow = contextTable.convertRowIndexToModel(viewRow);
        int modelColumn = contextTable.convertColumnIndexToModel(viewColumn);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem rowAbove = new JMenuItem("Insertar fila arriba");
        rowAbove.addActionListener(event -> insertRowAtModelRow(modelRow));
        JMenuItem rowBelow = new JMenuItem("Insertar fila abajo");
        rowBelow.addActionListener(event -> insertRowAtModelRow(modelRow + 1));
        JMenuItem duplicate = new JMenuItem("Duplicar fila");
        duplicate.addActionListener(event -> duplicateSelectedRow());
        JMenuItem deleteRow = new JMenuItem("Eliminar fila");
        deleteRow.addActionListener(event -> deleteSelectedRow());
        JMenuItem clearCell = new JMenuItem("Borrar celda");
        clearCell.addActionListener(event -> clearSelectedCell());
        JMenuItem addImage = new JMenuItem("Añadir/Cambiar imagen fila");
        addImage.addActionListener(event -> setImageForModelRow(modelRow));

        menu.add(rowAbove);
        menu.add(rowBelow);
        menu.add(duplicate);
        menu.add(deleteRow);
        menu.addSeparator();
        menu.add(clearCell);
        menu.add(addImage);

        menu.show(contextTable, x, y);
    }

    private void autoFitColumnWidth(int viewColumn) {
        autoFitColumnWidthFor(getDataTable(), viewColumn);
    }

    private void autoFitColumnWidthFor(JTable jtable, int viewColumn) {
        TableColumn column = jtable.getColumnModel().getColumn(viewColumn);
        int headerWidth = jtable.getTableHeader().getFontMetrics(jtable.getTableHeader().getFont())
            .stringWidth(column.getHeaderValue().toString()) + 30;
        int maxWidth = headerWidth;

        FontMetrics metrics = jtable.getFontMetrics(jtable.getFont());
        for (int row = 0; row < jtable.getRowCount(); row++) {
            Object value = jtable.getValueAt(row, viewColumn);
            int width = metrics.stringWidth(value == null ? "" : value.toString()) + 30;
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        column.setPreferredWidth(Math.min(maxWidth, 360));
    }

    private void autoFitAllColumns() {
        for (JTable jt : dataTables) {
            for (int col = 0; col < jt.getColumnModel().getColumnCount(); col++) {
                autoFitColumnWidthFor(jt, col);
            }
        }
    }

    private void installSelectionSync() {
        for (JTable jt : dataTables) {
            jt.getSelectionModel().addListSelectionListener(event -> updateFormulaFieldFromSelection());
            jt.getColumnModel().getSelectionModel().addListSelectionListener(event -> updateFormulaFieldFromSelection());
        }
    }

    private void styleControlPanel(JPanel panel) {
        panel.setBackground(TOOLBAR_BACKGROUND);
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                button.setBackground(BUTTON_BACKGROUND);
                button.setForeground(BUTTON_TEXT);
                button.setFocusPainted(false);
                button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(197, 207, 222)),
                    BorderFactory.createEmptyBorder(5, 9, 5, 9)
                ));
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (component instanceof JComboBox) {
                component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                component.setBackground(Color.WHITE);
                component.setForeground(BUTTON_TEXT);
            } else if (component instanceof JTextField) {
                component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            } else if (component instanceof JLabel) {
                component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
        }
    }

    private void updateFormulaFieldFromSelection() {
        int viewRow = getDataTable().getSelectedRow();
        int viewCol = getDataTable().getSelectedColumn();
        if (viewRow == -1 || viewCol == -1) {
            return;
        }
        Object value = getDataTable().getValueAt(viewRow, viewCol);
        cellEditorField.setText(value == null ? "" : value.toString());
    }

    private void applyFormulaFieldToSelectedCell() {
        int viewRow = getDataTable().getSelectedRow();
        int viewCol = getDataTable().getSelectedColumn();
        if (viewRow == -1 || viewCol == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una celda para aplicar el valor.");
            return;
        }
        int modelRow = getDataTable().convertRowIndexToModel(viewRow);
        int modelCol = getDataTable().convertColumnIndexToModel(viewCol);
        String columnName = tableModel.getColumnName(modelCol);
        String input = cellEditorField.getText() == null ? "" : cellEditorField.getText().trim();

        if (input.startsWith("=")) {
            try {
                double result = evaluateFormulaExpression(input.substring(1));
                String formatted = formatFormulaNumber(result);
                tableModel.setValueAt(formatted, modelRow, modelCol);
                cellEditorField.setText(formatted);
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Formula invalida: " + ex.getMessage());
                return;
            }
        }

        // Las columnas X / Y de la hoja de troqueles solo aceptan numericos. El resto admite texto libre.
        if ((COL_X.equals(columnName) || COL_Y.equals(columnName))
            && !input.isEmpty()
            && SpreadsheetStats.tryParseDouble(input) == null) {
            JOptionPane.showMessageDialog(this, "La columna " + columnName + " solo admite valores numericos.");
            return;
        }
        tableModel.setValueAt(input, modelRow, modelCol);
    }

    private double evaluateFormulaExpression(String expression) {
        String expr = expression == null ? "" : expression.trim();
        if (expr.isEmpty()) {
            throw new IllegalArgumentException("expresion vacia");
        }
        Matcher matcher = Pattern.compile("^([A-Za-z]+)\\((.*)\\)$").matcher(expr);
        if (!matcher.matches()) {
            return resolveNumericToken(expr);
        }

        String function = matcher.group(1).toUpperCase(Locale.ROOT);
        String rawArgs = matcher.group(2).trim();
        if (rawArgs.isEmpty()) {
            throw new IllegalArgumentException("faltan argumentos");
        }
        String[] args = rawArgs.split("\\s*[;,]\\s*");

        if ("SUMAR".equals(function)) {
            double sum = 0.0;
            for (String arg : args) {
                sum += sumTokenOrRange(arg);
            }
            return sum;
        }
        if ("RESTAR".equals(function)) {
            if (args.length < 2) {
                throw new IllegalArgumentException("RESTAR requiere al menos 2 argumentos");
            }
            double value = resolveNumericToken(args[0]);
            for (int i = 1; i < args.length; i++) {
                value -= resolveNumericToken(args[i]);
            }
            return value;
        }
        if ("MULTIPLICAR".equals(function)) {
            double value = 1.0;
            for (String arg : args) {
                value *= resolveNumericToken(arg);
            }
            return value;
        }
        if ("DIVIDIR".equals(function)) {
            if (args.length < 2) {
                throw new IllegalArgumentException("DIVIDIR requiere al menos 2 argumentos");
            }
            double value = resolveNumericToken(args[0]);
            for (int i = 1; i < args.length; i++) {
                double divisor = resolveNumericToken(args[i]);
                if (Math.abs(divisor) < 0.0000001) {
                    throw new IllegalArgumentException("division por cero");
                }
                value /= divisor;
            }
            return value;
        }
        if ("CONTAR".equals(function)) {
            int count = 0;
            for (String arg : args) {
                count += countNumericTokenOrRange(arg);
            }
            return count;
        }
        throw new IllegalArgumentException("funcion no soportada: " + function);
    }

    private double sumTokenOrRange(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.contains(":")) {
            List<Double> values = collectRangeNumericValues(normalized);
            double sum = 0.0;
            for (Double value : values) {
                sum += value;
            }
            return sum;
        }
        return resolveNumericToken(normalized);
    }

    private int countNumericTokenOrRange(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.contains(":")) {
            return collectRangeNumericValues(normalized).size();
        }
        resolveNumericToken(normalized);
        return 1;
    }

    private double resolveNumericToken(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("argumento vacio");
        }
        if (normalized.matches("^[A-Za-z]+\\d+$")) {
            Object raw = getCellValueByReference(normalized);
            Double parsed = raw == null ? null : SpreadsheetStats.tryParseDouble(raw.toString());
            if (parsed == null) {
                throw new IllegalArgumentException(
                    "la celda " + normalized.toUpperCase(Locale.ROOT) + " no es numerica"
                );
            }
            return parsed;
        }
        Double parsed = SpreadsheetStats.tryParseDouble(normalized);
        if (parsed == null) {
            throw new IllegalArgumentException("valor no numerico: " + normalized);
        }
        return parsed;
    }

    private Object getCellValueByReference(String reference) {
        int[] rc = parseCellReference(reference);
        return tableModel.getValueAt(rc[0], rc[1]);
    }

    private int[] parseCellReference(String reference) {
        String normalized = reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("^([A-Z]+)(\\d+)$").matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("referencia invalida: " + reference);
        }
        int col = columnLettersToIndex(matcher.group(1));
        int row = Integer.parseInt(matcher.group(2)) - 1;
        if (row < 0 || row >= tableModel.getRowCount() || col < 0 || col >= tableModel.getColumnCount()) {
            throw new IllegalArgumentException("referencia fuera de rango: " + reference);
        }
        return new int[]{row, col};
    }

    private List<Double> collectRangeNumericValues(String rangeToken) {
        String[] parts = rangeToken.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("rango invalido: " + rangeToken);
        }
        int[] start = parseCellReference(parts[0].trim());
        int[] end = parseCellReference(parts[1].trim());
        int rowStart = Math.min(start[0], end[0]);
        int rowEnd = Math.max(start[0], end[0]);
        int colStart = Math.min(start[1], end[1]);
        int colEnd = Math.max(start[1], end[1]);

        List<Double> values = new ArrayList<Double>();
        for (int row = rowStart; row <= rowEnd; row++) {
            for (int col = colStart; col <= colEnd; col++) {
                Object raw = tableModel.getValueAt(row, col);
                Double parsed = raw == null ? null : SpreadsheetStats.tryParseDouble(raw.toString());
                if (parsed != null) {
                    values.add(parsed);
                }
            }
        }
        return values;
    }

    private int columnLettersToIndex(String letters) {
        int value = 0;
        for (int i = 0; i < letters.length(); i++) {
            char ch = letters.charAt(i);
            value = value * 26 + (ch - 'A' + 1);
        }
        return value - 1;
    }

    private String formatFormulaNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return String.format(Locale.US, "%.0f", value);
        }
        String text = String.format(Locale.US, "%.4f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void showUserManual() {
        JOptionPane.showMessageDialog(
            this,
            "Manual rapido de uso\n\n"
                + "Atajos principales:\n"
                + "- Ctrl+N: nueva fila\n"
                + "- Delete: eliminar fila\n"
                + "- Ctrl+Z / Ctrl+Y: deshacer / rehacer\n"
                + "- Ctrl+S: guardar CSV tabla\n"
                + "- Ctrl+Shift+S: exportar CSV visible\n"
                + "- Ctrl+O: cargar CSV\n"
                + "- Ctrl+E: exportar HTML\n"
                + "- Ctrl+F: enfocar buscador\n"
                + "- F1: abrir manual\n"
                + "- F2: renombrar hoja\n"
                + "- Ctrl+D: abrir dashboard web\n\n"
                + "Funciones destacadas:\n"
                + "- Renombrar hoja/columnas y personalizar colores.\n"
                + "- Plantillas de exportacion y versionado automatico (_v2, _v3...).\n"
                + "- Formula en celda: escribe '=...' en 'Celda actual' y aplica.\n"
                + "- Usa la guia de formulas para ejemplos detallados.",
            "Manual de usuario",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showFormulasGuide() {
        JOptionPane.showMessageDialog(
            this,
            "Guia de formulas\n\n"
                + "Sintaxis:\n"
                + "- =FUNCION(arg1,arg2,...)\n"
                + "- Separador: coma o punto y coma.\n"
                + "- Referencias: A1, B2, C10...\n"
                + "- Rangos: A1:C3 (en SUMAR y CONTAR).\n\n"
                + "Funciones disponibles:\n"
                + "- SUMAR(A1,B1,10)\n"
                + "- SUMAR(A1:C3)\n"
                + "- RESTAR(A1,B1,5)\n"
                + "- MULTIPLICAR(A1,B1,2)\n"
                + "- DIVIDIR(A1,B1)\n"
                + "- CONTAR(A1:C3)\n\n"
                + "Comportamiento numerico:\n"
                + "- Las operaciones numericas requieren celdas con numero.\n"
                + "- Si una referencia no es numerica, se muestra error.\n"
                + "- CONTAR solo cuenta celdas numericas.",
            "Guia de formulas",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void undoHistory() {
        if (!history.canUndo()) {
            JOptionPane.showMessageDialog(this, "No hay acciones para deshacer.");
            return;
        }
        executeHistoryOperation(() -> history.undo(tableModel));
        applyCombinedFilter();
        changeLog.record("Deshacer", null);
    }

    private void redoHistory() {
        if (!history.canRedo()) {
            JOptionPane.showMessageDialog(this, "No hay acciones para rehacer.");
            return;
        }
        executeHistoryOperation(() -> history.redo(tableModel));
        applyCombinedFilter();
        changeLog.record("Rehacer", null);
    }

    private void executeHistoryOperation(Runnable action) {
        historyOperationInProgress = true;
        try {
            action.run();
        } finally {
            historyOperationInProgress = false;
        }
    }

    private void applyTheme() {
        Color base = customBaseBackground;
        Color text = customTextColor;
        Color header = new Color(245, 247, 250);
        Color panelBackground = TOOLBAR_BACKGROUND;
        Color buttonBackground = BUTTON_BACKGROUND;

        getContentPane().setBackground(base);
        for (JTable t : dataTables) {
            t.setBackground(base);
            t.setForeground(text);
            t.setFillsViewportHeight(true);
            t.setShowGrid(true);
            t.setShowHorizontalLines(true);
            t.setShowVerticalLines(true);
            t.setIntercellSpacing(new Dimension(1, 1));
            t.setGridColor(new Color(150, 168, 195));
            t.setSelectionBackground(new Color(184, 207, 229));
            t.setSelectionForeground(Color.WHITE);
            t.getTableHeader().setBackground(header);
            t.getTableHeader().setForeground(text);
        }
        resultLabel.setForeground(text);
        filterLabel.setForeground(text);
        totalsLabel.setForeground(text);
        if (validationLabel != null) {
            validationLabel.setForeground(text);
        }
        kpiRowsLabel.setForeground(text);
        kpiSumLabel.setForeground(text);
        kpiAvgLabel.setForeground(text);

        applyThemeToPanel(topPanel, panelBackground, buttonBackground, text);
        applyThemeToPanel(bottomPanel, panelBackground, buttonBackground, text);
        if (getJMenuBar() != null) {
            getJMenuBar().setOpaque(true);
            getJMenuBar().setBackground(panelBackground);
            getJMenuBar().setForeground(text);
            for (int i = 0; i < getJMenuBar().getMenuCount(); i++) {
                JMenu menu = getJMenuBar().getMenu(i);
                if (menu != null) {
                    menu.setForeground(text);
                    menu.setBackground(panelBackground);
                }
            }
        }
        repaint();
    }

    private void promptRenameSheetTitle() {
        String currentSheetTitle = extractSheetTitle(getTitle());
        String input = JOptionPane.showInputDialog(this, "Nuevo titulo de la hoja:", currentSheetTitle);
        if (input == null) {
            return;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El titulo no puede estar vacio.");
            return;
        }
        applySheetTitle(trimmed);
    }

    private void applySheetTitleWithDateTime() {
        String base = extractSheetTitle(getTitle());
        String timestamp = LocalDateTime.now().format(TITLE_DATETIME_FORMAT);
        applySheetTitle(base + " - " + timestamp);
    }

    private void resetSheetTitleToDefault() {
        applySheetTitle(DEFAULT_SHEET_TITLE);
    }

    private void applySheetTitle(String rawTitle) {
        String normalized = rawTitle == null ? "" : rawTitle.trim();
        if (normalized.isEmpty()) {
            normalized = DEFAULT_SHEET_TITLE;
        }
        String previous = extractSheetTitle(getTitle());
        setTitle(WINDOW_TITLE_PREFIX + normalized);
        saveSheetTitle(normalized);
        if (changeLog != null && !normalized.equals(previous)) {
            changeLog.record("Renombrar hoja", "'" + previous + "' -> '" + normalized + "'");
        }
        if (dbAutoSync != null) {
            dbAutoSync.updateSheetName(normalized);
        }
    }

    private String extractSheetTitle(String fullTitle) {
        if (fullTitle != null && fullTitle.startsWith(WINDOW_TITLE_PREFIX)) {
            return fullTitle.substring(WINDOW_TITLE_PREFIX.length());
        }
        return DEFAULT_SHEET_TITLE;
    }

    private String loadSavedSheetTitle() {
        Preferences prefs = Preferences.userNodeForPackage(SpreadsheetFrame.class);
        return prefs.get(PREF_SHEET_TITLE_KEY, DEFAULT_SHEET_TITLE);
    }

    private void saveSheetTitle(String sheetTitle) {
        Preferences prefs = Preferences.userNodeForPackage(SpreadsheetFrame.class);
        prefs.put(PREF_SHEET_TITLE_KEY, sheetTitle);
    }

    private String loadExportNameTemplate() {
        Preferences prefs = Preferences.userNodeForPackage(SpreadsheetFrame.class);
        return prefs.get(PREF_EXPORT_NAME_TEMPLATE_KEY, "titulo_tipo");
    }

    private void saveExportNameTemplate(String template) {
        Preferences prefs = Preferences.userNodeForPackage(SpreadsheetFrame.class);
        prefs.put(PREF_EXPORT_NAME_TEMPLATE_KEY, template);
    }

    private void configureExportNameTemplate() {
        String[] options = new String[]{
            "titulo_tipo",
            "tipo_titulo",
            "titulo_fecha_tipo",
            "fecha_titulo_tipo"
        };
        String current = loadExportNameTemplate();
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Selecciona plantilla para nombres de exportacion:",
            "Plantilla de exportacion",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            current
        );
        if (selected == null) {
            return;
        }
        exportNameTemplate = selected;
        saveExportNameTemplate(selected);
        String preview = buildSheetBasedFileName("tabla", "csv");
        changeLog.record("Plantilla de exportacion", selected);
        JOptionPane.showMessageDialog(this, "Plantilla aplicada. Ejemplo: " + preview);
    }

    private void chooseCustomColors() {
        Color newBase = JColorChooser.showDialog(this, "Color base de la app", customBaseBackground);
        if (newBase != null) {
            customBaseBackground = newBase;
        }
        Color newText = JColorChooser.showDialog(this, "Color de texto principal", customTextColor);
        if (newText != null) {
            customTextColor = newText;
        }
        Color newEven = JColorChooser.showDialog(this, "Color para filas pares (referencia)", colorEvenRow);
        if (newEven != null) {
            colorEvenRow = newEven;
        }
        Color newOdd = JColorChooser.showDialog(this, "Color para filas impares (referencia)", colorOddRow);
        if (newOdd != null) {
            colorOddRow = newOdd;
        }
        applyTheme();
        repaintAllDataTables();
    }

    private void resetAllColors() {
        customBaseBackground = Color.WHITE;
        customTextColor = Color.BLACK;
        colorEvenRow = Color.WHITE;
        colorOddRow = new Color(245, 247, 250);
        applyTheme();
        repaintAllDataTables();
    }

    private void renameColumn(int modelColumn) {
        if (modelColumn < 0 || modelColumn >= tableModel.getColumnCount()) {
            return;
        }
        String currentName = tableModel.getColumnName(modelColumn);
        String proposed = JOptionPane.showInputDialog(this, "Nuevo nombre de columna:", currentName);
        if (proposed == null) {
            return;
        }
        String newName = proposed.trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de columna no puede estar vacio.");
            return;
        }
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != modelColumn && newName.equalsIgnoreCase(tableModel.getColumnName(col))) {
                JOptionPane.showMessageDialog(this, "Ya existe otra columna con ese nombre.");
                return;
            }
        }
        String oldName = currentName;
        executeHistoryOperation(() -> tableModel.setColumnIdentifiers(buildRenamedHeaders(modelColumn, newName)));
        invalidateColumnIndexCache();
        configureTableColumns();
        history.push(tableModel);
        changeLog.record("Renombrar columna", "'" + oldName + "' -> '" + newName + "'");
    }

    private Object[] buildRenamedHeaders(int targetModelColumn, String newName) {
        Object[] headers = new Object[tableModel.getColumnCount()];
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            headers[col] = col == targetModelColumn ? newName : tableModel.getColumnName(col);
        }
        return headers;
    }

    private void applyThemeToPanel(JPanel panel, Color panelBackground, Color buttonBackground, Color textColor) {
        if (panel == null) {
            return;
        }
        panel.setOpaque(true);
        panel.setBackground(panelBackground);
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                button.setBackground(buttonBackground);
                button.setForeground(textColor);
                button.setOpaque(true);
            } else if (component instanceof JComboBox) {
                component.setBackground(Color.WHITE);
                component.setForeground(textColor);
                if (component instanceof JComponent) {
                    ((JComponent) component).setOpaque(true);
                }
            } else if (component instanceof JTextField) {
                component.setBackground(Color.WHITE);
                component.setForeground(textColor);
                if (component instanceof JComponent) {
                    ((JComponent) component).setOpaque(true);
                }
            } else if (component instanceof JLabel) {
                component.setForeground(textColor);
            }
        }
    }

    private class NumericCellEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;

        public NumericCellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean stopCellEditing() {
            Object raw = getCellEditorValue();
            String input = raw == null ? "" : raw.toString();
            Double parsed = SpreadsheetStats.tryParseDouble(input);
            if (parsed == null) {
                JOptionPane.showMessageDialog(SpreadsheetFrame.this, "La columna 'Valor' solo admite datos numericos.");
                return false;
            }
            delegate.setValue(String.format(Locale.US, "%.2f", parsed));
            return super.stopCellEditing();
        }
    }

    private class ClientSuggestCellEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private final JTextField textField;
        private final int targetModelCol;
        private int editingModelRow = -1;

        ClientSuggestCellEditor(int targetModelCol) {
            super(new JTextField());
            this.targetModelCol = targetModelCol;
            this.textField = (JTextField) getComponent();
            this.textField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("control B"), "accept-client-suggestion");
            this.textField.getActionMap().put("accept-client-suggestion", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event) {
                    if (applyPendingClientSuggestionForCell(editingModelRow, targetModelCol)) {
                        Object current = tableModel.getValueAt(editingModelRow, targetModelCol);
                        textField.setText(current == null ? "" : String.valueOf(current));
                    }
                }
            });
            this.textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    updateHint();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    updateHint();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    updateHint();
                }

                private void updateHint() {
                    if (editingModelRow < 0) {
                        return;
                    }
                    updateClientSuggestionHintFromText(editingModelRow, targetModelCol, textField.getText());
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
            editingModelRow = table.convertRowIndexToModel(row);
            updateClientSuggestionHintFromText(editingModelRow, targetModelCol, textField.getText());
            return component;
        }

    }
}
