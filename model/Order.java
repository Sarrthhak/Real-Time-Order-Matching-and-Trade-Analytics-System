package model;

import java.time.Instant;
import java.util.UUID;

public class Order {
    private final String orderId;
    private final String symbol;
    private final Side side;
    private final OrderType orderType;
    private double price;
    private final int quantity;  // Made final
    private final Instant timestamp;
    private OrderStatus status;
    private int filledQuantity;
    
    public enum Side {
        BUY, SELL
    }
    
    public enum OrderType {
        MARKET, LIMIT
    }
    
    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED
    }
    
    // Constructor for limit orders
    public Order(String symbol, Side side, double price, int quantity) {
        this.orderId = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.side = side;
        this.orderType = OrderType.LIMIT;
        this.price = price;
        this.quantity = quantity;  // Now final
        this.timestamp = Instant.now();
        this.status = OrderStatus.NEW;
        this.filledQuantity = 0;
    }
    
    // Constructor for market orders
    public Order(String symbol, Side side, int quantity) {
        this.orderId = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.side = side;
        this.orderType = OrderType.MARKET;
        this.price = 0.0; // Market orders don't have a price limit
        this.quantity = quantity;  // Now final
        this.timestamp = Instant.now();
        this.status = OrderStatus.NEW;
        this.filledQuantity = 0;
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Instant getTimestamp() { return timestamp; }
    public OrderStatus getStatus() { return status; }
    public int getFilledQuantity() { return filledQuantity; }
    public int getRemainingQuantity() { return quantity - filledQuantity; }
    
    // Setters
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setPrice(double price) { this.price = price; }
    
    public void fill(int quantity) {
        this.filledQuantity += quantity;
        if (this.filledQuantity >= this.quantity) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Order{id=%s, symbol=%s, side=%s, type=%s, price=%.2f, qty=%d, filled=%d, status=%s}",
                orderId, symbol, side, orderType, price, quantity, filledQuantity, status);
    }
}
