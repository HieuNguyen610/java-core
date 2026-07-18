-- Oracle 21c XE Schema for Core Java API
-- Database: ORCLPDB1 (Pluggable Database)
-- Run as: sqlplus system/password@//localhost:1521/ORCLPDB1 @schema.sql

-- Drop existing objects (in correct order to avoid FK issues)
DECLARE
    v_count NUMBER;
BEGIN
    -- Check if table exists
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'PRODUCTS';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE products CASCADE CONSTRAINTS';
    END IF;

    -- Drop sequence if exists
    SELECT COUNT(*) INTO v_count FROM user_sequences WHERE sequence_name = 'PRODUCTS_SEQ';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP SEQUENCE products_seq';
    END IF;
END;
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