package com.trabajo.troqueles;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.DefaultTableModel;

/**
 * Persistencia de hojas de trabajo en PostgreSQL (fase inicial multiusuario).
 */
final class DbWorkbookRepository {
    private static final String SQL_UPSERT_SHEET_OPTIMISTIC =
        "INSERT INTO workbook_sheets(sheet_name, header_payload, sheet_version, updated_at) "
            + "VALUES (?, ?, 1, NOW()) "
            + "ON CONFLICT (sheet_name) DO UPDATE SET "
            + "header_payload = EXCLUDED.header_payload, "
            + "updated_at = NOW(), "
            + "sheet_version = workbook_sheets.sheet_version + 1 "
            + "WHERE workbook_sheets.sheet_version = ? "
            + "RETURNING sheet_version";
    private static final String SQL_UPSERT_SHEET_FORCE =
        "INSERT INTO workbook_sheets(sheet_name, header_payload, sheet_version, updated_at) "
            + "VALUES (?, ?, 1, NOW()) "
            + "ON CONFLICT (sheet_name) DO UPDATE SET "
            + "header_payload = EXCLUDED.header_payload, "
            + "updated_at = NOW(), "
            + "sheet_version = workbook_sheets.sheet_version + 1 "
            + "RETURNING sheet_version";
    private static final String SQL_DELETE_ROWS =
        "DELETE FROM workbook_rows WHERE sheet_name = ?";
    private static final String SQL_INSERT_ROW =
        "INSERT INTO workbook_rows(sheet_name, row_order, row_payload_json) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_SHEET =
        "SELECT header_payload, sheet_version FROM workbook_sheets WHERE sheet_name = ?";
    private static final String SQL_SELECT_SHEET_VERSION =
        "SELECT sheet_version FROM workbook_sheets WHERE sheet_name = ?";
    private static final String SQL_SELECT_ROWS =
        "SELECT row_payload_json FROM workbook_rows WHERE sheet_name = ? ORDER BY row_order ASC";
    private static final String SQL_INSERT_AUDIT_EVENT =
        "INSERT INTO audit_events(event_action, event_detail, worker_name) VALUES (?, ?, ?)";
    private static final String SQL_DELETE_CLIENT_STATS =
        "DELETE FROM workbook_client_stats WHERE sheet_name = ?";
    private static final String SQL_INSERT_CLIENT_STATS =
        "INSERT INTO workbook_client_stats(sheet_name, client_label, row_count, pedidos_count, updated_at) "
            + "VALUES (?, ?, ?, ?, NOW())";
    private static final String SQL_SELECT_CLIENT_RANKING =
        "SELECT client_label, row_count, pedidos_count FROM workbook_client_stats "
            + "WHERE sheet_name = ? ORDER BY row_count DESC, pedidos_count DESC, client_label ASC";
    private static final String SQL_PURGE_EXPIRED_ROW_LOCKS =
        "DELETE FROM workbook_row_locks WHERE sheet_name = ? AND expires_at < NOW()";
    private static final String SQL_SELECT_ROW_LOCK =
        "SELECT worker_name, expires_at > NOW() AS active FROM workbook_row_locks "
            + "WHERE sheet_name = ? AND row_order = ?";
    private static final String SQL_INSERT_ROW_LOCK =
        "INSERT INTO workbook_row_locks(sheet_name, row_order, worker_name, locked_at, expires_at) "
            + "VALUES (?, ?, ?, NOW(), NOW() + (? * INTERVAL '1 second'))";
    private static final String SQL_RENEW_ROW_LOCK =
        "UPDATE workbook_row_locks SET locked_at = NOW(), expires_at = NOW() + (? * INTERVAL '1 second') "
            + "WHERE sheet_name = ? AND row_order = ? AND worker_name = ?";
    private static final String SQL_TAKE_EXPIRED_ROW_LOCK =
        "UPDATE workbook_row_locks SET worker_name = ?, locked_at = NOW(), "
            + "expires_at = NOW() + (? * INTERVAL '1 second') "
            + "WHERE sheet_name = ? AND row_order = ? AND expires_at < NOW()";
    private static final String SQL_DELETE_ROW_LOCK =
        "DELETE FROM workbook_row_locks WHERE sheet_name = ? AND row_order = ? AND worker_name = ?";
    private static final String SQL_DELETE_WORKER_ROW_LOCKS =
        "DELETE FROM workbook_row_locks WHERE sheet_name = ? AND worker_name = ?";
    private static final String SQL_SELECT_ACTIVE_ROW_LOCKS =
        "SELECT row_order, worker_name FROM workbook_row_locks "
            + "WHERE sheet_name = ? AND expires_at >= NOW() ORDER BY row_order ASC";

