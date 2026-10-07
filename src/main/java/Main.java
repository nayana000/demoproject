import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {

        int port = getPort();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        // Application endpoints
        server.createContext("/", Main::handleHome);
        server.createContext("/hello", Main::handleHello);
        server.createContext("/health", Main::handleHealth);
        server.createContext("/info", Main::handleInfo);

        // Use a thread pool to handle requests
        server.setExecutor(
                Executors.newFixedThreadPool(10)
        );

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("Shutting down application...");
                    server.stop(2);
                    System.out.println("Application stopped.");
                })
        );

        server.start();

        System.out.println("----------------------------------------");
        System.out.println("Java Demo Application Started");
        System.out.println("Port        : " + port);
        System.out.println("Environment : " +
                System.getenv().getOrDefault(
                        "APP_ENV",
                        "local"
                ));
        System.out.println("----------------------------------------");
    }

    /**
     * Get application port from environment variable.
     *
     * Example:
     * PORT=9090
     *
     * If PORT is not provided, application uses 8080.
     */
    private static int getPort() {

        String port = System.getenv("PORT");

        if (port == null || port.isBlank()) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException exception) {

            System.out.println(
                    "Invalid PORT value. Using default port "
                            + DEFAULT_PORT
            );

            return DEFAULT_PORT;
        }
    }

    /**
     * Home endpoint.
     *
     * GET /
     */
    private static void handleHome(
            HttpExchange exchange
    ) throws IOException {

        if (!isGetRequest(exchange)) {
            return;
        }

        String response = """
                <!doctype html>
                <html>
                <head>
                    <title>Java CI/CD Demo</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 40px;
                        }

                        h1 {
                            color: #2c3e50;
                        }

                        li {
                            margin: 10px 0;
                        }

                        a {
                            text-decoration: none;
                            color: #3498db;
                        }
                    </style>
                </head>

                <body>

                    <h1>Java CI/CD Demo Application</h1>

                    <p>
                        Application is running successfully.
                    </p>

                    <h3>Available Endpoints</h3>

                    <ul>
                        <li>
                            <a href="/hello">
                                /hello
                            </a>
                        </li>

                        <li>
                            <a href="/health">
                                /health
                            </a>
                        </li>

                        <li>
                            <a href="/info">
                                /info
                            </a>
                        </li>
                    </ul>

                </body>
                </html>
                """;

        sendResponse(
                exchange,
                200,
                "text/html; charset=UTF-8",
                response
        );
    }

    /**
     * Hello endpoint.
     *
     * GET /hello
     */
    private static void handleHello(
            HttpExchange exchange
    ) throws IOException {

        if (!isGetRequest(exchange)) {
            return;
        }

        String response = """
                {
                    "message": "Hello from Java CI/CD Demo!",
                    "status": "success"
                }
                """;

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response
        );
    }

    /**
     * Health endpoint.
     *
     * GET /health
     *
     * This endpoint can be used by:
     *
     * - Jenkins
     * - Docker health checks
     * - Load balancers
     * - Monitoring systems
     */
    private static void handleHealth(
            HttpExchange exchange
    ) throws IOException {

        if (!isGetRequest(exchange)) {
            return;
        }

        String response = """
                {
                    "status": "UP"
                }
                """;

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response
        );
    }

    /**
     * Application information endpoint.
     *
     * GET /info
     */
    private static void handleInfo(
            HttpExchange exchange
    ) throws IOException {

        if (!isGetRequest(exchange)) {
            return;
        }

        String applicationName = "demoproject";

        String version = "1.0.0";

        String environment =
                System.getenv().getOrDefault(
                        "APP_ENV",
                        "local"
                );

        String response = """
                {
                    "application": "%s",
                    "version": "%s",
                    "environment": "%s",
                    "java_version": "%s"
                }
                """.formatted(
                        applicationName,
                        version,
                        environment,
                        System.getProperty("java.version")
                );

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response
        );
    }

    /**
     * Check HTTP method.
     */
    private static boolean isGetRequest(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            String response = """
                    {
                        "error": "Method Not Allowed"
                    }
                    """;

            sendResponse(
                    exchange,
                    405,
                    "application/json; charset=UTF-8",
                    response
            );

            return false;
        }

        return true;
    }

    /**
     * Send HTTP response.
     */
    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            String response
    ) throws IOException {

        byte[] responseBytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-cache"
        );

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(responseBytes);
        }
    }
}
