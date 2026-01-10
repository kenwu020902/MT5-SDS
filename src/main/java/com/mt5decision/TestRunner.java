package main.java.com.mt5decision;

import com.mt5decision.core.models.*;
import com.mt5decision.core.services.*;
import com.mt5decision.core.config.*;
import com.mt5decision.utils.*;

public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== MT5 Stock Order Decision System - Test Runner ===\n");
        
        // Test 1: Trend Analyzer
        testTrendAnalyzer();
        
        // Test 2: Order Decision
        testOrderDecision();
        
        // Test 3: Risk Configuration
        testRiskConfig();
        
        // Test 4: MACD Calculator
        testMACDCalculator();
        
        // Test 5: Logger
        testLogger();
        
        // Test 6: Validator
        testValidator();
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static void testTrendAnalyzer() {
        System.out.println("1. Testing Trend Analyzer:");
        
        try {
            // Create test candles
            CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                                  java.time.LocalDateTime.now().minusMinutes(15));
            CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 1200, 
                                                  java.time.LocalDateTime.now());
            
            // Test moderate analyzer
            TrendAnalyzer analyzer = new TrendAnalyzer(false, false);
            TrendDirection result = analyzer.confirmTrend(prevCandle, currCandle, null);
            
            System.out.println("   Moderate Analyzer Result: " + result);
            System.out.println("   Expected: UPTREND");
            System.out.println("   Test Passed: " + (result == TrendDirection.UPTREND));
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testOrderDecision() {
        System.out.println("2. Testing Order Decision:");
        
        try {
            // Create buy decision
            OrderDecision buyDecision = OrderDecision.buy(
                "US30", 
                35000.50, 
                34900.00, 
                35200.00, 
                0.1, 
                "Uptrend confirmed"
            );
            
            System.out.println("   Buy Decision: " + buyDecision);
            System.out.println("   Is Valid: " + buyDecision.isValid());
            System.out.println("   Risk Amount: $" + buyDecision.calculateRiskAmount());
            System.out.println("   Risk/Reward: " + buyDecision.calculateRiskRewardRatio());
            
            // Test hold decision
            OrderDecision holdDecision = OrderDecision.hold("US30", "Waiting for confirmation");
            System.out.println("\n   Hold Decision: " + holdDecision);
            System.out.println("   Is Valid: " + holdDecision.isValid());
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testRiskConfig() {
        System.out.println("3. Testing Risk Configuration:");
        
        try {
            RiskConfig riskConfig = new RiskConfig();
            riskConfig.setAccountBalance(10000.0);
            riskConfig.setRiskPerTrade(0.02); // 2%
            
            System.out.println("   Config: " + riskConfig);
            System.out.println("   Risk Amount: $" + riskConfig.calculateRiskAmount());
            
            double entryPrice = 35000.0;
            double stopLoss = 34900.0;
            double positionSize = riskConfig.calculatePositionSize(entryPrice, stopLoss);
            
            System.out.println("   Position Size for trade: " + positionSize + " lots");
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testMACDCalculator() {
        System.out.println("4. Testing MACD Calculator:");
        
        try {
            // Create sample candles
            java.util.List<CandleData> candles = new java.util.ArrayList<>();
            java.time.LocalDateTime baseTime = java.time.LocalDateTime.now().minusHours(24);
            
            for (int i = 0; i < 50; i++) {
                double price = 35000.0 + (Math.random() * 200 - 100); // Random price around 35000
                double open = price - Math.random() * 50;
                double close = price + Math.random() * 50;
                double high = Math.max(open, close) + Math.random() * 30;
                double low = Math.min(open, close) - Math.random() * 30;
                
                candles.add(new CandleData(open, high, low, close, 
                                          (long)(1000 + Math.random() * 1000),
                                          baseTime.plusMinutes(i * 30)));
            }
            
            MACDCalculator calculator = new MACDCalculator();
            MACDData macdData = calculator.calculateMACD(candles);
            
            if (macdData != null) {
                System.out.println("   MACD Line: " + macdData.getMacdLine());
                System.out.println("   Signal Line: " + macdData.getSignalLine());
                System.out.println("   Histogram: " + macdData.getHistogram());
                System.out.println("   Is Bullish: " + macdData.isBullish());
                System.out.println("   Is Bearish: " + macdData.isBearish());
            } else {
                System.out.println("   MACD Calculation returned null");
            }
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testLogger() {
        System.out.println("5. Testing Logger:");
        
        try {
            Logger logger = Logger.getInstance();
            
            logger.setLogLevel(Logger.LogLevel.DEBUG);
            logger.setWriteToFile(false); // Don't write to file for testing
            
            logger.debug("This is a debug message");
            logger.info("This is an info message");
            logger.warn("This is a warning message");
            logger.error("This is an error message");
            logger.logTrade("US30", "BUY", 35000.50, 0.1, "Trend confirmation");
            
            System.out.println("   Logger initialized successfully");
            System.out.println("   Log level: " + logger);
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testValidator() {
        System.out.println("6. Testing Validator:");
        
        try {
            // Test symbol validation
            System.out.println("   Valid Symbol 'US30': " + Validator.isValidSymbol("US30"));
            System.out.println("   Valid Symbol 'EURUSD': " + Validator.isValidSymbol("EURUSD"));
            System.out.println("   Invalid Symbol '': " + Validator.isValidSymbol(""));
            
            // Test price validation
            System.out.println("\n   Valid Price 35000: " + Validator.isValidPrice(35000));
            System.out.println("   Invalid Price 0: " + Validator.isValidPrice(0));
            System.out.println("   Invalid Price -100: " + Validator.isValidPrice(-100));
            
            // Test order decision validation
            OrderDecision decision = OrderDecision.buy("US30", 35000, 34900, 35200, 0.1, "Test");
            Validator.ValidationResult result = Validator.validateOrderDecision(decision);
            
            System.out.println("\n   Order Decision Validation:");
            System.out.println("   Is Valid: " + result.isValid());
            System.out.println("   Message: " + result.getMessage());
            
        } catch (Exception e) {
            System.out.println("   Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}