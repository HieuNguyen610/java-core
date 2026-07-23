package com.example.api.corejavaproject.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Model class - không có annotation gì cả (khác với Quarkus/Spring dùng @Entity)
 *
 * JAXB annotations added for SOAP XML serialization.
 */
@XmlRootElement(name = "Product")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(name = "Product", propOrder = {"id", "name", "price", "description"})
public class Product {
    private int id;
    private String name;
    private double price;
    private String description;

    public Product() {}

    public Product(int id, String name, double price, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
    }

    // Getters và Setters - tự viết tay, không có Lombok
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
    }
}