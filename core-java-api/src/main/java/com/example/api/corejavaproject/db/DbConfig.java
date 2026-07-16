package com.example.api.corejavaproject.db;

/**
 * Configuration for database connection.
 * Can be loaded from environment variables or a config file.
 */
public class DbConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DbConfig(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public static DbConfig fromEnvironment() {
        return new DbConfig(
            System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost",
            System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 3306,
            System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "core_java_db",
            System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root",
            System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : ""
        );
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
    }
}