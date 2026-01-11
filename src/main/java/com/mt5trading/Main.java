package com.mt5trading;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.MT5DataFetcher;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("MT5 Trading Decision System v1.0");
        System.out.println("=================================");
        
        try {
            // Load configuration
            TradingConfig config = TradingConfig.load();
            System.out.println("Configuration loaded:");
            System.out.println("  Symbol: " + config.getSymbol());
            System.out.println("  Timeframe: " + config.getTimeframe());
            System.out.println("  MT5 API URL: " + config.getMt5ApiUrl());
            
            // Initialize MT5 connector
            MT5Connector connector = new MT5Connector(config);
            
            // Initialize data fetcher
            MT5DataFetcher dataFetcher = new MT5DataFetcher(connector);
            
            // Initialize decision engine
            DecisionEngine decisionEngine = new DecisionEngine(config, dataFetcher);
            
            // Start the system
            decisionEngine.start();
            
            // Keep the application running
            System.out.println("\nSystem is running. Press Ctrl+C to stop.");
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                decisionEngine.stop();
                connector.disconnect();
                System.out.println("System stopped.");
            }));
            
            // Wait indefinitely
            Thread.currentThread().join();
            
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Application interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}