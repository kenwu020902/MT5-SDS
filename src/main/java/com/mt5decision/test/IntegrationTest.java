package main.java.com.mt5decision.test;

import com.mt5decision.core.models.*;
import com.mt5decision.core.services.*;
import com.mt5decision.core.config.*;
import java.util.List;

public class IntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== Integration Test ===\n");
        
        // 1. Create configuration
        TradingConfig config = new TradingConfig();
        config.setSymbol("US30");
        config.setUseStrictConfirmation(false);
        config.setUseMACDConfirmation(true);
        config.setCheckMarketStructure(true);
        
        RiskConfig riskConfig = new RiskConfig();
        riskConfig.setAccountBalance(10000.0);
        riskConfig.setRiskPerTrade(0.02);
        
        // 2. Get mock data
        List<CandleData> candles = MockDataProvider.getUS30Candles(50, "UPTREND");
        
        if (candles.size() < 2) {
            System.out.println("Insufficient data for testing");
            return;
        }
        
        CandleData previousCandle = candles.get(1);
        CandleData currentCandle = candles.get(0);
        
        System.out.println("Previous Candle:");
        System.out.println("  Open: " + previousCandle.getOpen());
        System.out.println("  High: " + previousCandle.getHigh());
        System.out.println("  Low: " + previousCandle.getLow());
        System.out.println("  Close: " + previousCandle.getClose());
        System.out.println("  Is Bullish: " + previousCandle.isBullish());
        
        System.out.println("\nCurrent Candle:");
        System.out.println("  Open: " + currentCandle.getOpen());
        System.out.println("  Is New Candle: " + 
            currentCandle.getTime().isAfter(previousCandle.getTime()));
        
        // 3. Calculate MACD
        MACDCalculator macdCalculator = new MACDCalculator();
        MACDData macdData = macdCalculator.calculateMACD(candles);
        
        if (macdData != null) {
            System.out.println("\nMACD Analysis:");
            System.out.println("  MACD Line: " + macdData.getMacdLine());
            System.out.println("  Signal Line: " + macdData.getSignalLine());
            System.out.println("  Is Bullish: " + macdData.isBullish());
            System.out.println("  Is Bearish: " + macdData.isBearish());
        }
        
        // 4. Analyze trend
        TrendAnalyzer analyzer = new TrendAnalyzer(
            config.isUseStrictConfirmation(),
            config.isUseMACDConfirmation()
        );
        
        TrendDirection trend;
        if (config.isCheckMarketStructure()) {
            List<CandleData> recentCandles = candles.subList(0, Math.min(10, candles.size()));
            trend = analyzer.confirmTrendWithStructure(
                previousCandle, currentCandle, recentCandles, macdData
            );
        } else {
            trend = analyzer.confirmTrend(previousCandle, currentCandle, macdData);
        }
        
        System.out.println("\nTrend Analysis Result:");
        System.out.println("  Trend Direction: " + trend);
        
        // 5. Create decision
        if (trend != TrendDirection.NONE) {
            System.out.println("\nDecision Making:");
            System.out.println("  Would execute: " + trend);
            
            // Simulate position sizing
            double entryPrice = currentCandle.getOpen();
            double stopLoss = trend == TrendDirection.UPTREND 
                ? previousCandle.getLow() - riskConfig.getStopLossBuffer()
                : previousCandle.getHigh() + riskConfig.getStopLossBuffer();
            
            double positionSize = riskConfig.calculatePositionSize(entryPrice, stopLoss);
            
            System.out.println("  Entry Price: " + entryPrice);
            System.out.println("  Stop Loss: " + stopLoss);
            System.out.println("  Position Size: " + positionSize + " lots");
            System.out.println("  Risk Amount: $" + riskConfig.calculateRiskAmount());
            
            // Create order decision
            OrderDecision decision;
            if (trend == TrendDirection.UPTREND) {
                double takeProfit = entryPrice + (entryPrice - stopLoss) * config.getRiskRewardRatio();
                decision = OrderDecision.buy(
                    config.getSymbol(),
                    entryPrice,
                    stopLoss,
                    takeProfit,
                    positionSize,
                    "Uptrend confirmed with MACD alignment"
                );
            } else {
                double takeProfit = entryPrice - (stopLoss - entryPrice) * config.getRiskRewardRatio();
                decision = OrderDecision.sell(
                    config.getSymbol(),
                    entryPrice,
                    stopLoss,
                    takeProfit,
                    positionSize,
                    "Downtrend confirmed with MACD alignment"
                );
            }
            
            System.out.println("\nGenerated Order Decision:");
            System.out.println("  " + decision);
        }
        
        System.out.println("\n=== Integration Test Complete ===");
    }
}