    private final DbSettings dbSettings;

    DbWorkbookRepository(DbSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    SaveResult saveSheetOptimistic(
        String sheetName,
        DefaultTableModel model,
        long expectedVersion,
        String workerName
    ) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            connection.setAutoCommit(false);
            try {
                DbSchemaBootstrap.ensureBaseSchema(connection);
                List<String> headers = extractHeaders(model);
                Long newVersion = upsertSheetOptimistic(connection, sheetName, encodeCells(headers), expectedVersion);
                if (newVersion == null) {
                    connection.rollback();
                    long currentVersion = selectCurrentVersion(connection, sheetName);
                    return SaveResult.conflict(currentVersion);
                }
                deleteRows(connection, sheetName);
                insertRows(connection, sheetName, model);
                replaceClientStats(connection, sheetName, model);
                insertAuditEvent(
                    connection,
                    "save_sheet",
                    "sheet=" + sheetName + ", version=" + newVersion + ", rows=" + model.getRowCount(),
                    workerName
                );
                connection.commit();
                return SaveResult.saved(newVersion.longValue());
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    long saveSheetForce(String sheetName, DefaultTableModel model, String workerName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            connection.setAutoCommit(false);
            try {
                DbSchemaBootstrap.ensureBaseSchema(connection);
                List<String> headers = extractHeaders(model);
                long newVersion = upsertSheetForce(connection, sheetName, encodeCells(headers));
                deleteRows(connection, sheetName);
                insertRows(connection, sheetName, model);
                replaceClientStats(connection, sheetName, model);
                insertAuditEvent(
                    connection,
                    "force_save_sheet",
                    "sheet=" + sheetName + ", version=" + newVersion + ", rows=" + model.getRowCount(),
                    workerName
                );
                connection.commit();
                return newVersion;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    WorkbookData loadSheet(String sheetName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);

            SheetSnapshot snapshot = selectSheetSnapshot(connection, sheetName);
            if (snapshot == null) {
                return null;
            }
            List<String> headers = decodeCells(snapshot.headerPayload);
            List<Object[]> rows = selectRows(connection, sheetName, headers.size());
            return new WorkbookData(headers, rows, snapshot.version);
        }
    }

    SheetVersionSnapshot fetchSheetVersionSnapshot(String sheetName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_SHEET_VERSION)) {
                st.setString(1, sheetName);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        return SheetVersionSnapshot.missing();
                    }
                    return SheetVersionSnapshot.present(rs.getLong(1));
                }
            }
        }
    }

    RowLockAcquireResult tryAcquireRowLock(
        String sheetName,
        int rowOrder,
        String workerName,
        long ttlSeconds
    ) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            purgeExpiredRowLocks(connection, sheetName);
            RowLockState current = selectRowLockState(connection, sheetName, rowOrder);
            String normalizedWorker = workerName == null ? "" : workerName.trim();
            if (current == null) {
                insertRowLock(connection, sheetName, rowOrder, normalizedWorker, ttlSeconds);
                return RowLockAcquireResult.ACQUIRED;
            }
            if (normalizedWorker.equals(current.workerName)) {
                renewRowLock(connection, sheetName, rowOrder, normalizedWorker, ttlSeconds);
                return RowLockAcquireResult.RENEWED;
            }
            if (!current.active) {
                takeExpiredRowLock(connection, sheetName, rowOrder, normalizedWorker, ttlSeconds);
                return RowLockAcquireResult.ACQUIRED;
            }
            return RowLockAcquireResult.BLOCKED;
        }
    }

    String findRowLockHolder(String sheetName, int rowOrder) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            purgeExpiredRowLocks(connection, sheetName);
            RowLockState current = selectRowLockState(connection, sheetName, rowOrder);
            if (current == null || !current.active) {
                return "";
            }
            return current.workerName;
        }
    }

    void releaseRowLock(String sheetName, int rowOrder, String workerName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            try (PreparedStatement st = connection.prepareStatement(SQL_DELETE_ROW_LOCK)) {
                st.setString(1, sheetName);
                st.setInt(2, rowOrder);
                st.setString(3, workerName == null ? "" : workerName.trim());
                st.executeUpdate();
            }
        }
    }

    void releaseAllRowLocksForWorker(String sheetName, String workerName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            try (PreparedStatement st = connection.prepareStatement(SQL_DELETE_WORKER_ROW_LOCKS)) {
                st.setString(1, sheetName);
                st.setString(2, workerName == null ? "" : workerName.trim());
                st.executeUpdate();
            }
        }
    }

    Map<Integer, String> loadActiveRowLocks(String sheetName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            purgeExpiredRowLocks(connection, sheetName);
            Map<Integer, String> locks = new HashMap<Integer, String>();
            try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_ACTIVE_ROW_LOCKS)) {
                st.setString(1, sheetName);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        locks.put(Integer.valueOf(rs.getInt(1)), rs.getString(2));
                    }
                }
            }
            return locks;
        }
    }

    List<ClientRankingEntry> loadClientRanking(String sheetName) throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
            List<ClientRankingEntry> result = new ArrayList<ClientRankingEntry>();
            try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_CLIENT_RANKING)) {
                st.setString(1, sheetName);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        result.add(new ClientRankingEntry(
                            rs.getString(1),
                            rs.getInt(2),
                            rs.getInt(3)
                        ));
                    }
                }
            }
            return result;
        }
    }

    private Long upsertSheetOptimistic(
        Connection connection,
        String sheetName,
        String headerPayload,
        long expectedVersion
    ) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_UPSERT_SHEET_OPTIMISTIC)) {
            st.setString(1, sheetName);
            st.setString(2, headerPayload);
            st.setLong(3, expectedVersion);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return Long.valueOf(rs.getLong(1));
            }
        }
    }

    private void purgeExpiredRowLocks(Connection connection, String sheetName) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_PURGE_EXPIRED_ROW_LOCKS)) {
            st.setString(1, sheetName);
            st.executeUpdate();
        }
    }

    private RowLockState selectRowLockState(Connection connection, String sheetName, int rowOrder) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_ROW_LOCK)) {
            st.setString(1, sheetName);
            st.setInt(2, rowOrder);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new RowLockState(rs.getString(1), rs.getBoolean(2));
            }
        }
    }

    private void insertRowLock(
        Connection connection,
        String sheetName,
        int rowOrder,
        String workerName,
        long ttlSeconds
    ) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_INSERT_ROW_LOCK)) {
            st.setString(1, sheetName);
            st.setInt(2, rowOrder);
            st.setString(3, workerName);
            st.setLong(4, ttlSeconds);
            st.executeUpdate();
        }
    }

    private void renewRowLock(
        Connection connection,
        String sheetName,
        int rowOrder,
        String workerName,
        long ttlSeconds
    ) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_RENEW_ROW_LOCK)) {
            st.setLong(1, ttlSeconds);
            st.setString(2, sheetName);
            st.setInt(3, rowOrder);
            st.setString(4, workerName);
            st.executeUpdate();
        }
    }

    private void takeExpiredRowLock(
        Connection connection,
        String sheetName,
        int rowOrder,
        String workerName,
        long ttlSeconds
    ) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_TAKE_EXPIRED_ROW_LOCK)) {
            st.setString(1, workerName);
            st.setLong(2, ttlSeconds);
            st.setString(3, sheetName);
            st.setInt(4, rowOrder);
            st.executeUpdate();
        }
    }

    private long upsertSheetForce(Connection connection, String sheetName, String headerPayload) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_UPSERT_SHEET_FORCE)) {
            st.setString(1, sheetName);
            st.setString(2, headerPayload);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void deleteRows(Connection connection, String sheetName) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_DELETE_ROWS)) {
            st.setString(1, sheetName);
            st.executeUpdate();
        }
    }

    private void insertRows(Connection connection, String sheetName, DefaultTableModel model) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_INSERT_ROW)) {
            for (int row = 0; row < model.getRowCount(); row++) {
                st.setString(1, sheetName);
                st.setInt(2, row);
                st.setString(3, encodeCells(extractRow(model, row)));
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    private SheetSnapshot selectSheetSnapshot(Connection connection, String sheetName) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_SHEET)) {
            st.setString(1, sheetName);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new SheetSnapshot(rs.getString(1), rs.getLong(2));
            }
        }
    }

    private long selectCurrentVersion(Connection connection, String sheetName) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_SHEET_VERSION)) {
            st.setString(1, sheetName);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return 0L;
                }
                return rs.getLong(1);
            }
        }
    }

    private void insertAuditEvent(Connection connection, String action, String detail, String workerName)
        throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SQL_INSERT_AUDIT_EVENT)) {
            st.setString(1, action);
            st.setString(2, detail == null ? "" : detail);
            st.setString(3, workerName == null ? "" : workerName);
            st.executeUpdate();
        }
    }

    private void replaceClientStats(Connection connection, String sheetName, DefaultTableModel model) throws SQLException {
        Map<String, ClientAccumulator> byClient = buildClientStats(model);
        try (PreparedStatement delete = connection.prepareStatement(SQL_DELETE_CLIENT_STATS)) {
            delete.setString(1, sheetName);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(SQL_INSERT_CLIENT_STATS)) {
            for (Map.Entry<String, ClientAccumulator> entry : byClient.entrySet()) {
                insert.setString(1, sheetName);
                insert.setString(2, entry.getKey());
                insert.setInt(3, entry.getValue().rowCount);
                insert.setInt(4, entry.getValue().pedidos.size());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private Map<String, ClientAccumulator> buildClientStats(DefaultTableModel model) {
        Map<String, ClientAccumulator> byClient = new HashMap<String, ClientAccumulator>();
        int idxNombre = columnIndex(model, "Nombre");
        int idxCliente = columnIndex(model, "Cod. cliente");
        int idxPedido = columnIndex(model, "Nº");
        for (int row = 0; row < model.getRowCount(); row++) {
            String nombre = idxNombre >= 0 ? stringValue(model.getValueAt(row, idxNombre)).trim() : "";
            String codigo = idxCliente >= 0 ? stringValue(model.getValueAt(row, idxCliente)).trim() : "";
            String key = !nombre.isEmpty() ? nombre : (!codigo.isEmpty() ? "COD " + codigo : "(Sin cliente)");
            ClientAccumulator acc = byClient.get(key);
            if (acc == null) {
                acc = new ClientAccumulator();
                byClient.put(key, acc);
            }
            acc.rowCount++;
            if (idxPedido >= 0) {
                String pedido = stringValue(model.getValueAt(row, idxPedido)).trim();
                if (!pedido.isEmpty()) {
                    acc.pedidos.add(pedido);
                }
            }
        }
        return byClient;
    }

    private int columnIndex(DefaultTableModel model, String columnName) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equals(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<Object[]> selectRows(Connection connection, String sheetName, int columns) throws SQLException {
        List<Object[]> rows = new ArrayList<Object[]>();
        try (PreparedStatement st = connection.prepareStatement(SQL_SELECT_ROWS)) {
            st.setString(1, sheetName);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    List<String> decoded = decodeCells(rs.getString(1));
                    Object[] row = new Object[Math.max(0, columns)];
                    for (int i = 0; i < row.length; i++) {
                        row[i] = i < decoded.size() ? decoded.get(i) : "";
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private List<String> extractHeaders(DefaultTableModel model) {
        List<String> headers = new ArrayList<String>();
        for (int col = 0; col < model.getColumnCount(); col++) {
            headers.add(stringValue(model.getColumnName(col)));
        }
        return headers;
    }

    private List<String> extractRow(DefaultTableModel model, int row) {
        List<String> values = new ArrayList<String>();
        for (int col = 0; col < model.getColumnCount(); col++) {
            values.add(formatCellForPersistence(model.getValueAt(row, col)));
        }
        return values;
    }

    private String formatCellForPersistence(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "terminado" : "no terminado";
        }
        return stringValue(value);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String encodeCells(List<String> cells) {
        if (cells == null || cells.isEmpty()) {
            return "";
        }
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String raw = cells.get(i) == null ? "" : cells.get(i);
            String encoded = encoder.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            if (i > 0) {
                sb.append('.');
            }
            sb.append(encoded);
        }
        return sb.toString();
    }

    private static List<String> decodeCells(String payload) {
        List<String> result = new ArrayList<String>();
        if (payload == null || payload.isEmpty()) {
            return result;
        }
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] parts = payload.split("\\.");
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                result.add("");
                continue;
            }
            try {
                byte[] raw = decoder.decode(part);
                result.add(new String(raw, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ex) {
                // Fallback conservador para datos legacy o dañados.
                result.add(part);
            }
        }
        return result;
    }

    static final class WorkbookData {
        private final List<String> headers;
        private final List<Object[]> rows;
        private final long version;

        WorkbookData(List<String> headers, List<Object[]> rows, long version) {
            this.headers = headers;
            this.rows = rows;
            this.version = version;
        }

        List<String> headers() {
            return headers;
        }

        List<Object[]> rows() {
            return rows;
        }

        long version() {
            return version;
        }
    }

    static final class SheetVersionSnapshot {
        private final boolean exists;
        private final long version;

        private SheetVersionSnapshot(boolean exists, long version) {
            this.exists = exists;
            this.version = version;
        }

        static SheetVersionSnapshot missing() {
            return new SheetVersionSnapshot(false, 0L);
        }

        static SheetVersionSnapshot present(long version) {
            return new SheetVersionSnapshot(true, version);
        }

        boolean exists() {
            return exists;
        }

        long version() {
            return version;
        }
    }

    enum RowLockAcquireResult {
        ACQUIRED,
        RENEWED,
        BLOCKED
    }

    static final class ClientRankingEntry {
        private final String clientLabel;
        private final int rowCount;
        private final int pedidosCount;

        ClientRankingEntry(String clientLabel, int rowCount, int pedidosCount) {
            this.clientLabel = clientLabel;
            this.rowCount = rowCount;
            this.pedidosCount = pedidosCount;
        }

        String clientLabel() {
            return clientLabel;
        }

        int rowCount() {
            return rowCount;
        }

        int pedidosCount() {
            return pedidosCount;
        }
    }

    static final class SaveResult {
        private final boolean conflict;
        private final long version;

        private SaveResult(boolean conflict, long version) {
            this.conflict = conflict;
            this.version = version;
        }

        static SaveResult saved(long version) {
            return new SaveResult(false, version);
        }

        static SaveResult conflict(long currentVersion) {
            return new SaveResult(true, currentVersion);
        }

        boolean isConflict() {
            return conflict;
        }

        long version() {
            return version;
        }
    }

    private static final class SheetSnapshot {
        private final String headerPayload;
        private final long version;

        private SheetSnapshot(String headerPayload, long version) {
            this.headerPayload = headerPayload;
            this.version = version;
        }
    }

    private static final class RowLockState {
        private final String workerName;
        private final boolean active;

        private RowLockState(String workerName, boolean active) {
            this.workerName = workerName == null ? "" : workerName;
            this.active = active;
        }
    }

    private static final class ClientAccumulator {
        private int rowCount;
        private final Set<String> pedidos = new HashSet<String>();
    }
}

