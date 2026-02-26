# Real-Time Order Matching & Trade Analytics System

A high-performance stock exchange simulator built in Java for quantitative finance interview preparation and trading system education.

---

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [How It Works](#how-it-works)
- [Technologies Used](#technologies-used)
- [Installation & Setup](#installation--setup)
- [Usage Guide](#usage-guide)
- [Code Structure](#code-structure)
- [Key Concepts Explained](#key-concepts-explained)
- [Interview Talking Points](#interview-talking-points)
- [Future Enhancements](#future-enhancements)
- [License](#license)

---

## Overview

This project is a real-time order matching system that simulates how stock exchanges match buyers and sellers. It handles thousands of orders, maintains an order book, executes trades, and provides live analytics - similar to how NASDAQ or NYSE operates, but simplified for learning.

The system was built to understand trading system internals and to prepare for quantitative finance interviews at firms like Towers Research Capital, Jane Street, Citadel, and other trading companies.

---

## Features

### Core Trading Features
- Real-time order matching with price-time priority algorithm
- Support for LIMIT and MARKET orders
- Order book management with bid and ask levels
- Trade execution and recording
- Order cancellation functionality

### Analytics & Monitoring
- Live VWAP (Volume-Weighted Average Price) calculation
- Real-time market summary (best bid, best ask, spread)
- Trade volume and count tracking
- 24-hour price change percentage
- High and low prices tracking
- Average trade size calculation
- Market depth visualization

### Simulation & Control
- Automated order generator (configurable 10-200 orders/sec)
- 5 pre-configured stock symbols (AAPL, GOOGL, MSFT, AMZN, TSLA)
- 70% limit orders, 30% market orders for realistic simulation
- Market making orders to provide liquidity
- Interactive console menu for manual control
- Pause/resume order generation
- Manual order placement capability

---

## Architecture

The system follows a clean, modular architecture with clear separation of concerns:
+-------------------+     +-------------------+     +-------------------+
|                   |     |                   |     |                   |
|  OrderGenerator   +---->+  MatchingEngine   +---->+ AnalyticsService  |
|   (Simulator)     |     |    (Core Logic)   |     |   (Statistics)    |
|                   |     |                   |     |                   |
+-------------------+     +---------+---------+     +-------------------+
                                   |
                                   |
                                   v
                           +-----------------+
                           |                 |
                           |  OrderBook      |
                           | (Order Storage) |
                           |                 |
                           +-----------------+
### Component Breakdown
- **Model Layer**: Defines core entities (Order, Trade)
- **Engine Layer**: Contains business logic (OrderBook, MatchingEngine)
- **Analytics Layer**: Handles real-time statistics
- **Simulator Layer**: Generates test data
- **Main**: Interactive console application

---

## How It Works

### Step-by-Step Order Flow
1. **Order Creation**: An order is created (either by simulator or manual input)
2. **Submission**: Order is sent to MatchingEngine
3. **Matching**: Engine checks opposite side of order book for matches
4. **Execution**: If match found, trade is created and both orders are updated
5. **Book Update**: Remaining quantity (if any) added to order book
6. **Analytics Update**: Trade data sent to AnalyticsService for real-time metrics
7. **Display**: Updated information shown in console

### Matching Logic
- For BUY orders: Looks at SELL orders from lowest price upward
- For SELL orders: Looks at BUY orders from highest price downward
- Orders matched at the resting order's price
- Multiple matches possible until order is fully filled or no matches remain

---

## Technologies Used

### Java Features
- **Java 8+**: Core language
- **Concurrent Collections**: ConcurrentHashMap, ConcurrentSkipListSet for thread-safe operations
- **ExecutorService**: For asynchronous order processing
- **Flow API (Reactive Streams)**: For publishing trades to subscribers
- **ReentrantReadWriteLock**: For fine-grained locking control
- **AtomicBoolean**: For thread-safe flags

### Data Structures
- **ConcurrentSkipListSet**: Maintains sorted order levels (O(log n) access)
- **ConcurrentHashMap**: Fast lookup by order ID
- **LinkedList**: Maintains recent trades for analytics
- **ArrayList**: Stores orders at same price level

### Design Patterns
- **Publisher-Subscriber**: Trade distribution to analytics
- **Factory Method**: Order creation
- **Strategy**: Different matching strategies for order types
- **Observer**: Analytics updates on new trades

---

## Installation & Setup

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Git (optional, for cloning)

### Steps to Run

1. **Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/Real-Time-Order-Matching-System.git
cd Real-Time-Order-Matching-System
