package com.trabajo.troqueles;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Arrancador bloqueante del servidor LAN.
 */
final class LanServerLauncher {
    private static final Logger LOGGER = Logger.getLogger(LanServerLauncher.class.getName());
    private static final String ENV_BIND_HOST = "TROQUELES_LAN_BIND_HOST";
    private static final String ENV_START_PORT = "TROQUELES_LAN_PORT";

    private LanServerLauncher() {
    }

    static int run(String[] args) {
        String host = readArg(args, "--host", readEnvOrDefault(ENV_BIND_HOST, "0.0.0.0"));
        int startPort = parsePort(readArg(args, "--port", readEnvOrDefault(ENV_START_PORT, "9010")), 9010);
        DbSettings dbSettings = DbSettings.loadDefault();

        try {
            LanSyncServer server = LanSyncServer.create(host, startPort, dbSettings);
            server.start();

            LOGGER.log(Level.INFO, "Servidor LAN activo en http://{0}:{1}/health",
                new Object[]{server.getBindHost(), server.getPort()});

            CountDownLatch waitForever = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
                waitForever.countDown();
            }, "troqueles-lan-stop"));

            waitForever.await();
            return 0;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo iniciar servidor LAN.", ex);
            return 2;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo conectar o inicializar PostgreSQL.", ex);
            return 3;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return 130;
        }
    }

    private static String readArg(String[] args, String key, String fallback) {
        if (args == null) {
            return fallback;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equalsIgnoreCase(args[i])) {
                String value = args[i + 1];
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return fallback;
    }

    private static String readEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static int parsePort(String raw, int fallback) {
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

