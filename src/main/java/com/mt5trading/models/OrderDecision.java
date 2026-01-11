package com.mt5trading.models;

import java.time.LocalDateTime;

public class OrderDecision {
    private TrendDirection trend;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private double positionSize;
    private OrderAction action;
    private String symbol;
    private LocalDateTime decisionTime;
    private String reason;
    private double confidence;
    
    public enum OrderAction {
        BUY,
        SELL,
        HOLD,
        CLOSE
    }
    
    public OrderDecision() {
        this.decisionTime = LocalDateTime.now();
        this.confidence = 0.0;
    }
    
    // Getters and Setters
    public TrendDirection getTrend() { return trend; }
    public void setTrend(TrendDirection trend) { this.trend = trend; }
    
    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }
    
    public double getStopLoss() { return stopLoss; }
    public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
    
    public double getTakeProfit() { return takeProfit; }
    public void setTakeProfit(double takeProfit) { this.takeProfit = takeProfit; }
    
    public double getPositionSize() { return positionSize; }
    public void setPositionSize(double positionSize) { this.positionSize = positionSize; }
    
    public OrderAction getAction() { return action; }
    public void setAction(OrderAction action) { this.action = action; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public LocalDateTime getDecisionTime() { return decisionTime; }
    public void setDecisionTime(LocalDateTime decisionTime) { this.decisionTime = decisionTime; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    // Helper methods
    public boolean isBuySignal() {
        return action == OrderAction.BUY;
    }
    
    public boolean isSellSignal() {
        return action == OrderAction.SELL;
    }
    
    public boolean shouldExecute() {
        return action == OrderAction.BUY || action == OrderAction.SELL;
    }
    
    public double getRiskRewardRatio() {
        if (entryPrice == 0 || stopLoss == 0) return 0;
        double risk = Math.abs(entryPrice - stopLoss);
        double reward = Math.abs(takeProfit - entryPrice);
        return reward / risk;
    }
    
    public void validate() {
        if (entryPrice <= 0) {
            throw new IllegalStateException("Invalid entry price: " + entryPrice);
        }
        if (positionSize <= 0) {
            throw new IllegalStateException("Invalid position size: " + positionSize);
        }
        if (confidence < 0 || confidence > 1) {
            throw new IllegalStateException("Confidence must be between 0 and 1: " + confidence);
        }
    }
    
    @Override
    public String toString() {
        return String.format("OrderDecision[Action: %s, Symbol: %s, Price: %.5f, SL: %.5f, TP: %.5f, " +
                           "Size: %.2f, Confidence: %.1f%%, Reason: %s]",
                action, symbol, entryPrice, stopLoss, takeProfit, positionSize, 
                confidence * 100, reason);
    }
}