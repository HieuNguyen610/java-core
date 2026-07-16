package com.example.api.corejavaproject.service;

import com.example.api.corejavaproject.model.Product;
import com.example.api.corejavaproject.repository.ProductRepository;
import java.util.List;
import java.util.Optional;

/**
 * Service layer - thay thế CDI/Bean Managed trong Quarkus
 * Tự quản lý dependency (không có @Inject tự động)
 */
public class ProductService {

    // Khởi tạo thủ công - không có @Inject hay @Singleton tự động
    private final ProductRepository repository;

    public ProductService() {
        this.repository = new ProductRepository();
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        return repository.save(product);
    }

    public Optional<Product> getProduct(int id) {
        return repository.findById(id);
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Optional<Product> updateProduct(int id, Product updated) {
        Optional<Product> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        validateProduct(updated);
        updated.setId(id);
        return Optional.of(repository.save(updated));
    }

    public boolean deleteProduct(int id) {
        return repository.deleteById(id);
    }

    public List<Product> searchProducts(String keyword) {
        return repository.findByNameContaining(keyword);
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
}