import analytics.AnalyticsService;
import engine.MatchingEngine;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import model.Order;
import simulator.OrderGenerator;

public class Main {
    private static MatchingEngine matchingEngine;
    private static AnalyticsService analyticsService;
    private static OrderGenerator orderGenerator;
    private static boolean isRunning = true;
    
    public static void main(String[] args) {
        System.out.println("======================================");
        System.out.println("Real-Time Order Matching System");
        System.out.println("======================================");
        
        // Initialize components
        initializeSystem();
        
        // Start the order generator
        orderGenerator.start(50); // 50 orders per second
        
        // Start the interactive console
        startConsole();
        
        // Shutdown hook for clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down system...");
            shutdown();
        }));
    }
    
    private static void initializeSystem() {
        System.out.println("Initializing system...");
        
        // Create analytics service
        analyticsService = new AnalyticsService();
        
        // Create matching engine with analytics
        matchingEngine = new MatchingEngine(analyticsService);
        
        // Create order generator
        orderGenerator = new OrderGenerator(matchingEngine);
        
        System.out.println("System initialized successfully!");
        System.out.println("Trading symbols: " + orderGenerator.getSymbols());
    }
    
    private static void startConsole() {
        // Using try-with-resources to automatically close scanner
        try (Scanner scanner = new Scanner(System.in)) {
            while (isRunning) {
                try {
                    System.out.println("\n--- Real-Time Trading System Menu ---");
                    System.out.println("1. View Market Summary");
                    System.out.println("2. View Detailed Analytics");
                    System.out.println("3. Place Order Manually");
                    System.out.println("4. Cancel Order");
                    System.out.println("5. View Order Book");
                    System.out.println("6. Adjust Order Generation Rate");
                    System.out.println("7. Stop/Start Order Generator");
                    System.out.println("8. Exit");
                    System.out.print("Select option: ");
                    
                    String input = scanner.nextLine().trim();
                    
                    // Using enhanced switch expression (Java 14+)
                    switch (input) {
                        case "1" -> viewMarketSummary();
                        case "2" -> viewDetailedAnalytics(scanner);
                        case "3" -> placeManualOrder(scanner);
                        case "4" -> cancelOrder(scanner);
                        case "5" -> viewOrderBook(scanner);
                        case "6" -> adjustOrderRate(scanner);
                        case "7" -> toggleOrderGenerator();
                        case "8" -> {
                            isRunning = false;
                            System.out.println("Exiting...");
                        }
                        default -> System.out.println("Invalid option. Please try again.");
                    }
                    
                    TimeUnit.MILLISECONDS.sleep(100);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Console interrupted: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    System.err.println("Error in console: " + e.getMessage());
                }
            }
        } // Scanner automatically closed here
    }
    
    private static void viewMarketSummary() {
        System.out.println("\n--- Market Summary ---");
        System.out.printf("%-8s %-12s %-12s %-12s %-12s %-12s%n", 
            "Symbol", "Best Bid", "Best Ask", "Spread", "Last Price", "Volume");
        System.out.println("-".repeat(80));
        
        for (String symbol : orderGenerator.getSymbols()) {
            try {
                double bestBid = matchingEngine.getBestBid(symbol);
                double bestAsk = matchingEngine.getBestAsk(symbol);
                double spread = matchingEngine.getSpread(symbol);
                double lastPrice = analyticsService.getLastPrice(symbol);
                double volume = analyticsService.getTotalVolume(symbol);
                
                System.out.printf("%-8s $%-10.2f $%-10.2f $%-10.2f $%-10.2f %-12.0f%n",
                    symbol, bestBid, bestAsk, spread, lastPrice, volume);
            } catch (Exception e) {
                System.out.printf("%-8s Error loading data: %s%n", symbol, e.getMessage());
            }
        }
    }
    
    private static void viewDetailedAnalytics(Scanner scanner) {
        System.out.print("Enter symbol (or 'all' for all): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        
        if (symbol.equals("ALL")) {
            for (String sym : orderGenerator.getSymbols()) {
                printSymbolAnalytics(sym);
                System.out.println("-".repeat(50));
            }
        } else if (orderGenerator.getSymbols().contains(symbol)) {
            printSymbolAnalytics(symbol);
        } else {
            System.out.println("Symbol not found!");
        }
    }
    
    private static void printSymbolAnalytics(String symbol) {
        try {
            System.out.println("\nAnalytics for " + symbol + ":");
            System.out.printf("  VWAP: $%.2f%n", analyticsService.getVWAP(symbol));
            System.out.printf("  Total Volume: %.0f%n", analyticsService.getTotalVolume(symbol));
            System.out.printf("  Trade Count: %d%n", analyticsService.getTradeCount(symbol));
            System.out.printf("  Last Price: $%.2f%n", analyticsService.getLastPrice(symbol));
            System.out.printf("  24h Change: %.2f%%%n", analyticsService.getPriceChange24h(symbol));
            System.out.printf("  24h High: $%.2f%n", analyticsService.getHighPrice(symbol));
            System.out.printf("  24h Low: $%.2f%n", analyticsService.getLowPrice(symbol));
            System.out.printf("  Avg Trade Size: %.2f%n", analyticsService.getAverageTradeSize(symbol));
        } catch (Exception e) {
            System.out.println("  Error loading analytics: " + e.getMessage());
        }
    }
    
    private static void placeManualOrder(Scanner scanner) {
        try {
            System.out.println("\n--- Place Manual Order ---");
            
            System.out.print("Enter symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            
            if (!orderGenerator.getSymbols().contains(symbol)) {
                System.out.println("Symbol not supported!");
                return;
            }
            
            System.out.print("Enter side (BUY/SELL): ");
            String sideStr = scanner.nextLine().trim().toUpperCase();
            Order.Side side = Order.Side.valueOf(sideStr);
            
            System.out.print("Enter order type (LIMIT/MARKET): ");
            String typeStr = scanner.nextLine().trim().toUpperCase();
            Order.OrderType type = Order.OrderType.valueOf(typeStr);
            
            System.out.print("Enter quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            
            Order order;
            if (type == Order.OrderType.LIMIT) {
                System.out.print("Enter price: ");
                double price = Double.parseDouble(scanner.nextLine().trim());
                order = new Order(symbol, side, price, quantity);
            } else {
                order = new Order(symbol, side, quantity);
            }
            
            matchingEngine.submitOrder(order);
            System.out.println("Order submitted: " + order);
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
            System.out.println("Please check your side (BUY/SELL), type (LIMIT/MARKET), or numeric values.");
        } catch (Exception e) {
            System.out.println("Error placing order: " + e.getMessage());
        }
    }
    
    private static void cancelOrder(Scanner scanner) {
        try {
            System.out.print("Enter symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            
            System.out.print("Enter order ID: ");
            String orderId = scanner.nextLine().trim();
            
            boolean cancelled = matchingEngine.cancelOrder(symbol, orderId);
            if (cancelled) {
                System.out.println("Order " + orderId + " cancelled successfully");
            } else {
                System.out.println("Order not found or cannot be cancelled");
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error cancelling order: " + e.getMessage());
        }
    }
    
    private static void viewOrderBook(Scanner scanner) {
        try {
            System.out.print("Enter symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            
            // Check if symbol is empty
            if (symbol.isEmpty()) {
                System.out.println("Symbol cannot be empty!");
                return;
            }
            
            if (!orderGenerator.getSymbols().contains(symbol)) {
                System.out.println("Symbol '" + symbol + "' not found!");
                System.out.println("Available symbols: " + orderGenerator.getSymbols());
                return;
            }
            
            System.out.println("\nOrder Book for " + symbol + ":");
            
            try {
                double bestBid = matchingEngine.getBestBid(symbol);
                double bestAsk = matchingEngine.getBestAsk(symbol);
                double spread = matchingEngine.getSpread(symbol);
                int bidDepth = matchingEngine.getMarketDepth(symbol, Order.Side.BUY, 5);
                int askDepth = matchingEngine.getMarketDepth(symbol, Order.Side.SELL, 5);
                
                System.out.printf("Best Bid: $%.2f%n", bestBid);
                System.out.printf("Best Ask: $%.2f%n", bestAsk);
                System.out.printf("Spread: $%.2f%n", spread);
                System.out.printf("Market Depth (Top 5 levels) - Bid side: %d%n", bidDepth);
                System.out.printf("Market Depth (Top 5 levels) - Ask side: %d%n", askDepth);
            } catch (NullPointerException e) {
                System.out.println("Error: Order book data not available for " + symbol);
            } catch (Exception e) {
                System.out.println("Error retrieving order book data: " + e.getMessage());
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid symbol format: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error viewing order book: " + e.getMessage());
        }
    }
    
    private static void adjustOrderRate(Scanner scanner) {
        try {
            System.out.print("Enter new orders per second (10-200): ");
            String rateStr = scanner.nextLine().trim();
            
            if (rateStr.isEmpty()) {
                System.out.println("Rate cannot be empty!");
                return;
            }
            
            int rate = Integer.parseInt(rateStr);
            
            if (rate >= 10 && rate <= 200) {
                if (orderGenerator.isRunning()) {
                    orderGenerator.stop();
                    orderGenerator.start(rate);
                }
                System.out.println("Order rate adjusted to " + rate + " orders/sec");
            } else {
                System.out.println("Rate must be between 10 and 200");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format: Please enter a valid integer");
        } catch (Exception e) {
            System.out.println("Error adjusting rate: " + e.getMessage());
        }
    }
    
    private static void toggleOrderGenerator() {
        try {
            if (orderGenerator.isRunning()) {
                orderGenerator.stop();
                System.out.println("Order generator stopped");
            } else {
                orderGenerator.start(50);
                System.out.println("Order generator started (50 orders/sec)");
            }
        } catch (IllegalStateException e) {
            System.out.println("Cannot toggle order generator: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error toggling order generator: " + e.getMessage());
        }
    }
    
    private static void shutdown() {
        try {
            if (orderGenerator != null) {
                orderGenerator.stop();
            }
            if (matchingEngine != null) {
                matchingEngine.shutdown();
            }
            System.out.println("System shutdown complete.");
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}
