package com.example.api.corejavaproject.repository;

import com.example.api.corejavaproject.db.DbConfig;
import com.example.api.corejavaproject.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository using raw JDBC for Oracle database.
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
     *
     * Note: Oracle uses SEQUENCE/IDENTITY for auto-increment, not AUTO_INCREMENT.
     * For insert, we use RETURNING clause to get generated ID.
     */
    public Product save(Product product) {
        boolean isInsert = product.getId() == 0;

        if (isInsert) {
            return insertProduct(product);
        } else {
            return updateProduct(product);
        }
    }

    private Product insertProduct(Product product) {
        // Oracle: Use RETURNING clause instead of getGeneratedKeys()
        String sql = "INSERT INTO products (name, price, description) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            stmt.setString(3, product.getDescription());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Oracle: Get the generated ID using RETURNING clause
                try (PreparedStatement idStmt = conn.prepareStatement(
                        "SELECT products_seq.CURRVAL FROM DUAL")) {
                    try (ResultSet rs = idStmt.executeQuery()) {
                        if (rs.next()) {
                            product.setId(rs.getInt(1));
                        }
                    }
                }
            }
            return product;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert product: " + product, e);
        }
    }

    private Product updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, price = ?, description = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            stmt.setString(3, product.getDescription());
            stmt.setInt(4, product.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Product not found with id: " + product.getId());
            }
            return product;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product: " + product, e);
        }
    }

    public Optional<Product> findById(int id) {
        // Oracle: Use FETCH FIRST 1 ROWS ONLY instead of LIMIT
        String sql = "SELECT id, name, price, description FROM products WHERE id = ?";

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
        // Oracle: ORDER BY id
        // For pagination, use FETCH FIRST N ROWS ONLY (Oracle 12c+)
        String sql = "SELECT id, name, price, description FROM products ORDER BY id";
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
        // Oracle: Use UPPER() or LOWER() for case-insensitive search
        String sql = "SELECT id, name, price, description FROM products WHERE LOWER(name) LIKE ? ORDER BY id";
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
     * Oracle uses IDENTITY column (Oracle 12c+) instead of AUTO_INCREMENT.
     * Also creates a sequence for manual ID generation.
     */
    public void createTableIfNotExists() {
        String createSequenceSql = """
            BEGIN
                EXECUTE IMMEDIATE 'CREATE SEQUENCE products_seq START WITH 1 INCREMENT BY 1';
            EXCEPTION
                WHEN OTHERS THEN
                    IF SQLCODE = -2289 THEN NULL;
                    ELSE RAISE;
                END IF;
            END;
            """;

        String createTableSql = """
            CREATE TABLE products (
                id NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                name VARCHAR2(255) NOT NULL,
                price NUMBER(10, 2) NOT NULL,
                description CLOB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String checkTableSql = "SELECT COUNT(*) FROM user_tables WHERE table_name = 'PRODUCTS'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if table exists
            try (ResultSet rs = stmt.executeQuery(checkTableSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Table doesn't exist, create it
                    try {
                        stmt.execute(createTableSql);
                        System.out.println("Products table created successfully.");
                    } catch (SQLException e) {
                        // Table might already exist (race condition)
                        if (e.getErrorCode() != 955) { // ORA-00955: name is already used by an existing object
                            throw e;
                        }
                    }
                }
            }

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