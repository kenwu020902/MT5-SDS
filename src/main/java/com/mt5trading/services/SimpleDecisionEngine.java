package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;

/**
 * 简单的交易决策引擎实现
 * 用于模拟测试
 */
public class SimpleDecisionEngine extends DecisionEngine {
    
    public SimpleDecisionEngine(TradingConfig config, MT5Connector connector) {
        super(config, connector);
    }
    
    @Override
    public void analyzeNewCandle(CandleData candle) {
        System.out.println("[决策引擎] 分析K线 - 时间: " + candle.getTime() + 
                         ", 收盘价: " + candle.getClose());
        
        // 简单的决策逻辑：如果收盘价高于1.10200则买入，低于1.09800则卖出
        double currentPrice = candle.getClose();
        
        if (currentPrice > 1.10200) {
            System.out.println("[决策引擎] 📈 检测到买入信号 (价格 > 1.10200)");
            executeTrade(config.getSymbol(), "BUY", 0.1);
        } else if (currentPrice < 1.09800) {
            System.out.println("[决策引擎] 📉 检测到卖出信号 (价格 < 1.09800)");
            executeTrade(config.getSymbol(), "SELL", 0.1);
        } else {
            System.out.println("[决策引擎] ⏸️ 价格在区间内，保持观望");
        }
    }
    
    @Override
    public void executeTrade(String symbol, String action, double volume) {
        System.out.println("[决策引擎] 执行" + action + "交易: " + symbol + " 数量: " + volume);
        
        // 在测试模式下，只打印日志，不真正执行
        if (config.isTestMode()) {
            System.out.println("[决策引擎] 🧪 测试模式：模拟交易执行");
            System.out.println("[决策引擎] 实际交易已跳过 (app.test.mode=true)");
        } else {
            // 实际执行交易
            try {
                connector.sendOrder(symbol, action, volume, 0, 0, 0);
                System.out.println("[决策引擎] ✅ 交易指令已发送");
            } catch (Exception e) {
                System.err.println("[决策引擎] ❌ 交易发送失败: " + e.getMessage());
            }
        }
    }
}