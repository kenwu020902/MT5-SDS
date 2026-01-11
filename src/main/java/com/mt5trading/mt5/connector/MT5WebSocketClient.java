package com.mt5trading.mt5.connector;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

public class MT5WebSocketClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(MT5WebSocketClient.class);
    
    private final TradingConfig config;
    private final Consumer<CandleData> onNewCandle;
    private final DecisionEngine decisionEngine;
    
    public MT5WebSocketClient(URI serverUri, TradingConfig config, 
                             Consumer<CandleData> onNewCandle, DecisionEngine decisionEngine) {
        super(serverUri);
        this.config = config;
        this.onNewCandle = onNewCandle;
        this.decisionEngine = decisionEngine;
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("WebSocket connection opened to MT5");
        
        // Subscribe to symbol updates
        String subscribeMessage = String.format(
                "{\"action\": \"subscribe\", \"symbol\": \"%s\", \"timeframe\": \"%s\"}",
                config.getSymbol(), config.getTimeframe());
        
        send(subscribeMessage);
        logger.info("Subscribed to {} on timeframe {}", config.getSymbol(), config.getTimeframe());
    }
    
    @Override
    public void onMessage(String message) {
        try {
            if (config.isEnableConsoleLogging()) {
                logger.debug("Received WebSocket message: {}", message);
            }
            
            // Parse candle data from message
            CandleData candle = parseCandleData(message);
            if (candle != null) {
                onNewCandle.accept(candle);
                
                // Trigger decision engine on new candle
                if (isNewCandle(candle)) {
                    decisionEngine.analyzeNewCandle(candle);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("WebSocket connection closed. Code: {}, Reason: {}, Remote: {}", 
                   code, reason, remote);
        
        // Attempt to reconnect
        if (remote) {
            reconnectWithDelay(5000);
        }
    }
    
    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error", ex);
    }
    
    private CandleData parseCandleData(String message) {
        try {
            // Simple JSON parsing (in real implementation, use Jackson)
            if (message.contains("\"type\":\"candle\"")) {
                // Parse candle data from JSON
                // This is a simplified parser - in production use proper JSON parsing
                String[] parts = message.split(",");
                
                double open = extractDouble(parts, "open");
                double high = extractDouble(parts, "high");
                double low = extractDouble(parts, "low");
                double close = extractDouble(parts, "close");
                long volume = extractLong(parts, "volume");
                long timestamp = extractLong(parts, "time");
                
                LocalDateTime time = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                
                return new CandleData(time, open, high, low, close, volume);
            }
        } catch (Exception e) {
            logger.error("Failed to parse candle data", e);
        }
        return null;
    }
    
    private double extractDouble(String[] parts, String key) {
        for (String part : parts) {
            if (part.contains("\"" + key + "\":")) {
                String value = part.split(":")[1].replace("\"", "").trim();
                return Double.parseDouble(value);
            }
        }
        return 0.0;
    }
    
    private long extractLong(String[] parts, String key) {
        for (String part : parts) {
            if (part.contains("\"" + key + "\":")) {
                String value = part.split(":")[1].replace("\"", "").trim();
                return Long.parseLong(value);
            }
        }
        return 0L;
    }
    
    private boolean isNewCandle(CandleData candle) {
        // Check if this is a new candle (different from previous)
        // Implementation would track previous candle time
        return true; // Simplified
    }
    
    private void reconnectWithDelay(int delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                logger.info("Attempting to reconnect...");
                reconnect();
            } catch (Exception e) {
                logger.error("Reconnection failed", e);
            }
        }).start();
    }
}