package com.trabajo.troqueles;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor LAN base para bloque 2 de multiusuario.
 *
 * <p>En esta fase ofrece endpoints de salud y bootstrap de esquema para
 * validar conectividad entre puestos y PostgreSQL central.</p>
 */
final class LanSyncServer {
    private static final Logger LOGGER = Logger.getLogger(LanSyncServer.class.getName());
    private static final int STOP_DELAY_SECONDS = 2;

    private final HttpServer server;
    private final ExecutorService executor;
    private final DbSettings dbSettings;
    private final String bindHost;
    private final int port;

    private LanSyncServer(HttpServer server, DbSettings dbSettings, String bindHost) {
        this.server = server;
        this.dbSettings = dbSettings;
        this.bindHost = bindHost;
        this.port = server.getAddress().getPort();
        this.executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "troqueles-lan-http");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        this.server.createContext("/health", new HealthHandler());
        this.server.createContext("/bootstrap", new BootstrapHandler());
    }

    static LanSyncServer create(String host, int startPort, DbSettings dbSettings) throws IOException {
        IOException lastException = null;
        for (int candidate = startPort; candidate <= startPort + 30; candidate++) {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(host, candidate), 0);
                return new LanSyncServer(server, dbSettings, host);
            } catch (IOException ex) {
                lastException = ex;
            }
        }
        throw new IOException("No se encontro puerto LAN libre para el servidor.", lastException);
    }

    void start() throws SQLException {
        ensureSchema();
        server.start();
    }

    void stop() {
        server.stop(STOP_DELAY_SECONDS);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    int getPort() {
        return port;
    }

    String getBindHost() {
        return bindHost;
    }

    private void ensureSchema() throws SQLException {
        try (Connection connection = DbConnections.open(dbSettings)) {
            DbSchemaBootstrap.ensureBaseSchema(connection);
        }
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
    }

    private void handleSafely(HttpExchange exchange, HandlerAction action) throws IOException {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        try {
            action.run(exchange);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error interno en endpoint LAN: " + exchange.getRequestURI(), ex);
            sendText(exchange, 500, "{\"status\":\"error\",\"message\":\"error interno\"}");
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface HandlerAction {
        void run(HttpExchange exchange) throws Exception;
    }

    private final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleSafely(exchange, ex -> {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "{\"status\":\"error\",\"message\":\"metodo no permitido\"}");
                    return;
                }
                String payload = "{\"status\":\"ok\",\"service\":\"troqueles-lan\",\"time\":\""
                    + Instant.now().toString()
                    + "\"}";
                sendText(ex, 200, payload);
            });
        }
    }

    private final class BootstrapHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleSafely(exchange, ex -> {
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "{\"status\":\"error\",\"message\":\"metodo no permitido\"}");
                    return;
                }
                try {
                    ensureSchema();
                    sendText(ex, 200, "{\"status\":\"ok\",\"message\":\"schema listo\"}");
                } catch (SQLException sqlEx) {
                    LOGGER.log(Level.WARNING, "Error bootstrap schema PostgreSQL", sqlEx);
                    sendText(ex, 500, "{\"status\":\"error\",\"message\":\"error en bootstrap\"}");
                }
            });
        }
    }
}

