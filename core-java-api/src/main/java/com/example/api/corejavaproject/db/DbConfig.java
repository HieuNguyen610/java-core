package com.example.api.corejavaproject.db;

/**
 * Configuration for Oracle database connection.
 * Can be loaded from environment variables or a config file.
 */
public class DbConfig {

    private final String host;
    private final int port;
    private final String database;  // SID or Service Name
    private final String username;
    private final String password;
    private final boolean useSsl;

    public DbConfig(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, false);
    }

    public DbConfig(String host, int port, String database, String username, String password, boolean useSsl) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSsl = useSsl;
    }

    public static DbConfig fromEnvironment() {
        return new DbConfig(
            getEnv("DB_HOST", "localhost"),
            getEnvInt("DB_PORT", 1521),
            getEnv("DB_NAME", "ORCLPDB1"),  // Oracle 21c XE default PDB
            getEnv("DB_USER", "system"),
            getEnv("DB_PASSWORD", ""),
            getEnvBool("DB_SSL", false)
        );
    }

    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null ? val : defaultValue;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String val = System.getenv(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    private static boolean getEnvBool(String key, boolean defaultValue) {
        String val = System.getenv(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isUseSsl() { return useSsl; }

    /**
     * Builds Oracle JDBC connection string using Thin driver.
     * Format: jdbc:oracle:thin:@//host:port/serviceName (Service Name format)
     * Format: jdbc:oracle:thin:@host:port:SID (for SID-based)
     */
    public String getJdbcUrl() {
        // Oracle 21c XE uses Service Name format
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, database);
    }

    /**
     * Builds Oracle JDBC connection string using SID (older format).
     * Format: jdbc:oracle:thin:@host:port:SID
     */
    public String getJdbcUrlWithSid(String sid) {
        return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, sid != null ? sid : database);
    }
}