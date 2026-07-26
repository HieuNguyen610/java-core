package com.example.api.corejavaproject.model;

/**
 * Model class for Kafka User - không có annotation gì cả (pure Java SE)
 */
public class KafkaUser {
    private int id;
    private String username;
    private String address;

    public KafkaUser() {}

    public KafkaUser(int id, String username, String address) {
        this.id = id;
        this.username = username;
        this.address = address;
    }

    // Getters và Setters - tự viết tay, không có Lombok
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "KafkaUser{id=" + id + ", username='" + username + "', address='" + address + "'}";
    }

    /**
     * Convert to JSON string for Kafka message
     */
    public String toJson() {
        return String.format("{\"id\":%d,\"username\":\"%s\",\"address\":\"%s\"}",
                id, username != null ? username : "", address != null ? address : "");
    }

    /**
     * Parse from JSON string
     */
    public static KafkaUser fromJson(String json) {
        KafkaUser user = new KafkaUser();
        // Simple JSON parsing - không dùng Jackson/Gson
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    switch (key) {
                        case "id":
                            user.setId(Integer.parseInt(value));
                            break;
                        case "username":
                            user.setUsername(value);
                            break;
                        case "address":
                            user.setAddress(value);
                            break;
                    }
                }
            }
        }
        return user;
    }
}