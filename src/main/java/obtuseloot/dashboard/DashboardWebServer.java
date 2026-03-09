package obtuseloot.dashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class DashboardWebServer {
    private final Path root;
    private final int port;
    private HttpServer server;

    public DashboardWebServer(Path root, int port) {
        this.root = root;
        this.port = port;
    }

    public void start() throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int port() {
        return port;
    }

    public boolean isRunning() {
        return server != null;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String sanitized = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        if (sanitized.isBlank()) {
            sanitized = "ecosystem-dashboard.html";
        }

        Path file = root.resolve(sanitized).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            byte[] response = "Not Found".getBytes();
            exchange.sendResponseHeaders(404, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
            return;
        }

        byte[] response = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }
}
