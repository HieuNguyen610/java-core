package com.example.api.corejavaproject.server;

/**
 * Main Entry Point - thay vì Quarkus main class hoặc SpringApplication.run()
 *
 * Trong Quarkus: chỉ cần @QuarkusApplication là tự khởi động
 * Trong Core Java: phải tự tạo server, handle lifecycle
 */
public class Main {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        System.out.println("=".repeat(60));
        System.out.println("  CORE JAVA API - Khong dung framework");
        System.out.println("=".repeat(60));

        try {
            ProductHttpServer httpServer = new ProductHttpServer(port);
            httpServer.start();

            // Thêm shutdown hook - Quarkus/Spring có sẵn
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down server...");
                httpServer.stop();
            }));

            // Giữ process alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}