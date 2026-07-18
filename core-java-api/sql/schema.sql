-- Oracle 19c Schema for Core Java API
-- Run as: sqlplus username/password@//host:port/ORCL @schema.sql

-- Drop existing objects (in correct order to avoid FK issues)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE products CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
DROP SEQUENCE products_seq;
/

-- Create sequence for product IDs
CREATE SEQUENCE products_seq START WITH 1 INCREMENT BY 1;

-- Create products table
-- Using IDENTITY column (Oracle 12c+) instead of AUTO_INCREMENT
CREATE TABLE products (
    id NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    price NUMBER(10, 2) NOT NULL,
    description CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better search performance
CREATE INDEX idx_products_name ON products(UPPER(name));

-- Sample data
INSERT INTO products (name, price, description) VALUES ('Laptop', 999.99, 'High-performance laptop');
INSERT INTO products (name, price, description) VALUES ('Mouse', 29.99, 'Wireless mouse');
INSERT INTO products (name, price, description) VALUES ('Keyboard', 79.99, 'Mechanical keyboard');

-- Commit the transaction
COMMIT;

-- Verify data
SELECT * FROM products;