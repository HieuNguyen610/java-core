-- Create database if not exists
CREATE DATABASE IF NOT EXISTS core_java_db;
USE core_java_db;

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sample data
INSERT INTO products (name, price, description) VALUES
    ('Laptop', 999.99, 'High-performance laptop'),
    ('Mouse', 29.99, 'Wireless mouse'),
    ('Keyboard', 79.99, 'Mechanical keyboard');