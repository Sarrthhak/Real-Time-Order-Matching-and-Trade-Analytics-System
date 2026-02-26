package engine;

import analytics.AnalyticsService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import model.Order;
import model.Trade;

public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks;
    private final AnalyticsService analyticsService;
    private final SubmissionPublisher<Trade> tradePublisher;
    private final ExecutorService executorService;
    private final List<Flow.Subscriber<Trade>> subscribers;
    
    public MatchingEngine(AnalyticsService analyticsService) {
        this.orderBooks = new ConcurrentHashMap<>();
        this.analyticsService = analyticsService;
        this.tradePublisher = new SubmissionPublisher<>();
        this.executorService = Executors.newFixedThreadPool(4);
        this.subscribers = new ArrayList<>();
        
        // Move subscription outside constructor or make analyticsService final
    }

    // Add a separate initialization method
    public void init() {
        // Subscribe analytics service to trades
        subscribe(analyticsService);
    }
    
    public void addOrderBook(String symbol) {
        orderBooks.putIfAbsent(symbol, new OrderBook(symbol));
    }
    
    public void submitOrder(Order order) {
        executorService.submit(() -> processOrder(order));
    }
    
    private void processOrder(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            orderBook = new OrderBook(order.getSymbol());
            orderBooks.put(order.getSymbol(), orderBook);
        }
        
        List<Trade> trades = orderBook.matchOrder(order);
        
        // Publish trades to subscribers
        for (Trade trade : trades) {
            tradePublisher.submit(trade);
        }
        
        // Update analytics
        analyticsService.updateMetrics(trades);
    }
    
    public boolean cancelOrder(String symbol, String orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook != null) {
            Order order = orderBook.getOrder(orderId);
            if (order != null && order.getStatus() != Order.OrderStatus.FILLED) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderBook.removeOrder(orderId);
                return true;
            }
        }
        return false;
    }
    
    public Order getOrder(String symbol, String orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null ? orderBook.getOrder(orderId) : null;
    }
    
    public double getBestBid(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null ? orderBook.getBestBid() : 0.0;
    }
    
    public double getBestAsk(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null ? orderBook.getBestAsk() : 0.0;
    }
    
    public double getSpread(String symbol) {
        double bestAsk = getBestAsk(symbol);
        double bestBid = getBestBid(symbol);
        if (bestAsk > 0 && bestBid > 0) {
            return bestAsk - bestBid;
        }
        return 0.0;
    }
    
    public int getMarketDepth(String symbol, Order.Side side, int levels) {
        // Simplified market depth - returns total volume at top N levels
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) return 0;
        
        int totalVolume = 0;
        double currentPrice = side == Order.Side.BUY ? orderBook.getBestBid() : orderBook.getBestAsk();
        
        for (int i = 0; i < levels; i++) {
            totalVolume += orderBook.getVolumeAtPrice(side, currentPrice);
            // In a real implementation, you'd need to get the next price level
            // This is simplified for demonstration
        }
        
        return totalVolume;
    }
    
    public void subscribe(Flow.Subscriber<Trade> subscriber) {
        tradePublisher.subscribe(subscriber);
        subscribers.add(subscriber);
    }
    
    public void shutdown() {
        tradePublisher.close();
        executorService.shutdown();
    }
    
    public Set<String> getAvailableSymbols() {
        return orderBooks.keySet();
    }
    
    public Map<String, Object> getMarketSummary(String symbol) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("symbol", symbol);
        summary.put("bestBid", getBestBid(symbol));
        summary.put("bestAsk", getBestAsk(symbol));
        summary.put("spread", getSpread(symbol));
        summary.put("vwap", analyticsService.getVWAP(symbol));
        summary.put("totalVolume", analyticsService.getTotalVolume(symbol));
        summary.put("tradeCount", analyticsService.getTradeCount(symbol));
        summary.put("lastPrice", analyticsService.getLastPrice(symbol));
        summary.put("priceChange24h", analyticsService.getPriceChange24h(symbol));
        return summary;
    }
}
