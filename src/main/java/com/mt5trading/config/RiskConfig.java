package main.java.com.mt5trading.core.config;

public class RiskConfig {
    private double accountBalance;
    private double riskPerTrade; // Percentage per trade (e.g., 0.01 = 1%)
    private double maxPositionSize;
    private double stopLossPercentage;
    private double takeProfitPercentage;
    private double riskRewardRatio;
    private double stopLossBuffer; // Buffer for stop loss in points
    
    public RiskConfig() {
        // Default values
        this.accountBalance = 10000.0;
        this.riskPerTrade = 0.02; // 2% per trade
        this.maxPositionSize = 1.0; // Maximum lots
        this.stopLossPercentage = 0.02; // 2% stop loss
        this.takeProfitPercentage = 0.04; // 4% take profit
        this.riskRewardRatio = 2.0;
        this.stopLossBuffer = 5.0;
    }
    
    public RiskConfig(double accountBalance, double riskPerTrade, double maxPositionSize,
                     double stopLossPercentage, double takeProfitPercentage, 
                     double riskRewardRatio, double stopLossBuffer) {
        this.accountBalance = accountBalance;
        this.riskPerTrade = riskPerTrade;
        this.maxPositionSize = maxPositionSize;
        this.stopLossPercentage = stopLossPercentage;
        this.takeProfitPercentage = takeProfitPercentage;
        this.riskRewardRatio = riskRewardRatio;
        this.stopLossBuffer = stopLossBuffer;
    }
    
    // Getters
    public double getAccountBalance() { return accountBalance; }
    public double getRiskPerTrade() { return riskPerTrade; }
    public double getMaxPositionSize() { return maxPositionSize; }
    public double getStopLossPercentage() { return stopLossPercentage; }
    public double getTakeProfitPercentage() { return takeProfitPercentage; }
    public double getRiskRewardRatio() { return riskRewardRatio; }
    public double getStopLossBuffer() { return stopLossBuffer; }
    
    // Setters
    public void setAccountBalance(double accountBalance) { 
        this.accountBalance = accountBalance; 
    }
    public void setRiskPerTrade(double riskPerTrade) { 
        if (riskPerTrade >= 0 && riskPerTrade <= 0.1) { // Max 10% risk
            this.riskPerTrade = riskPerTrade; 
        }
    }
    public void setMaxPositionSize(double maxPositionSize) { 
        this.maxPositionSize = maxPositionSize; 
    }
    public void setStopLossPercentage(double stopLossPercentage) { 
        this.stopLossPercentage = stopLossPercentage; 
    }
    public void setTakeProfitPercentage(double takeProfitPercentage) { 
        this.takeProfitPercentage = takeProfitPercentage; 
    }
    public void setRiskRewardRatio(double riskRewardRatio) { 
        this.riskRewardRatio = riskRewardRatio; 
    }
    public void setStopLossBuffer(double stopLossBuffer) { 
        this.stopLossBuffer = stopLossBuffer; 
    }
    
    // Helper methods
    public double calculateRiskAmount() {
        return accountBalance * riskPerTrade;
    }
    
    public double calculatePositionSize(double entryPrice, double stopLossPrice) {
        double riskAmount = calculateRiskAmount();
        double riskPerUnit = Math.abs(entryPrice - stopLossPrice);
        
        if (riskPerUnit == 0) {
            return 0;
        }
        
        double positionSize = riskAmount / riskPerUnit;
        
        // Apply maximum position size constraint
        return Math.min(positionSize, maxPositionSize);
    }
    
    public double calculateStopLoss(double entryPrice, boolean isBuy) {
        double stopLossDistance = entryPrice * stopLossPercentage;
        return isBuy ? entryPrice - stopLossDistance : entryPrice + stopLossDistance;
    }
    
    public double calculateTakeProfit(double entryPrice, double stopLoss, boolean isBuy) {
        double risk = Math.abs(entryPrice - stopLoss);
        double reward = risk * riskRewardRatio;
        return isBuy ? entryPrice + reward : entryPrice - reward;
    }
    
    @Override
    public String toString() {
        return String.format(
            "RiskConfig{accountBalance=%.2f, riskPerTrade=%.2f%%, maxPositionSize=%.2f, " +
            "stopLoss=%.2f%%, takeProfit=%.2f%%, riskReward=%.2f, buffer=%.2f}",
            accountBalance, riskPerTrade * 100, maxPositionSize,
            stopLossPercentage * 100, takeProfitPercentage * 100,
            riskRewardRatio, stopLossBuffer
        );
    }
}
