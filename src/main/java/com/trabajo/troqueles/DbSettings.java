package com.trabajo.troqueles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuracion de conexion para PostgreSQL en el despliegue de taller.
 *
 * <p>Orden de prioridad para cada valor:</p>
 * <ol>
 *   <li>Variable de entorno TROQUELES_DB_*</li>
 *   <li>Fichero ~/.troqueles/db.properties</li>
 *   <li>Valor por defecto</li>
 * </ol>
 */
final class DbSettings {
    private static final String ENV_HOST = "TROQUELES_DB_HOST";
    private static final String ENV_PORT = "TROQUELES_DB_PORT";
    private static final String ENV_NAME = "TROQUELES_DB_NAME";
    private static final String ENV_USER = "TROQUELES_DB_USER";
    private static final String ENV_PASS = "TROQUELES_DB_PASS";
    private static final String ENV_SSL = "TROQUELES_DB_SSL";

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final boolean ssl;

    private DbSettings(String host, int port, String database, String user, String password, boolean ssl) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.ssl = ssl;
    }

    static DbSettings loadDefault() {
        Path configPath = Path.of(System.getProperty("user.home"), ".troqueles", "db.properties");
        Properties fileProps = new Properties();
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                fileProps.load(in);
            } catch (IOException ignored) {
                // Si falla la lectura, se mantienen defaults + variables de entorno.
            }
        }

        String host = pick(ENV_HOST, fileProps, "db.host", "127.0.0.1");
        int port = parsePort(pick(ENV_PORT, fileProps, "db.port", "5432"));
        String database = pick(ENV_NAME, fileProps, "db.name", "troqueles");
        String user = pick(ENV_USER, fileProps, "db.user", "troqueles_app");
        String password = pick(ENV_PASS, fileProps, "db.pass", "");
        boolean ssl = parseBoolean(pick(ENV_SSL, fileProps, "db.ssl", "false"));

        return new DbSettings(host, port, database, user, password, ssl);
    }

    String jdbcUrl() {
        String sslMode = ssl ? "require" : "disable";
        return "jdbc:postgresql://" + host + ":" + port + "/" + database
            + "?sslmode=" + sslMode + "&ApplicationName=troqueles-app";
    }

    String user() {
        return user;
    }

    String password() {
        return password;
    }

    private static String pick(String envKey, Properties props, String key, String fallback) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propValue = props.getProperty(key);
        if (propValue != null && !propValue.isBlank()) {
            return propValue.trim();
        }
        return fallback;
    }

    private static int parsePort(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 5432;
        } catch (Exception ignored) {
            return 5432;
        }
    }

    private static boolean parseBoolean(String raw) {
        return Objects.equals("true", raw == null ? null : raw.trim().toLowerCase());
    }
}

