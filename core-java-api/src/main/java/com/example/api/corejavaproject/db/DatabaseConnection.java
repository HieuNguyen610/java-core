package com.example.api.corejavaproject.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage Oracle database connections using JDBC.
 * Uses core Java SE - no frameworks.
 */
public class DatabaseConnection {

    // Oracle 21c XE default: ORCLPDB1 (Pluggable Database)
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/ORCLPDB1";
    private static final String DEFAULT_USER = "system";
    private static final String DEFAULT_PASSWORD = "";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnection() {
        this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    public DatabaseConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public DatabaseConnection(DbConfig config) {
        this(config.getJdbcUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * Creates a new database connection.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        try {
            // Load the Oracle JDBC driver
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found. Make sure ojdbc8.jar is in lib/", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Creates a connection with custom properties.
     */
    public Connection getConnection(Properties props) throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found. Make sure ojdbc8.jar is in lib/", e);
        }
        return DriverManager.getConnection(url, props);
    }

    /**
     * Factory method to create a connection with connection string components.
     */
    public static Connection connect(String host, int port, String serviceName, String username, String password)
            throws SQLException {
        String url = String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, serviceName);
        return connect(url, username, password);
    }

    /**
     * Factory method to create a connection with a full JDBC URL.
     */
    public static Connection connect(String jdbcUrl, String username, String password) throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found. Make sure ojdbc8.jar is in lib/", e);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    public String toString() {
        return "DatabaseConnection{url='" + url + "', user='" + user + "'}";
    }
}