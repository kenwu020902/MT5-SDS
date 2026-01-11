package com.mt5trading.services;

import com.mt5trading.config.RiskConfig;
import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.*;
import com.mt5trading.mt5.connector.MT5Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DecisionEngine {
    private static final Logger logger = LoggerFactory.getLogger(DecisionEngine.class);
    
    private final TradingConfig tradingConfig;
    private final RiskConfig riskConfig;
    private final MT5DataFetcher dataFetcher;
    private final TrendAnalyzer trendAnalyzer;
    private final MACDCalculator macdCalculator;
    private final MT5Connector mt5Connector;
    
    private final List<CandleData> candles;
    private final List<OrderDecision> recentDecisions;
    private LocalDateTime lastCandleTime;
    private boolean isRunning;
    private ScheduledExecutorService scheduler;
    
    public DecisionEngine(TradingConfig tradingConfig, MT5DataFetcher dataFetcher) {
        this.tradingConfig = tradingConfig;
        this.riskConfig = new RiskConfig();
        this.dataFetcher = dataFetcher;
        this.trendAnalyzer = new TrendAnalyzer(tradingConfig);
        this.macdCalculator = new MACDCalculator(
                tradingConfig.getMacdFastPeriod(),
                tradingConfig.getMacdSlowPeriod(),
                tradingConfig.getMacdSignalPeriod()
        );
        this.mt5Connector = dataFetcher.getConnector();
        
        this.candles = new ArrayList<>();
        this.recentDecisions = new ArrayList<>();
        this.lastCandleTime = null;
        this.isRunning = false;
    }
    
    public void start() {
        logger.info("Starting Decision Engine...");
        
        // 获取账户余额
        mt5Connector.getAccountBalance().thenAccept(balance -> {
            riskConfig.setAccountBalance(balance);
            logger.info("Account balance loaded: ${}", balance);
            
            // Load historical data
            dataFetcher.loadHistoricalData().thenAccept(historicalCandles -> {
                candles.clear();
                candles.addAll(historicalCandles);
                
                if (!candles.isEmpty()) {
                    lastCandleTime = candles.get(candles.size() - 1).getTime();
                    logger.info("Historical data loaded. Last candle time: {}", lastCandleTime);
                }
                
                // Start scheduled analysis
                startScheduledAnalysis();
                isRunning = true;
                
                logger.info("Decision Engine started successfully");
            }).exceptionally(e -> {
                logger.error("Failed to load historical data", e);
                return null;
            });
        }).exceptionally(e -> {
            logger.error("Failed to get account balance", e);
            return null;
        });
    }
    
    private void startScheduledAnalysis() {
        scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performAnalysis();
            } catch (Exception e) {
                logger.error("Error during scheduled analysis", e);
            }
        }, 0, tradingConfig.getPollingInterval(), TimeUnit.MILLISECONDS);
    }
    
    public void analyzeNewCandle(CandleData newCandle) {
        if (!isRunning) {
            return;
        }
        
        // Check if this is a new candle
        if (lastCandleTime == null || !newCandle.getTime().equals(lastCandleTime)) {
            logger.info("New candle detected: {}", newCandle);
            lastCandleTime = newCandle.getTime();
            
            // Add to candle list
            candles.add(newCandle);
            dataFetcher.updateCandleData(newCandle);
            
            // Perform analysis
            performAnalysis();
        }
    }
    
    private void performAnalysis() {
        if (candles.size() < 2) {
            logger.warn("Not enough data for analysis");
            return;
        }
        
        // Get current and previous candles
        CandleData previousCandle = candles.get(candles.size() - 2);
        CandleData currentCandle = candles.get(candles.size() - 1);
        
        // Analyze trend
        TrendDirection trend = trendAnalyzer.confirmTrend(previousCandle, currentCandle, candles);
        
        if (trend != TrendDirection.NONE) {
            // Make trading decision
            OrderDecision decision = makeTradingDecision(trend, currentCandle);
            
            if (decision.shouldExecute()) {
                executeDecision(decision);
            }
            
            // Log decision
            logDecision(decision);
        }
    }
    
    private OrderDecision makeTradingDecision(TrendDirection trend, CandleData currentCandle) {
        OrderDecision decision = new OrderDecision();
        decision.setSymbol(tradingConfig.getSymbol());
        decision.setTrend(trend);
        decision.setDecisionTime(LocalDateTime.now());
        
        // Calculate confidence
        double confidence = calculateConfidence(trend, currentCandle);
        decision.setConfidence(confidence);
        
        // Set order parameters
        calculateOrderParameters(decision, currentCandle);
        
        // Set action based on trend
        if (trend == TrendDirection.UPTREND) {
            decision.setAction(OrderDecision.OrderAction.BUY);
            decision.setReason("Uptrend confirmed with " + String.format("%.1f%%", confidence * 100) + " confidence");
        } else if (trend == TrendDirection.DOWNTREND) {
            decision.setAction(OrderDecision.OrderAction.SELL);
            decision.setReason("Downtrend confirmed with " + String.format("%.1f%%", confidence * 100) + " confidence");
        } else {
            decision.setAction(OrderDecision.OrderAction.HOLD);
            decision.setReason("No clear trend direction");
        }
        
        return decision;
    }
    
    private double calculateConfidence(TrendDirection trend, CandleData currentCandle) {
        double confidence = 0.5; // Base confidence
        
        // Market structure alignment
        if (trend == TrendDirection.UPTREND && 
            trendAnalyzer.isMarketStructureBullish(candles)) {
            confidence += 0.2;
        } else if (trend == TrendDirection.DOWNTREND && 
                   trendAnalyzer.isMarketStructureBearish(candles)) {
            confidence += 0.2;
        }
        
        // Volume confirmation (if available)
        if (currentCandle.getVolume() > getAverageVolume()) {
            confidence += 0.1;
        }
        
        // MACD strength
        MACDData macdData = macdCalculator.calculateCurrentMACD(candles);
        if (macdData != null) {
            double macdStrength = Math.min(Math.abs(macdData.getCrossoverValue()) * 10, 0.2);
            confidence += macdStrength;
        }
        
        // Ensure confidence is between 0 and 1
        return Math.min(Math.max(confidence, 0.0), 1.0);
    }
    
    private void calculateOrderParameters(OrderDecision decision, CandleData currentCandle) {
        double entryPrice = currentCandle.getClose(); // 使用收盘价作为入场价
        decision.setEntryPrice(entryPrice);
        
        // Calculate stop loss and take profit based on pips
        double pipValue = 0.0001; // 对于大多数货币对
        if (tradingConfig.getSymbol().contains("JPY")) {
            pipValue = 0.01; // 对于日元货币对
        }
        
        double stopLossDistance = tradingConfig.getStopLossPips() * pipValue;
        double takeProfitDistance = tradingConfig.getTakeProfitPips() * pipValue;
        
        if (decision.getTrend() == TrendDirection.UPTREND) {
            decision.setStopLoss(entryPrice - stopLossDistance);
            decision.setTakeProfit(entryPrice + takeProfitDistance);
        } else if (decision.getTrend() == TrendDirection.DOWNTREND) {
            decision.setStopLoss(entryPrice + stopLossDistance);
            decision.setTakeProfit(entryPrice - takeProfitDistance);
        }
        
        // Calculate position size
        double positionSize = riskConfig.calculatePositionSize(
                entryPrice, decision.getStopLoss(), riskConfig.getAccountBalance());
        decision.setPositionSize(positionSize);
    }
    
    private void executeDecision(OrderDecision decision) {
        try {
            decision.validate();
            
            logger.info("Executing decision: {}", decision);
            
            // 检查是否已经有类似订单
            if (hasSimilarOpenOrder(decision)) {
                logger.info("Similar order already open, skipping execution");
                return;
            }
            
            // Convert to MT5 order
            String orderType = decision.getAction() == OrderDecision.OrderAction.BUY ? "BUY" : "SELL";
            
            // 发送订单到MT5
            mt5Connector.sendOrder(
                    decision.getSymbol(),
                    orderType,
                    decision.getPositionSize(),
                    decision.getEntryPrice(),
                    decision.getStopLoss(),
                    decision.getTakeProfit()
            ).thenAccept(success -> {
                if (success) {
                    logger.info("Order executed successfully");
                    decision.setReason(decision.getReason() + " - Order executed");
                } else {
                    logger.error("Failed to execute order");
                    decision.setReason(decision.getReason() + " - Order failed");
                }
                
                // Store decision
                recentDecisions.add(decision);
                if (recentDecisions.size() > 100) {
                    recentDecisions.remove(0);
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to execute decision", e);
            decision.setReason(decision.getReason() + " - Execution error: " + e.getMessage());
        }
    }
    
    private boolean hasSimilarOpenOrder(OrderDecision decision) {
        // 简单的重复订单检查
        for (OrderDecision prevDecision : recentDecisions) {
            if (prevDecision.getSymbol().equals(decision.getSymbol()) &&
                prevDecision.getAction() == decision.getAction() &&
                Math.abs(prevDecision.getEntryPrice() - decision.getEntryPrice()) < 0.001) {
                return true;
            }
        }
        return false;
    }
    
    private void logDecision(OrderDecision decision) {
        if (tradingConfig.isEnableConsoleLogging()) {
            System.out.println("=== TRADING DECISION ===");
            System.out.println(decision);
            System.out.println("========================");
        }
        
        logger.info("Trading decision made: {}", decision);
    }
    
    private double getAverageVolume() {
        if (candles.size() < 20) {
            return 0;
        }
        
        double sum = 0;
        int startIndex = Math.max(0, candles.size() - 20);
        
        for (int i = startIndex; i < candles.size(); i++) {
            sum += candles.get(i).getVolume();
        }
        
        return sum / Math.min(candles.size(), 20);
    }
    
    public void stop() {
        logger.info("Stopping Decision Engine...");
        isRunning = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Decision Engine stopped");
    }
    
    public List<OrderDecision> getRecentDecisions() {
        return new ArrayList<>(recentDecisions);
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}