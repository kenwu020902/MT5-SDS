package com.mt5trading;

import com.mt5trading.mt5.connector.MT5WebSocketClient;
import com.mt5trading.config.TradingConfig;
import com.mt5trading.config.RiskConfig;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.models.CandleData;
import java.util.Properties;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("MT5 Stock Decision System");
        System.out.println("========================================");
        
        try {
            // Load configuration
            Properties props = new Properties();
            InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties");
            
            if (input == null) {
                System.out.println("ERROR: Cannot find application.properties");
                System.out.println("Please create src/main/resources/application.properties");
                System.out.println("See README.md for configuration details");
                return;
            }
            
            props.load(input);
            System.out.println("Configuration loaded successfully");
            System.out.println("Symbol: " + props.getProperty("mt5.symbol", "Not configured"));
            System.out.println("WebSocket URL: " + props.getProperty("mt5.websocket.url", "Not configured"));
            
            // Create configuration objects
            TradingConfig tradingConfig = new TradingConfig();
            RiskConfig riskConfig = new RiskConfig();
            
            // Configure from properties
            tradingConfig.setSymbol(props.getProperty("mt5.symbol", "US30"));
            tradingConfig.setTimeframe(props.getProperty("mt5.timeframe", "PERIOD_H1"));
            tradingConfig.setUseMACDConfirmation(Boolean.parseBoolean(props.getProperty("trading.useMACDConfirmation", "true")));
            tradingConfig.setUseStrictConfirmation(Boolean.parseBoolean(props.getProperty("trading.useStrictConfirmation", "true")));
            
            riskConfig.setRiskPercentage(Double.parseDouble(props.getProperty("trading.risk.percentage", "2.0")));
            
            // Create DecisionEngine (you'll need to implement this)
            DecisionEngine decisionEngine = new DecisionEngine(tradingConfig, riskConfig);
            
            // Consumer for new candle data
            Consumer<CandleData> onNewCandle = candle -> {
                System.out.println("New candle: " + candle.getTime() + 
                                 " O:" + candle.getOpen() + 
                                 " H:" + candle.getHigh() + 
                                 " L:" + candle.getLow() + 
                                 " C:" + candle.getClose());
            };
            
            // Create and start WebSocket client
            System.out.println("Starting MT5 WebSocket client...");
            String websocketUrl = props.getProperty("mt5.websocket.url", "ws://localhost:8080");
            MT5WebSocketClient client = new MT5WebSocketClient(
                new URI(websocketUrl),
                tradingConfig,
                onNewCandle,
                decisionEngine
            );
            
            // Connect to MT5
            client.connect();
            System.out.println("Connected to MT5. System is running...");
            System.out.println("Press Ctrl+C to stop");
            
            // Keep the program running
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("Error starting system: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
