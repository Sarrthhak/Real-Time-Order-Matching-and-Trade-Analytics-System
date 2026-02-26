package analytics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import model.Trade;

public class AnalyticsService implements Flow.Subscriber<Trade> {
    private final Map<String, SymbolAnalytics> analyticsMap;
    private Flow.Subscription subscription; // Now properly used
    
    public AnalyticsService() {
        this.analyticsMap = new ConcurrentHashMap<>();
    }
    
    // Flow.Subscriber methods
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        // Request items with backpressure support
        subscription.request(1); // Start with 1, then request more after each item
        System.out.println("AnalyticsService subscribed to trade stream");
    }
    
    @Override
    public void onNext(Trade trade) {
        // Process the trade
        updateMetrics(List.of(trade));
        
        // Request the next item - this properly uses the subscription
        if (subscription != null) {
            subscription.request(1);
        }
    }
    
    @Override
    public void onError(Throwable throwable) {
        System.err.println("Error in analytics service: " + throwable.getMessage());
        // In a production system, you might want to retry or handle this gracefully
    }
    
    @Override
    public void onComplete() {
        System.out.println("Analytics service completed");
        // Cancel subscription to clean up
        if (subscription != null) {
            subscription.cancel();
        }
    }
    
    // Method to manually cancel subscription if needed
    public void cancel() {
        if (subscription != null) {
            subscription.cancel();
        }
    }
    
    public void updateMetrics(List<Trade> trades) {
        for (Trade trade : trades) {
            SymbolAnalytics analytics = analyticsMap.computeIfAbsent(
                trade.getSymbol(), k -> new SymbolAnalytics());
            analytics.update(trade);
        }
    }
    
    public double getVWAP(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getVWAP() : 0.0;
    }
    
    public double getTotalVolume(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getTotalVolume() : 0.0;
    }
    
    public int getTradeCount(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getTradeCount() : 0;
    }
    
    public double getLastPrice(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getLastPrice() : 0.0;
    }
    
    public double getPriceChange24h(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getPriceChange24h() : 0.0;
    }
    
    public double getHighPrice(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getHighPrice() : 0.0;
    }
    
    public double getLowPrice(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getLowPrice() : 0.0;
    }
    
    public double getAverageTradeSize(String symbol) {
        SymbolAnalytics analytics = analyticsMap.get(symbol);
        return analytics != null ? analytics.getAverageTradeSize() : 0.0;
    }
    
    public Map<String, Object> getAllMetrics(String symbol) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("vwap", getVWAP(symbol));
        metrics.put("totalVolume", getTotalVolume(symbol));
        metrics.put("tradeCount", getTradeCount(symbol));
        metrics.put("lastPrice", getLastPrice(symbol));
        metrics.put("priceChange24h", getPriceChange24h(symbol));
        metrics.put("highPrice", getHighPrice(symbol));
        metrics.put("lowPrice", getLowPrice(symbol));
        metrics.put("averageTradeSize", getAverageTradeSize(symbol));
        return metrics;
    }
    
    // Inner class for per-symbol analytics
    private static class SymbolAnalytics {
        private double totalVolume = 0;
        private double totalVWAPVolume = 0;
        private int tradeCount = 0;
        private double lastPrice = 0;
        private double highPrice = Double.MIN_VALUE;
        private double lowPrice = Double.MAX_VALUE;
        private final LinkedList<Trade> recentTrades;
        private static final int MAX_RECENT_TRADES = 1000;
        
        public SymbolAnalytics() {
            this.recentTrades = new LinkedList<>();
        }
        
        public synchronized void update(Trade trade) {
            totalVolume += trade.getQuantity();
            totalVWAPVolume += trade.getPrice() * trade.getQuantity();
            tradeCount++;
            lastPrice = trade.getPrice();
            highPrice = Math.max(highPrice, trade.getPrice());
            lowPrice = Math.min(lowPrice, trade.getPrice());
            
            recentTrades.add(trade);
            if (recentTrades.size() > MAX_RECENT_TRADES) {
                recentTrades.removeFirst();
            }
        }
        
        public synchronized double getVWAP() {
            return totalVolume > 0 ? totalVWAPVolume / totalVolume : 0.0;
        }
        
        public synchronized double getTotalVolume() {
            return totalVolume;
        }
        
        public synchronized int getTradeCount() {
            return tradeCount;
        }
        
        public synchronized double getLastPrice() {
            return lastPrice;
        }
        
        public synchronized double getPriceChange24h() {
            if (recentTrades.size() < 2) return 0.0;
            Trade oldestTrade = recentTrades.getFirst();
            Trade newestTrade = recentTrades.getLast();
            
            if (oldestTrade != null && newestTrade != null && oldestTrade.getPrice() > 0) {
                return ((newestTrade.getPrice() - oldestTrade.getPrice()) / oldestTrade.getPrice()) * 100;
            }
            return 0.0;
        }
        
        public synchronized double getHighPrice() {
            return highPrice != Double.MIN_VALUE ? highPrice : 0.0;
        }
        
        public synchronized double getLowPrice() {
            return lowPrice != Double.MAX_VALUE ? lowPrice : 0.0;
        }
        
        public synchronized double getAverageTradeSize() {
            return tradeCount > 0 ? totalVolume / tradeCount : 0.0;
        }
    }
}
