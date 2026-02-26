package engine;

import model.Order;
import model.Trade;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OrderBook {
    private final String symbol;
    private final NavigableSet<OrderLevel> buyLevels;
    private final NavigableSet<OrderLevel> sellLevels;
    private final Map<String, Order> orderMap;
    private final ReentrantReadWriteLock lock;
    
    public OrderBook(String symbol) {
        this.symbol = symbol;
        // Buy orders: highest price first (descending)
        this.buyLevels = new ConcurrentSkipListSet<>((l1, l2) -> Double.compare(l2.getPrice(), l1.getPrice()));
        // Sell orders: lowest price first (ascending)
        this.sellLevels = new ConcurrentSkipListSet<>(Comparator.comparingDouble(OrderLevel::getPrice));
        this.orderMap = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    public String getSymbol() { return symbol; }
    
    public void addOrder(Order order) {
        lock.writeLock().lock();
        try {
            if (!order.getSymbol().equals(symbol)) {
                throw new IllegalArgumentException("Order symbol doesn't match order book symbol");
            }
            
            orderMap.put(order.getOrderId(), order);
            NavigableSet<OrderLevel> levels = getLevels(order.getSide());
            addToLevel(levels, order);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void removeOrder(String orderId) {
        lock.writeLock().lock();
        try {
            Order order = orderMap.remove(orderId);
            if (order != null) {
                NavigableSet<OrderLevel> levels = getLevels(order.getSide());
                removeFromLevel(levels, order);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Order getOrder(String orderId) {
        lock.readLock().lock();
        try {
            return orderMap.get(orderId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public List<Trade> matchOrder(Order incomingOrder) {
        lock.writeLock().lock();
        try {
            List<Trade> trades = new ArrayList<>();
            NavigableSet<OrderLevel> opposingLevels = getLevels(incomingOrder.getSide().equals(Order.Side.BUY) ? 
                Order.Side.SELL : Order.Side.BUY);
            
            int remainingQty = incomingOrder.getRemainingQuantity();
            
            // For market orders or limit orders that can cross the spread
            Iterator<OrderLevel> levelIterator = opposingLevels.iterator();
            while (levelIterator.hasNext() && remainingQty > 0) {
                OrderLevel level = levelIterator.next();
                
                // For limit orders, check if price crosses
                if (incomingOrder.getOrderType() == Order.OrderType.LIMIT) {
                    if (incomingOrder.getSide() == Order.Side.BUY && level.getPrice() > incomingOrder.getPrice()) {
                        break; // Best sell price is too high for buy order
                    }
                    if (incomingOrder.getSide() == Order.Side.SELL && level.getPrice() < incomingOrder.getPrice()) {
                        break; // Best buy price is too low for sell order
                    }
                }
                
                // Match at this price level
                List<Order> ordersAtLevel = level.getOrders();
                Iterator<Order> orderIterator = ordersAtLevel.iterator();
                
                while (orderIterator.hasNext() && remainingQty > 0) {
                    Order restingOrder = orderIterator.next();
                    int matchQty = Math.min(remainingQty, restingOrder.getRemainingQuantity());
                    
                    // Create trade
                    Trade trade;
                    if (incomingOrder.getSide() == Order.Side.BUY) {
                        trade = new Trade(incomingOrder.getOrderId(), restingOrder.getOrderId(), 
                                        symbol, level.getPrice(), matchQty);
                    } else {
                        trade = new Trade(restingOrder.getOrderId(), incomingOrder.getOrderId(), 
                                        symbol, level.getPrice(), matchQty);
                    }
                    trades.add(trade);
                    
                    // Update orders
                    restingOrder.fill(matchQty);
                    incomingOrder.fill(matchQty);
                    remainingQty -= matchQty;
                    
                    // Remove fully filled resting orders
                    if (restingOrder.getStatus() == Order.OrderStatus.FILLED) {
                        orderIterator.remove();
                        orderMap.remove(restingOrder.getOrderId());
                    }
                }
                
                // Remove empty levels
                if (ordersAtLevel.isEmpty()) {
                    levelIterator.remove();
                }
            }
            
            // Add remaining quantity of incoming order to book if it's a limit order
            if (remainingQty > 0 && incomingOrder.getOrderType() == Order.OrderType.LIMIT) {
                addOrder(incomingOrder);
            }
            
            return trades;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private NavigableSet<OrderLevel> getLevels(Order.Side side) {
        return side == Order.Side.BUY ? buyLevels : sellLevels;
    }
    
    private void addToLevel(NavigableSet<OrderLevel> levels, Order order) {
        Optional<OrderLevel> existingLevel = levels.stream()
                .filter(level -> level.getPrice() == order.getPrice())
                .findFirst();
        
        if (existingLevel.isPresent()) {
            existingLevel.get().addOrder(order);
        } else {
            OrderLevel newLevel = new OrderLevel(order.getPrice());
            newLevel.addOrder(order);
            levels.add(newLevel);
        }
    }
    
    private void removeFromLevel(NavigableSet<OrderLevel> levels, Order order) {
        levels.stream()
                .filter(level -> level.getPrice() == order.getPrice())
                .findFirst()
                .ifPresent(level -> {
                    level.removeOrder(order.getOrderId());
                    if (level.getOrders().isEmpty()) {
                        levels.remove(level);
                    }
                });
    }
    
    public double getBestBid() {
        lock.readLock().lock();
        try {
            return buyLevels.isEmpty() ? 0.0 : buyLevels.first().getPrice();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public double getBestAsk() {
        lock.readLock().lock();
        try {
            return sellLevels.isEmpty() ? 0.0 : sellLevels.first().getPrice();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getVolumeAtPrice(Order.Side side, double price) {
        lock.readLock().lock();
        try {
            NavigableSet<OrderLevel> levels = getLevels(side);
            return levels.stream()
                    .filter(level -> level.getPrice() == price)
                    .findFirst()
                    .map(OrderLevel::getTotalVolume)
                    .orElse(0);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Inner class for price levels
    private static class OrderLevel {
        private final double price;
        private final List<Order> orders;
        
        public OrderLevel(double price) {
            this.price = price;
            this.orders = new ArrayList<>();
        }
        
        public double getPrice() { return price; }
        public List<Order> getOrders() { return orders; }
        
        public void addOrder(Order order) {
            orders.add(order);
        }
        
        public void removeOrder(String orderId) {
            orders.removeIf(o -> o.getOrderId().equals(orderId));
        }
        
        public int getTotalVolume() {
            return orders.stream().mapToInt(Order::getRemainingQuantity).sum();
        }
    }
}
