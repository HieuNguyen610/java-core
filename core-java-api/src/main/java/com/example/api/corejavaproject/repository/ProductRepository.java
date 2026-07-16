package com.example.api.corejavaproject.repository;

import com.example.api.corejavaproject.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository - thay thế JPA/Hibernate bằng tự quản lý in-memory database
 * Trong thực tế, đây sẽ là JDBC raw queries
 */
public class ProductRepository {

    // In-memory database simulation - thay vì JPA Repository
    private final Map<Integer, Product> database = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public ProductRepository() {
        // Initialize sample data
        save(new Product(0, "Laptop", 999.99, "Gaming laptop"));
        save(new Product(0, "Mouse", 29.99, "Wireless mouse"));
        save(new Product(0, "Keyboard", 79.99, "Mechanical keyboard"));
    }

    public Product save(Product product) {
        if (product.getId() == 0) {
            product.setId(nextId.getAndIncrement());
        }
        database.put(product.getId(), product);
        return product;
    }

    public Optional<Product> findById(int id) {
        return Optional.ofNullable(database.get(id));
    }

    public List<Product> findAll() {
        return new ArrayList<>(database.values());
    }

    public boolean deleteById(int id) {
        return database.remove(id) != null;
    }

    public List<Product> findByNameContaining(String keyword) {
        return database.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}