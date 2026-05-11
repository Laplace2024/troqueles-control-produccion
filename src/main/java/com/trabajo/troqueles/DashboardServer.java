package com.trabajo.troqueles;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardServer {
    private static final Logger LOGGER = Logger.getLogger(DashboardServer.class.getName());
    private static final int STOP_DELAY_SECONDS = 2;

    private final HttpServer server;
    private final ExecutorService executor;
    private final File rootDirectory;
    private final RowProvider rowProvider;
    private final int port;

    public DashboardServer(File rootDirectory, RowProvider rowProvider) throws IOException {
        this.rootDirectory = rootDirectory;
        this.rowProvider = rowProvider;
        this.server = createServerWithAvailablePort();
        this.port = server.getAddress().getPort();
        this.executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "dashboard-http");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        this.server.createContext("/api/rows", new ApiRowsHandler());
        this.server.createContext("/", new StaticFileHandler());
    }

    public void start() {
        server.start();
    }

    public void stop() {
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

    public int getPort() {
        return port;
    }

    private HttpServer createServerWithAvailablePort() throws IOException {
        IOException lastException = null;
        for (int candidate = 8765; candidate <= 8795; candidate++) {
            try {
                return HttpServer.create(new InetSocketAddress("127.0.0.1", candidate), 0);
            } catch (IOException ex) {
                lastException = ex;
            }
        }
        throw new IOException("No se encontro puerto libre para dashboard server.", lastException);
    }

    private void applyCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
    }

    private void handleSafely(HttpExchange exchange, HandlerAction action) throws IOException {
        applyCommonHeaders(exchange);
        try {
            action.run(exchange);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error interno en endpoint dashboard: " + exchange.getRequestURI(), ex);
            if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            }
            String message = "Error interno del servidor";
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
                // respuesta ya enviada o socket cerrado
            }
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface HandlerAction {
        void run(HttpExchange exchange) throws IOException;
    }

    private class ApiRowsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleSafely(exchange, ex -> {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "Metodo no permitido");
                    return;
                }
                ex.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");

                List<RowData> rows = rowProvider.provideRows();
                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < rows.size(); i++) {
                    RowData row = rows.get(i);
                    json.append("{\"concepto\":\"")
                        .append(escapeJson(row.getConcepto()))
                        .append("\",\"valor\":")
                        .append(String.format(Locale.US, "%.2f", row.getValor()))
                        .append(",\"categoria\":\"")
                        .append(escapeJson(row.getCategoria()))
                        .append("\"}");
                    if (i < rows.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]");

                byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            });
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleSafely(exchange, ex -> {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "Metodo no permitido");
                    return;
                }

                String path = ex.getRequestURI().getPath();
                if (path == null || "/".equals(path)) {
                    path = "/index.html";
                }

                String relative = path.startsWith("/") ? path.substring(1) : path;
                if (relative.contains("..")) {
                    sendText(ex, 400, "Ruta invalida");
                    return;
                }

                File file = new File(rootDirectory, relative);
                if (!file.exists() || file.isDirectory()) {
                    sendText(ex, 404, "No encontrado");
                    return;
                }

                byte[] body = Files.readAllBytes(file.toPath());
                ex.getResponseHeaders().set("Content-Type", contentType(file.getName()));
                if (isStaticAsset(file.getName())) {
                    ex.getResponseHeaders().set("Cache-Control", "private, max-age=60");
                } else {
                    ex.getResponseHeaders().set("Cache-Control", "no-store");
                }
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            });
        }
    }

    private boolean isStaticAsset(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".js") || lower.endsWith(".css") || lower.endsWith(".png")
            || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
            || lower.endsWith(".svg") || lower.endsWith(".woff") || lower.endsWith(".woff2");
    }

    private void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
    }

    private String contentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (lower.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }

    public interface RowProvider {
        List<RowData> provideRows();
    }

    public static class RowData {
        private final String concepto;
        private final double valor;
        private final String categoria;

        public RowData(String concepto, double valor, String categoria) {
            this.concepto = concepto;
            this.valor = valor;
            this.categoria = categoria;
        }

        public String getConcepto() {
            return concepto;
        }

        public double getValor() {
            return valor;
        }

        public String getCategoria() {
            return categoria;
        }
    }
}
