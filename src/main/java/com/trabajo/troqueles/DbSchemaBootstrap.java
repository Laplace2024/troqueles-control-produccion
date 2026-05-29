package com.trabajo.troqueles;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inicializa un esquema base para evolucionar a modo multiusuario.
 *
 * <p>Este bloque no conecta aun la UI con la BD: solo deja preparada la estructura
 * para los siguientes pasos de migracion.</p>
 */
final class DbSchemaBootstrap {
    private DbSchemaBootstrap() {
    }

    static void ensureBaseSchema(Connection connection) throws SQLException {
        execute(connection,
            "CREATE TABLE IF NOT EXISTS app_meta ("
                + "meta_key TEXT PRIMARY KEY,"
                + "meta_value TEXT NOT NULL"
                + ")"
        );
        execute(connection,
            "CREATE TABLE IF NOT EXISTS workbook_sheets ("
                + "sheet_name TEXT PRIMARY KEY,"
                + "header_payload TEXT NOT NULL,"
                + "sheet_version BIGINT NOT NULL DEFAULT 1,"
                + "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                + ")"
        );
        execute(connection,
            "ALTER TABLE workbook_sheets "
                + "ADD COLUMN IF NOT EXISTS sheet_version BIGINT NOT NULL DEFAULT 1"
        );
        execute(connection,
            "CREATE TABLE IF NOT EXISTS workbook_rows ("
                + "id BIGSERIAL PRIMARY KEY,"
                + "sheet_name TEXT NOT NULL,"
                + "row_order INTEGER NOT NULL,"
                + "row_payload_json TEXT NOT NULL,"
                + "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                + ")"
        );
        execute(connection,
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_workbook_rows_sheet_order "
                + "ON workbook_rows(sheet_name, row_order)"
        );
        execute(connection,
            "CREATE TABLE IF NOT EXISTS audit_events ("
                + "id BIGSERIAL PRIMARY KEY,"
                + "event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "event_action TEXT NOT NULL,"
                + "event_detail TEXT NOT NULL,"
                + "worker_name TEXT"
                + ")"
        );
        execute(connection,
            "CREATE TABLE IF NOT EXISTS workbook_client_stats ("
                + "sheet_name TEXT NOT NULL,"
                + "client_label TEXT NOT NULL,"
                + "row_count INTEGER NOT NULL,"
                + "pedidos_count INTEGER NOT NULL,"
                + "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "PRIMARY KEY (sheet_name, client_label)"
                + ")"
        );
        execute(connection,
            "CREATE TABLE IF NOT EXISTS workbook_row_locks ("
                + "sheet_name TEXT NOT NULL,"
                + "row_order INTEGER NOT NULL,"
                + "worker_name TEXT NOT NULL,"
                + "locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                + "expires_at TIMESTAMPTZ NOT NULL,"
                + "PRIMARY KEY (sheet_name, row_order)"
                + ")"
        );
        execute(connection,
            "CREATE INDEX IF NOT EXISTS idx_workbook_row_locks_expires "
                + "ON workbook_row_locks(expires_at)"
        );
        execute(connection,
            "INSERT INTO app_meta(meta_key, meta_value) VALUES ('schema_version', '2') "
                + "ON CONFLICT (meta_key) DO NOTHING"
        );
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}

