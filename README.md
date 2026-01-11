# MT5 Trading Decision System

A Java-based trading decision system for MetaTrader 5 that analyzes candlestick patterns and MACD indicators to make automated trading decisions.

## Features
- Real-time candle pattern analysis
- MACD trend confirmation
- Risk management configuration
- MT5 WebSocket integration
- Configurable confirmation rules

## Prerequisites
- Java 11+
- Maven 3.6+
- MT5 Terminal with WebSocket support

## Configuration
Edit `src/main/resources/application.properties`:
```properties
mt5.websocket.url=ws://localhost:8080
mt5.symbol=US30
mt5.timeframe=PERIOD_H1
trading.risk.percentage=2.0
trading.useStrictConfirmation=true
trading.useMACDConfirmation=true