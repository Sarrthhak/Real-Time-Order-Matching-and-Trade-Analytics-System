package model;

import java.time.Instant;
import java.util.UUID;

public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final Instant timestamp;
    
    public Trade(String buyOrderId, String sellOrderId, String symbol, double price, int quantity) {
        this.tradeId = UUID.randomUUID().toString();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getTradeId() { return tradeId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("Trade{id=%s, symbol=%s, price=%.2f, qty=%d, buyOrder=%s, sellOrder=%s}",
                tradeId, symbol, price, quantity, buyOrderId, sellOrderId);
    }
}
