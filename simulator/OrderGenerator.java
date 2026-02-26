package simulator;

import engine.MatchingEngine;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import model.Order;

public class OrderGenerator {
    private final MatchingEngine matchingEngine;
    private final ScheduledExecutorService scheduler;
    private final List<String> symbols;
    private final Random random;
    private final AtomicBoolean isRunning;
    private int orderCount = 0;
    
    // Configuration
    private static final int MIN_QUANTITY = 10;
    private static final int MAX_QUANTITY = 1000;
    private static final double MIN_PRICE = 10.0;
    private static final double MAX_PRICE = 1000.0;
    private static final double PRICE_VOLATILITY = 0.02; // 2% price variation
    
    public OrderGenerator(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.symbols = new ArrayList<>();
        this.random = new Random();
        this.isRunning = new AtomicBoolean(false);
        
        // Initialize with some default symbols - moved to a separate method
        initializeDefaultSymbols();
    }
    
    // Private method to initialize symbols - avoids overridable method call in constructor
    private void initializeDefaultSymbols() {
        // Directly add to the list instead of calling addSymbol
        symbols.add("AAPL");
        symbols.add("GOOGL");
        symbols.add("MSFT");
        symbols.add("AMZN");
        symbols.add("TSLA");
        
        // Also add order books in matching engine
        for (String symbol : symbols) {
            matchingEngine.addOrderBook(symbol);
        }
    }
    
    public void addSymbol(String symbol) {
        symbols.add(symbol);
        matchingEngine.addOrderBook(symbol);
    }
    
    public void start(int ordersPerSecond) {
        if (isRunning.compareAndSet(false, true)) {
            long delayMillis = 1000 / ordersPerSecond;
            
            // Schedule order generation
            scheduler.scheduleAtFixedRate(
                this::generateRandomOrder,
                0,
                delayMillis,
                TimeUnit.MILLISECONDS
            );
            
            // Schedule market making orders to provide liquidity
            scheduler.scheduleAtFixedRate(
                this::generateMarketMakingOrders,
                500,
                2000,
                TimeUnit.MILLISECONDS
            );
            
            System.out.println("Order generator started with " + ordersPerSecond + " orders/sec");
        }
    }
    
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Order generator stopped");
        }
    }
    
    private void generateRandomOrder() {
        if (!isRunning.get()) return;
        
        try {
            String symbol = symbols.get(random.nextInt(symbols.size()));
            Order.Side side = random.nextBoolean() ? Order.Side.BUY : Order.Side.SELL;
            
            // 70% limit orders, 30% market orders
            Order.OrderType type = random.nextInt(10) < 7 ? 
                Order.OrderType.LIMIT : Order.OrderType.MARKET;
            
            Order order;
            if (type == Order.OrderType.LIMIT) {
                double price = generatePrice(symbol); // Removed unused side parameter
                int quantity = generateQuantity();
                order = new Order(symbol, side, price, quantity);
            } else {
                int quantity = generateQuantity();
                order = new Order(symbol, side, quantity);
            }
            
            matchingEngine.submitOrder(order);
            orderCount++;
            
            if (orderCount % 100 == 0) {
                System.out.println("Generated " + orderCount + " orders so far");
            }
            
        } catch (Exception e) {
            System.err.println("Error generating order: " + e.getMessage());
        }
    }
    
    private void generateMarketMakingOrders() {
        // Generate pairs of buy and sell orders to provide liquidity
        for (String symbol : symbols) {
            double midPrice = getMidPrice(symbol);
            if (midPrice > 0) {
                double spread = midPrice * 0.001; // 0.1% spread
                
                // Generate buy order slightly below mid
                double buyPrice = midPrice - spread/2;
                Order buyOrder = new Order(symbol, Order.Side.BUY, buyPrice, 500);
                matchingEngine.submitOrder(buyOrder);
                
                // Generate sell order slightly above mid
                double sellPrice = midPrice + spread/2;
                Order sellOrder = new Order(symbol, Order.Side.SELL, sellPrice, 500);
                matchingEngine.submitOrder(sellOrder);
            }
        }
    }
    
    // Removed the unused 'side' parameter
    private double generatePrice(String symbol) {
        double basePrice = getMidPrice(symbol);
        if (basePrice == 0) {
            basePrice = MIN_PRICE + random.nextDouble() * (MAX_PRICE - MIN_PRICE);
        }
        
        // Add some randomness
        double variation = basePrice * PRICE_VOLATILITY * (random.nextDouble() - 0.5);
        double price = basePrice + variation;
        
        // Round to 2 decimal places
        return Math.round(price * 100.0) / 100.0;
    }
    
    private double getMidPrice(String symbol) {
        double bestBid = matchingEngine.getBestBid(symbol);
        double bestAsk = matchingEngine.getBestAsk(symbol);
        
        if (bestBid > 0 && bestAsk > 0) {
            return (bestBid + bestAsk) / 2;
        } else if (bestBid > 0) {
            return bestBid;
        } else if (bestAsk > 0) {
            return bestAsk;
        }
        return 0;
    }
    
    private int generateQuantity() {
        return MIN_QUANTITY + random.nextInt(MAX_QUANTITY - MIN_QUANTITY + 1);
    }
    
    public int getOrderCount() {
        return orderCount;
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public List<String> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }
}
