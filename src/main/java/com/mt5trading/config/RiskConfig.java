package com.mt5trading.config;

public class RiskConfig {
    private double accountBalance;
    private double maxRiskPerTrade;
    private double maxDailyLoss;
    private double maxConcurrentTrades;
    private boolean useFixedPositionSize;
    private double fixedPositionSize;
    private double volatilityMultiplier;
    
    public RiskConfig() {
        // Default values
        this.maxRiskPerTrade = 0.02; // 2%
        this.maxDailyLoss = 0.05; // 5%
        this.maxConcurrentTrades = 3;
        this.useFixedPositionSize = false;
        this.fixedPositionSize = 0.1; // Standard lot size
        this.volatilityMultiplier = 1.0;
    }
    
    // Getters and Setters
    public double getAccountBalance() { return accountBalance; }
    public void setAccountBalance(double accountBalance) { this.accountBalance = accountBalance; }
    
    public double getMaxRiskPerTrade() { return maxRiskPerTrade; }
    public void setMaxRiskPerTrade(double maxRiskPerTrade) { this.maxRiskPerTrade = maxRiskPerTrade; }
    
    public double getMaxDailyLoss() { return maxDailyLoss; }
    public void setMaxDailyLoss(double maxDailyLoss) { this.maxDailyLoss = maxDailyLoss; }
    
    public double getMaxConcurrentTrades() { return maxConcurrentTrades; }
    public void setMaxConcurrentTrades(double maxConcurrentTrades) { this.maxConcurrentTrades = maxConcurrentTrades; }
    
    public boolean isUseFixedPositionSize() { return useFixedPositionSize; }
    public void setUseFixedPositionSize(boolean useFixedPositionSize) { this.useFixedPositionSize = useFixedPositionSize; }
    
    public double getFixedPositionSize() { return fixedPositionSize; }
    public void setFixedPositionSize(double fixedPositionSize) { this.fixedPositionSize = fixedPositionSize; }
    
    public double getVolatilityMultiplier() { return volatilityMultiplier; }
    public void setVolatilityMultiplier(double volatilityMultiplier) { this.volatilityMultiplier = volatilityMultiplier; }
    
    // Risk calculation methods
    public double calculatePositionSize(double entryPrice, double stopLoss, double currentBalance) {
        if (useFixedPositionSize) {
            return fixedPositionSize;
        }
        
        // Calculate risk amount based on percentage
        double riskAmount = currentBalance * maxRiskPerTrade;
        
        // Calculate price distance to stop loss
        double priceDistance = Math.abs(entryPrice - stopLoss);
        
        if (priceDistance == 0) {
            return fixedPositionSize;
        }
        
        // Calculate position size (simplified calculation)
        double positionSize = (riskAmount / priceDistance) * volatilityMultiplier;
        
        // Apply maximum position size limit
        try {
            TradingConfig tradingConfig = TradingConfig.load();
            return Math.min(positionSize, tradingConfig.getMaxPositionSize());
        } catch (Exception e) {
            return Math.min(positionSize, 1000); // Fallback max size
        }
    }
    
    public boolean isDailyLossLimitExceeded(double dailyProfitLoss) {
        double maxLossAmount = accountBalance * maxDailyLoss;
        return dailyProfitLoss < -maxLossAmount;
    }
    
    public boolean canOpenNewTrade(int currentOpenTrades) {
        return currentOpenTrades < maxConcurrentTrades;
    }
    
    public double calculateStopLossDistance(double entryPrice, double volatility) {
        // Adjust stop loss based on volatility
        try {
            TradingConfig config = TradingConfig.load();
            return config.getStopLossPips() * volatility * volatilityMultiplier;
        } catch (Exception e) {
            return 50; // Default 50 pips
        }
    }
    
    @Override
    public String toString() {
        return String.format("RiskConfig[MaxRisk: %.1f%%, MaxDailyLoss: %.1f%%, ConcurrentTrades: %.0f]",
                maxRiskPerTrade * 100, maxDailyLoss * 100, maxConcurrentTrades);
    }
}