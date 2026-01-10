package main.java.com.mt5decision.core.config;

public class TradingConfig {
    private String symbol = "US30";
    private String timeframe = "M15";
    private boolean useStrictConfirmation = true;
    private boolean useMACDConfirmation = true;
    private boolean checkMarketStructure = true;
    private double riskPerTrade = 0.01; // 1% per trade
    private double riskRewardRatio = 1.5;
    private double stopLossBuffer = 5.0;
    private double accountBalance = 10000.0;
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public boolean isUseStrictConfirmation() { return useStrictConfirmation; }
    public void setUseStrictConfirmation(boolean useStrictConfirmation) { 
        this.useStrictConfirmation = useStrictConfirmation; 
    }
    
    // ... other getters and setters
}