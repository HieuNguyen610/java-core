package com.example.api.corejavaproject.repository;

import com.example.api.corejavaproject.db.DbConfig;
import com.example.api.corejavaproject.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository using raw JDBC for MySQL database.
 * Replaces JPA/Hibernate with plain JDBC queries - core Java SE only.
 */
public class ProductRepository {

    private final DbConfig dbConfig;

    public ProductRepository() {
        this(DbConfig.fromEnvironment());
    }

    public ProductRepository(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Saves a product to the database.
     * If product.id == 0, it's a new product (INSERT).
     * Otherwise, it updates existing product (UPDATE).
     */
    public Product save(Product product) {
        String sql;
        boolean isInsert = product.getId() == 0;

        if (isInsert) {
            sql = "INSERT INTO products (name, price, description) VALUES (?, ?, ?)";
        } else {
            sql = "UPDATE products SET name = ?, price = ?, description = ? WHERE id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            stmt.setString(3, product.getDescription());

            if (!isInsert) {
                stmt.setInt(4, product.getId());
            }

            int affectedRows = stmt.executeUpdate();

            if (isInsert && affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return product;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product: " + product, e);
        }
    }

    public Optional<Product> findById(int id) {
        String sql = "SELECT id, name, price, description, created_at, updated_at FROM products WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product by id: " + id, e);
        }
    }

    public List<Product> findAll() {
        String sql = "SELECT id, name, price, description, created_at, updated_at FROM products ORDER BY id";
        List<Product> products = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
            return products;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all products", e);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product by id: " + id, e);
        }
    }

    public List<Product> findByNameContaining(String keyword) {
        String sql = "SELECT id, name, price, description, created_at, updated_at FROM products WHERE LOWER(name) LIKE ?";
        List<Product> products = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + keyword.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
            return products;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find products by name containing: " + keyword, e);
        }
    }

    /**
     * Creates the products table if it doesn't exist.
     */
    public void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                price DECIMAL(10, 2) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create products table", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            dbConfig.getJdbcUrl(),
            dbConfig.getUsername(),
            dbConfig.getPassword()
        );
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getDouble("price"));
        product.setDescription(rs.getString("description"));
        return product;
    }
}