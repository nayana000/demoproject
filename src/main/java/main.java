package com.example.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HelloApplication {

    public static void main(String[] args) throws IOException {

        int port = 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        server.createContext("/", HelloApplication::handleRequest);
        server.createContext("/hello", HelloApplication::handleRequest);

        server.start();

        System.out.println("Application started on port " + port);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {

        String response = """
                <!doctype html>
                <html>
                <head>
                    <title>Java Demo</title>
                </head>
                <body>
                    <h1>Hello from standalone Java application!</h1>
                    <p>Path: %s</p>
                </body>
                </html>
                """.formatted(exchange.getRequestURI().getPath());

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response.getBytes());
        }
    }
}
