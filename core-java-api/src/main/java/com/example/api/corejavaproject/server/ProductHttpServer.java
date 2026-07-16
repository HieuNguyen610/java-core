package com.example.api.corejavaproject.server;

import com.example.api.corejavaproject.model.Product;
import com.example.api.corejavaproject.service.ProductService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP Server - thay thế Quarkus/Spring Boot embedded server
 *
 * Trong Quarkus/Spring: chỉ cần @Path, @GET annotation là xong
 * Trong Core Java: phải tự parse HTTP request, handle routing, serialize JSON
 */
public class ProductHttpServer {

    private final HttpServer server;
    private final ProductService productService;

    public ProductHttpServer(int port) throws IOException {
        this.productService = new ProductService();

        // Khởi tạo HttpServer - thay vì dùng @ApplicationPath
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Đăng ký routes thủ công - thay vì @Path annotation
        server.createContext("/api/products", new ProductHandler());
        server.createContext("/api/products/search", new SearchHandler());

        server.setExecutor(null); // default executor
    }

    public void start() {
        server.start();
        System.out.println("🚀 Core Java API Server started on http://localhost:" + server.getAddress().getPort());
        System.out.println("📍 Endpoints:");
        System.out.println("   GET    /api/products        - List all products");
        System.out.println("   GET    /api/products/{id}    - Get product by ID");
        System.out.println("   POST   /api/products         - Create product");
        System.out.println("   PUT    /api/products/{id}    - Update product");
        System.out.println("   DELETE /api/products/{id}    - Delete product");
        System.out.println("   GET    /api/products/search  - Search by name");
    }

    public void stop() {
        server.stop(0);
    }

    /**
     * Inner class Handler - thay vì @GET, @POST annotation
     */
    private class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String response;

            try {
                switch (method) {
                    case "GET" -> {
                        // Parse ID từ path: /api/products/123
                        String pathInfo = path.replace("/api/products", "");
                        if (pathInfo.isEmpty() || pathInfo.equals("/")) {
                            response = handleGetAll();
                        } else {
                            int id = Integer.parseInt(pathInfo.substring(1));
                            response = handleGetById(id);
                        }
                        sendResponse(exchange, 200, response);
                    }
                    case "POST" -> {
                        response = handleCreate(exchange);
                        sendResponse(exchange, 201, response);
                    }
                    case "PUT" -> {
                        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                        response = handleUpdate(id, exchange);
                        sendResponse(exchange, 200, response);
                    }
                    case "DELETE" -> {
                        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                        boolean deleted = productService.deleteProduct(id);
                        sendResponse(exchange, deleted ? 204 : 404, "");
                    }
                    default -> sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}");
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            }
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery(); // name=keyword
            if (query == null || !query.startsWith("name=")) {
                sendResponse(exchange, 400, "{\"error\":\"Missing name parameter\"}");
                return;
            }

            String keyword = query.substring(5); // remove "name="
            List<Product> products = productService.searchProducts(keyword);
            sendResponse(exchange, 200, toJson(products));
        }
    }

    // === Các handler methods - thay vì @GetMapping, @PostMapping ===

    private String handleGetAll() {
        List<Product> products = productService.getAllProducts();
        return toJson(products);
    }

    private String handleGetById(int id) {
        return productService.getProduct(id)
                .map(this::toJson)
                .orElse("{\"error\":\"Product not found\"}");
    }

    private String handleCreate(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Product product = fromJson(body);
        Product created = productService.createProduct(product);
        return toJson(created);
    }

    private String handleUpdate(int id, HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Product product = fromJson(body);
        return productService.updateProduct(id, product)
                .map(this::toJson)
                .orElse("{\"error\":\"Product not found\"}");
    }

    // === JSON Utilities - thay vì dùng Jackson/JAXB annotation tự động ===

    private String toJson(Object obj) {
        if (obj instanceof Product p) {
            return String.format(
                "{\"id\":%d,\"name\":\"%s\",\"price\":%.2f,\"description\":\"%s\"}",
                p.getId(), escapeJson(p.getName()), p.getPrice(), escapeJson(p.getDescription())
            );
        } else if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            Iterator<?> it = list.iterator();
            while (it.hasNext()) {
                sb.append(toJson(it.next()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return "{}";
    }

    private Product fromJson(String json) {
        // Parse JSON thủ công - thay vì @RequestBody tự động
        Product p = new Product();
        // Đơn giản hóa parser cho demo
        json = json.replace("{", "").replace("}", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();
                switch (key) {
                    case "name" -> p.setName(val);
                    case "description" -> p.setDescription(val);
                    case "price" -> p.setPrice(Double.parseDouble(val));
                }
            }
        }
        return p;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}