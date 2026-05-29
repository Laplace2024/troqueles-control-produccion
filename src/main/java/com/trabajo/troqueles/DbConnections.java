package com.trabajo.troqueles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fabrica minima de conexiones JDBC para bloque 1 de migracion a multiusuario.
 */
final class DbConnections {
    private static final Logger LOGGER = Logger.getLogger(DbConnections.class.getName());

    private DbConnections() {
    }

    static Connection open(DbSettings settings) throws SQLException {
        Connection connection = DriverManager.getConnection(
            settings.jdbcUrl(),
            settings.user(),
            settings.password()
        );
        LOGGER.log(Level.FINE, "Conexion PostgreSQL abierta.");
        return connection;
    }
}

