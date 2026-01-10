package main.java.com.mt5decision;

import com.mt5decision.core.config.TradingConfig;
import com.mt5decision.core.services.DecisionEngine;
import com.mt5decision.core.models.OrderDecision;

public class Main {
    public static void main(String[] args) {
        // Configure trading parameters
        TradingConfig config = new TradingConfig();
        config.setSymbol("US30");
        config.setUseStrictConfirmation(true);
        config.setUseMACDConfirmation(true);
        config.setRiskPerTrade(0.02); // 2% risk per trade
        config.setRiskRewardRatio(2.0);
        
        // Initialize decision engine
        DecisionEngine engine = new DecisionEngine(config);
        
        // Make trading decision
        OrderDecision decision = engine.makeDecision();
        
        // Output decision
        System.out.println("Trading Decision:");
        System.out.println("Action: " + decision.getAction());
        System.out.println("Symbol: " + decision.getSymbol());
        System.out.println("Entry: " + decision.getEntryPrice());
        System.out.println("Stop Loss: " + decision.getStopLoss());
        System.out.println("Take Profit: " + decision.getTakeProfit());
        System.out.println("Reason: " + decision.getReason());
        
        // Send order to MT5 if decision is not NONE
        if (decision.getAction() != OrderAction.NONE) {
            sendOrderToMT5(decision);
        }
    }
    
    private static void sendOrderToMT5(OrderDecision decision) {
        // Implementation to send order to MT5 platform
        // This would use MT5 Java API or REST API
    }
}