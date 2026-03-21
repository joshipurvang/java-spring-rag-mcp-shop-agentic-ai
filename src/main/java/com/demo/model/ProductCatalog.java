package com.demo.model;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * ProductCatalog - holds products and orders in memory.
 *
 * Products are also indexed into Redis Vector Store at startup (see VectorStoreConfig).
 * Orders are tracked here in a simple HashMap.
 */
@Component
public class ProductCatalog {

    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private int orderSeq = 1000;

    public ProductCatalog() {
        add("P001", "MacBook Pro M3 14-inch",    "Laptops",     1999.99, 12,
            "Apple M3 chip, 18GB RAM, 512GB SSD, Liquid Retina display, great for developers");
        add("P002", "Dell XPS 15 OLED",          "Laptops",     1799.99, 5,
            "Intel Core i9, RTX 4070, 32GB RAM, 1TB SSD, 3.5K OLED screen for creative work");
        add("P003", "Sony WH-1000XM5",           "Headphones",  349.99,  30,
            "Industry-leading noise cancellation, 30hr battery, premium audio quality");
        add("P004", "Samsung Galaxy S24 Ultra",   "Phones",      999.99,  25,
            "200MP camera, S Pen stylus, 12GB RAM, AI photography features, 5G");
        add("P005", "iPad Pro 12.9 M4",           "Tablets",    1099.99, 8,
            "M4 chip, Ultra Retina OLED, Apple Pencil Pro support, ideal for artists");
        add("P006", "Logitech MX Master 3S",      "Accessories",  99.99, 50,
            "Ergonomic wireless mouse, 8K DPI, quiet clicks, USB-C, works with 3 devices");
        add("P007", "LG 27-inch 4K UltraFine",   "Monitors",    599.99,  15,
            "4K IPS panel, 99% DCI-P3, USB-C 96W charging, for professional photo editing");
        add("P008", "Keychron Q1 Pro",            "Keyboards",   199.99,  20,
            "Wireless mechanical keyboard, Gateron switches, aluminum body, RGB lighting");
        add("P009", "Nintendo Switch OLED",       "Gaming",      349.99,  7,
            "7-inch OLED display, portable gaming console, 64GB storage, enhanced audio");
        add("P010", "Philips Hue Starter Kit",    "Smart Home",  199.99,  0,
            "Smart LED bulbs, 16 million colors, Alexa and Google Assistant compatible");
    }

    private void add(String id, String name, String category,
                     double price, int stock, String description) {
        products.put(id, new Product(id, name, category, price, stock, description));
    }

    public Collection<Product> all() { return products.values(); }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public String placeOrder(String productId, int qty, String email) {
        Product p = products.get(productId);
        if (p == null) return "ERROR: product " + productId + " not found";
        if (p.stock() < qty) return "ERROR: only " + p.stock() + " units in stock";

        // Decrement stock
        products.put(productId, new Product(
            p.id(), p.name(), p.category(), p.price(), p.stock() - qty, p.description()));

        String orderId = "ORD-" + (++orderSeq);
        double total = p.price() * qty;
        orders.put(orderId, new Order(orderId, productId, p.name(), qty, total, email, "CONFIRMED"));

        return String.format("Order %s confirmed — %d x %s — $%.2f — sent to %s",
            orderId, qty, p.name(), total, email);
    }

    public String getOrderStatus(String orderId) {
        return Optional.ofNullable(orders.get(orderId))
            .map(o -> String.format("Order %s: %d x %s | $%.2f | Status: %s | Email: %s",
                o.orderId(), o.qty(), o.productName(), o.total(), o.status(), o.email()))
            .orElse("Order not found: " + orderId);
    }

    public Collection<Order> allOrders() { return orders.values(); }

    // ── Records ──────────────────────────────────────────────────────────────

    public record Product(String id, String name, String category,
                          double price, int stock, String description) {

        /** Rich text used to create the embedding stored in Redis Vector Store */
        public String toEmbeddingText() {
            return String.format(
                "Product ID: %s | Name: %s | Category: %s | Price: $%.2f | " +
                "Stock: %d units | Description: %s",
                id, name, category, price, stock, description);
        }
    }

    public record Order(String orderId, String productId, String productName,
                        int qty, double total, String email, String status) {}
}
