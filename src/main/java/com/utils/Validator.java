package main.java.com.utils;

import com.mt5decision.core.models.CandleData;
import com.mt5decision.core.models.OrderDecision;
import java.util.List;
import java.time.LocalDateTime;

public class Validator {
    
    // Trading parameter validation
    public static boolean isValidSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        
        // Common stock and forex symbols validation
        String pattern = "^[A-Z0-9]+(\\.[A-Z]+)?$";
        return symbol.matches(pattern);
    }
    
    public static boolean isValidTimeframe(String timeframe) {
        if (timeframe == null) {
            return false;
        }
        
        // Common MT5 timeframes
        String[] validTimeframes = {
            "M1", "M5", "M15", "M30", "H1", "H4", "D1", "W1", "MN1"
        };
        
        for (String tf : validTimeframes) {
            if (tf.equals(timeframe)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isValidPrice(double price) {
        return price > 0 && price < 1000000; // Reasonable price range
    }
    
    public static boolean isValidVolume(double volume) {
        return volume > 0 && volume <= 100; // Max 100 lots
    }
    
    public static boolean isValidStopLoss(double entryPrice, double stopLoss, boolean isBuy) {
        if (!isValidPrice(entryPrice) || !isValidPrice(stopLoss)) {
            return false;
        }
        
        if (isBuy) {
            return stopLoss < entryPrice && (entryPrice - stopLoss) <= entryPrice * 0.1; // Max 10% SL
        } else {
            return stopLoss > entryPrice && (stopLoss - entryPrice) <= entryPrice * 0.1;
        }
    }
    
    public static boolean isValidTakeProfit(double entryPrice, double takeProfit, boolean isBuy) {
        if (!isValidPrice(entryPrice) || !isValidPrice(takeProfit)) {
            return false;
        }
        
        if (isBuy) {
            return takeProfit > entryPrice && (takeProfit - entryPrice) <= entryPrice * 0.2; // Max 20% TP
        } else {
            return takeProfit < entryPrice && (entryPrice - takeProfit) <= entryPrice * 0.2;
        }
    }
    
    public static boolean isValidRiskRewardRatio(double ratio) {
        return ratio >= 1.0 && ratio <= 5.0;
    }
    
    // Candle data validation
    public static boolean isValidCandle(CandleData candle) {
        if (candle == null) {
            return false;
        }
        
        // Basic price validation
        if (!isValidPrice(candle.getOpen()) ||
            !isValidPrice(candle.getHigh()) ||
            !isValidPrice(candle.getLow()) ||
            !isValidPrice(candle.getClose())) {
            return false;
        }
        
        // High must be >= Open, Close, Low
        if (candle.getHigh() < candle.getOpen() ||
            candle.getHigh() < candle.getClose() ||
            candle.getHigh() < candle.getLow()) {
            return false;
        }
        
        // Low must be <= Open, Close, High
        if (candle.getLow() > candle.getOpen() ||
            candle.getLow() > candle.getClose() ||
            candle.getLow() > candle.getHigh()) {
            return false;
        }
        
        // Volume validation
        if (candle.getVolume() < 0) {
            return false;
        }
        
        // Time validation (not in future)
        if (candle.getTime().isAfter(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }
    
    public static boolean isValidCandleList(List<CandleData> candles, int minSize) {
        if (candles == null || candles.size() < minSize) {
            return false;
        }
        
        // Check each candle
        for (CandleData candle : candles) {
            if (!isValidCandle(candle)) {
                return false;
            }
        }
        
        // Check chronological order (most recent first)
        for (int i = 0; i < candles.size() - 1; i++) {
            if (candles.get(i).getTime().isBefore(candles.get(i + 1).getTime())) {
                return false; // Should be in descending order (most recent first)
            }
        }
        
        return true;
    }
    
    // Order decision validation
    public static ValidationResult validateOrderDecision(OrderDecision decision) {
        if (decision == null) {
            return new ValidationResult(false, "Decision is null");
        }
        
        if (!decision.isValid()) {
            return new ValidationResult(false, "Decision is not valid");
        }
        
        // Additional validation based on action
        switch (decision.getAction()) {
            case BUY:
            case SELL:
                boolean isBuy = decision.getAction() == OrderDecision.OrderAction.BUY;
                
                // Validate prices
                if (!isValidPrice(decision.getEntryPrice())) {
                    return new ValidationResult(false, "Invalid entry price");
                }
                
                if (!isValidStopLoss(decision.getEntryPrice(), decision.getStopLoss(), isBuy)) {
                    return new ValidationResult(false, "Invalid stop loss");
                }
                
                if (!isValidTakeProfit(decision.getEntryPrice(), decision.getTakeProfit(), isBuy)) {
                    return new ValidationResult(false, "Invalid take profit");
                }
                
                // Validate position size
                if (!isValidVolume(decision.getPositionSize())) {
                    return new ValidationResult(false, "Invalid position size");
                }
                
                // Validate risk/reward
                double riskReward = decision.calculateRiskRewardRatio();
                if (!isValidRiskRewardRatio(riskReward)) {
                    return new ValidationResult(false, 
                        String.format("Invalid risk/reward ratio: %.2f", riskReward));
                }
                
                break;
                
            case HOLD:
            case CLOSE:
                // For HOLD and CLOSE, just need symbol and reason
                if (decision.getSymbol() == null || decision.getSymbol().isEmpty()) {
                    return new ValidationResult(false, "Symbol is required");
                }
                if (decision.getReason() == null || decision.getReason().isEmpty()) {
                    return new ValidationResult(false, "Reason is required");
                }
                break;
        }
        
        // Confidence validation
        if (decision.getConfidence() < 0 || decision.getConfidence() > 1) {
            return new ValidationResult(false, 
                String.format("Invalid confidence: %.2f", decision.getConfidence()));
        }
        
        return new ValidationResult(true, "Decision is valid");
    }
    
    // Market data validation
    public static boolean isReasonableSpread(double spread, String symbol) {
        if (spread < 0) {
            return false;
        }
        
        // Maximum acceptable spreads based on symbol type
        if (symbol.contains("US30") || symbol.contains("DJI")) {
            return spread <= 5.0; // Max 5 points for US30
        } else if (symbol.contains("XAU") || symbol.contains("GOLD")) {
            return spread <= 1.0; // Max 1 dollar for gold
        } else if (symbol.contains("EURUSD") || symbol.contains("GBPUSD")) {
            return spread <= 0.0002; // Max 2 pips for major forex
        }
        
        return spread <= 10.0; // Default max
    }
    
    public static boolean isNormalVolatility(double currentVolatility, double averageVolatility) {
        if (currentVolatility <= 0 || averageVolatility <= 0) {
            return false;
        }
        
        // Check if current volatility is within 3 standard deviations (simplified)
        double ratio = currentVolatility / averageVolatility;
        return ratio >= 0.1 && ratio <= 10.0;
    }
    
    // Configuration validation
    public static boolean isValidRiskPercentage(double riskPercentage) {
        return riskPercentage > 0 && riskPercentage <= 5.0; // Max 5% risk per trade
    }
    
    public static boolean isValidAccountBalance(double balance) {
        return balance >= 100 && balance <= 10000000; // Reasonable range
    }
    
    // Network and connection validation
    public static boolean isValidServerAddress(String server) {
        if (server == null || server.isEmpty()) {
            return false;
        }
        
        // Simple URL validation
        String pattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*" +
                        "([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        
        return server.matches(pattern) || server.equals("localhost") || 
               server.matches("^\\d{1,3}(\\.\\d{1,3}){3}$"); // IP address
    }
    
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }
    
    // Validation result class
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }
    
    // Batch validation method
    public static ValidationResult validateTradingParameters(
            String symbol, String timeframe, double riskPercentage, 
            double accountBalance, double riskRewardRatio) {
        
        StringBuilder errors = new StringBuilder();
        
        if (!isValidSymbol(symbol)) {
            errors.append("Invalid symbol. ");
        }
        
        if (!isValidTimeframe(timeframe)) {
            errors.append("Invalid timeframe. ");
        }
        
        if (!isValidRiskPercentage(riskPercentage)) {
            errors.append("Invalid risk percentage. ");
        }
        
        if (!isValidAccountBalance(accountBalance)) {
            errors.append("Invalid account balance. ");
        }
        
        if (!isValidRiskRewardRatio(riskRewardRatio)) {
            errors.append("Invalid risk/reward ratio. ");
        }
        
        if (errors.length() > 0) {
            return new ValidationResult(false, errors.toString().trim());
        }
        
        return new ValidationResult(true, "All parameters are valid");
    }
}