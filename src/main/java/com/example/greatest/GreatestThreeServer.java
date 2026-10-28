package com.example.greatest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class GreatestThreeServer {
    private GreatestThreeServer() {
    }

    static int greatest(int first, int second, int third) {
        return Math.max(first, Math.max(second, third));
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "ok"));
        server.createContext("/greatest", GreatestThreeServer::handleGreatest);
        server.start();
        System.out.println("Greatest-of-three server listening on port 8080");
    }

    private static void handleGreatest(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "Only GET is supported");
            return;
        }

        try {
            Map<String, String> parameters = parseQuery(exchange.getRequestURI().getRawQuery());
            int first = Integer.parseInt(parameters.get("a"));
            int second = Integer.parseInt(parameters.get("b"));
            int third = Integer.parseInt(parameters.get("c"));
            respond(exchange, 200, Integer.toString(greatest(first, second, third)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            respond(exchange, 400, "Provide integer query parameters a, b, and c");
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> parameters = new HashMap<>();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query parameters are required");
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                parameters.put(parts[0], parts[1]);
            }
        }
        return parameters;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
