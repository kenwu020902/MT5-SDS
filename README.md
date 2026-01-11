# MT5 Trading Decision System

A Java-based trading decision system for MetaTrader 5 that analyzes candlestick patterns and MACD indicators to make automated trading decisions.

## Features
- Real-time candle pattern analysis
- MACD trend confirmation
- Risk management configuration
- MT5 WebSocket/REST API integration
- Configurable confirmation rules
- Automated order execution
- Risk management and position sizing

## Prerequisites
- Java 11+
- Maven 3.6+
- MT5 Terminal with WebSocket/REST API support

## Configuration
Edit `src/main/resources/application.properties`:

```properties
# MT5 API Configuration
mt5.api.url=http://localhost:8080/api
mt5.websocket.url=ws://localhost:8080
mt5.login=1234567
mt5.password=your_password
mt5.server=YourBrokerServer

# Trading Parameters
mt5.symbol=EURUSD
mt5.timeframe=PERIOD_H1
trading.risk.percentage=2.0
trading.useStrictConfirmation=true
trading.useMACDConfirmation=true