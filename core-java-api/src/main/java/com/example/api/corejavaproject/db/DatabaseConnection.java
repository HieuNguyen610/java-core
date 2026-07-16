package com.example.api.corejavaproject.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage MySQL database connections using JDBC.
 * Uses core Java SE - no frameworks.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/core_java_db";
    private static final String DEFAULT_USER = "root";
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

    /**
     * Creates a new database connection.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC driver (optional for modern drivers, but good practice)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Creates a connection with custom properties.
     */
    public Connection getConnection(Properties props) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(url, props);
    }

    /**
     * Factory method to create a connection with connection string components.
     */
    public static Connection connect(String host, int port, String database, String username, String password)
            throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
        return connect(url, username, password);
    }

    /**
     * Factory method to create a connection with a full JDBC URL.
     */
    public static Connection connect(String jdbcUrl, String username, String password) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    public String toString() {
        return "DatabaseConnection{url='" + url + "', user='" + user + "'}";
    }
}