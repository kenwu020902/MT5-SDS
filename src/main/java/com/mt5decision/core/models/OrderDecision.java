package main.java.com.mt5decision.core.models;

import java.time.LocalDateTime;

public class OrderDecision {
    public enum OrderAction {
        BUY,
        SELL,
        HOLD,
        CLOSE
    }
    
    private OrderAction action;
    private String symbol;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private double positionSize; // in lots
    private String reason;
    private LocalDateTime decisionTime;
    private double confidence; // 0.0 to 1.0
    
    // Private constructor
    private OrderDecision(OrderAction action, String symbol, double entryPrice, 
                         double stopLoss, double takeProfit, double positionSize, 
                         String reason, double confidence) {
        this.action = action;
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.positionSize = positionSize;
        this.reason = reason;
        this.decisionTime = LocalDateTime.now();
        this.confidence = confidence;
    }
    
    // Factory methods
    public static OrderDecision buy(String symbol, double entryPrice, double stopLoss, 
                                   double takeProfit, double positionSize, String reason) {
        return new OrderDecision(OrderAction.BUY, symbol, entryPrice, stopLoss, 
                               takeProfit, positionSize, reason, 0.8);
    }
    
    public static OrderDecision buy(String symbol, double entryPrice, double stopLoss, 
                                   double takeProfit, double positionSize, 
                                   String reason, double confidence) {
        return new OrderDecision(OrderAction.BUY, symbol, entryPrice, stopLoss, 
                               takeProfit, positionSize, reason, confidence);
    }
    
    public static OrderDecision sell(String symbol, double entryPrice, double stopLoss, 
                                    double takeProfit, double positionSize, String reason) {
        return new OrderDecision(OrderAction.SELL, symbol, entryPrice, stopLoss, 
                               takeProfit, positionSize, reason, 0.8);
    }
    
    public static OrderDecision sell(String symbol, double entryPrice, double stopLoss, 
                                    double takeProfit, double positionSize, 
                                    String reason, double confidence) {
        return new OrderDecision(OrderAction.SELL, symbol, entryPrice, stopLoss, 
                               takeProfit, positionSize, reason, confidence);
    }
    
    public static OrderDecision hold(String symbol, String reason) {
        return new OrderDecision(OrderAction.HOLD, symbol, 0, 0, 0, 0, reason, 0.5);
    }
    
    public static OrderDecision noDecision(String reason) {
        return new OrderDecision(OrderAction.HOLD, null, 0, 0, 0, 0, reason, 0.0);
    }
    
    public static OrderDecision close(String symbol, String reason) {
        return new OrderDecision(OrderAction.CLOSE, symbol, 0, 0, 0, 0, reason, 0.9);
    }
    
    // Getters
    public OrderAction getAction() { return action; }
    public String getSymbol() { return symbol; }
    public double getEntryPrice() { return entryPrice; }
    public double getStopLoss() { return stopLoss; }
    public double getTakeProfit() { return takeProfit; }
    public double getPositionSize() { return positionSize; }
    public String getReason() { return reason; }
    public LocalDateTime getDecisionTime() { return decisionTime; }
    public double getConfidence() { return confidence; }
    
    // Risk calculation methods
    public double calculateRiskAmount() {
        if (action == OrderAction.HOLD || action == OrderAction.CLOSE) {
            return 0;
        }
        return Math.abs(entryPrice - stopLoss) * positionSize;
    }
    
    public double calculatePotentialProfit() {
        if (action == OrderAction.HOLD || action == OrderAction.CLOSE) {
            return 0;
        }
        return Math.abs(takeProfit - entryPrice) * positionSize;
    }
    
    public double calculateRiskRewardRatio() {
        double risk = calculateRiskAmount();
        if (risk == 0) return 0;
        return calculatePotentialProfit() / risk;
    }
    
    public boolean isValid() {
        if (action == OrderAction.HOLD || action == OrderAction.CLOSE) {
            return symbol != null && reason != null;
        }
        
        return symbol != null && 
               entryPrice > 0 && 
               stopLoss > 0 && 
               takeProfit > 0 && 
               positionSize > 0 && 
               reason != null &&
               ((action == OrderAction.BUY && takeProfit > entryPrice && entryPrice > stopLoss) ||
                (action == OrderAction.SELL && takeProfit < entryPrice && entryPrice < stopLoss));
    }
    
    @Override
    public String toString() {
        if (action == OrderAction.HOLD || action == OrderAction.CLOSE) {
            return String.format("OrderDecision{action=%s, symbol='%s', reason='%s', time=%s}",
                    action, symbol, reason, decisionTime);
        }
        
        return String.format(
            "OrderDecision{action=%s, symbol='%s', entry=%.2f, SL=%.2f, TP=%.2f, " +
            "size=%.2f, reason='%s', confidence=%.2f, RR=%.2f, time=%s}",
            action, symbol, entryPrice, stopLoss, takeProfit, positionSize,
            reason, confidence, calculateRiskRewardRatio(), decisionTime
        );
    }
}