package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * US30 60秒图表交易决策引擎
 * 在每根K线的第45秒分析下一根K线趋势预测
 */
public class SimpleDecisionEngine extends DecisionEngine {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private LocalDateTime lastCandleTime;
    private int candleCheckCounter = 0;
    private double[] priceHistory;
    private int priceHistoryIndex = 0;
    
    public SimpleDecisionEngine(TradingConfig config, MT5Connector connector) {
        super(config, connector);
        priceHistory = new double[config.getPriceHistorySize()];
        initializeScheduler();
    }
    
    private void initializeScheduler() {
        // 每秒检查一次当前时间，在第45秒触发分析
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            int second = now.getSecond();
            
            // 在第45秒触发分析
            if (second == config.getCandleAnalysisSecond()) {
                analyzeNextCandleTrend(now);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void analyzeNewCandle(CandleData candle) {
        LocalDateTime candleTime = candle.getTime();
        System.out.println("[决策引擎] 新K线开始 - 时间: " + 
                         candleTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                         ", 开盘价: " + candle.getOpen());
        
        lastCandleTime = candleTime;
        candleCheckCounter = 0;
        
        // 存储开盘价作为分析基础
        if (priceHistoryIndex < priceHistory.length) {
            priceHistory[priceHistoryIndex] = candle.getOpen();
            priceHistoryIndex++;
        } else {
            // 滚动更新价格历史
            System.arraycopy(priceHistory, 1, priceHistory, 0, priceHistory.length - 1);
            priceHistory[priceHistory.length - 1] = candle.getOpen();
        }
    }
    
    /**
     * 在每根K线的第45秒分析下一根K线趋势
     */
    private void analyzeNextCandleTrend(LocalDateTime currentTime) {
        if (lastCandleTime == null) {
            System.out.println("[决策引擎] ⏳ 等待第一根完整K线数据...");
            return;
        }
        
        // 计算当前K线已过去的时间（秒）
        long secondsSinceCandleStart = java.time.Duration.between(lastCandleTime, currentTime).getSeconds();
        
        if (secondsSinceCandleStart < config.getCandleAnalysisSecond() || secondsSinceCandleStart >= config.getTimeframe()) {
            return; // 不在分析时间或K线已结束
        }
        
        candleCheckCounter++;
        System.out.println("\n[决策引擎] 🔍 第" + config.getCandleAnalysisSecond() + "秒趋势分析 (检查#" + candleCheckCounter + ")");
        System.out.println("[决策引擎] 当前K线开始时间: " + 
                         lastCandleTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println("[决策引擎] 分析时间: " + 
                         currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        try {
            // 获取当前实时价格
            double currentPrice = connector.getCurrentPrice(config.getSymbol());
            System.out.println("[决策引擎] 当前实时价格: " + currentPrice);
            
            // 分析下一根K线可能的趋势
            String trendPrediction = predictNextCandleTrend(currentPrice);
            
            // 基于趋势预测执行交易决策
            executeDecisionBasedOnTrend(trendPrediction, currentPrice);
            
        } catch (Exception e) {
            System.err.println("[决策引擎] ❌ 趋势分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 预测下一根K线趋势
     */
    private String predictNextCandleTrend(double currentPrice) {
        // 简单的趋势预测逻辑，您可以根据需要扩展
        double averagePrice = calculateAveragePrice();
        double priceChange = currentPrice - averagePrice;
        double percentageChange = (priceChange / averagePrice) * 100;
        
        System.out.println("[决策引擎] 平均参考价: " + averagePrice);
        System.out.println("[决策引擎] 价格变化: " + priceChange + " (" + String.format("%.2f", percentageChange) + "%)");
        
        // 使用配置的阈值
        if (percentageChange > config.getStrongBullishThreshold()) { // 上涨超过阈值
            return "STRONG_BULLISH";
        } else if (percentageChange > config.getBullishThreshold()) { // 上涨超过阈值
            return "BULLISH";
        } else if (percentageChange < config.getStrongBearishThreshold()) { // 下跌超过阈值
            return "STRONG_BEARISH";
        } else if (percentageChange < config.getBearishThreshold()) { // 下跌超过阈值
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }
    
    /**
     * 基于趋势预测执行交易决策
     */
    private void executeDecisionBasedOnTrend(String trendPrediction, double currentPrice) {
        String symbol = config.getSymbol();
        double volume = config.getTradeVolume(); // 使用配置的交易量
        
        switch (trendPrediction) {
            case "STRONG_BULLISH":
                System.out.println("[决策引擎] 📈📈 预测: 下一根K线强烈看涨");
                System.out.println("[决策引擎] 💡 决策: 执行买入订单");
                executeTrade(symbol, "BUY", volume, currentPrice);
                break;
                
            case "BULLISH":
                System.out.println("[决策引擎] 📈 预测: 下一根K线看涨");
                // 可以设置更保守的参数或添加额外条件
                if (currentPrice > calculateAveragePrice()) {
                    System.out.println("[决策引擎] 💡 决策: 价格高于均线，执行买入");
                    executeTrade(symbol, "BUY", volume * 0.5, currentPrice);
                } else {
                    System.out.println("[决策引擎] ⏸️ 决策: 观望等待更好入场点");
                }
                break;
                
            case "STRONG_BEARISH":
                System.out.println("[决策引擎] 📉📉 预测: 下一根K线强烈看跌");
                System.out.println("[决策引擎] 💡 决策: 执行卖出订单");
                executeTrade(symbol, "SELL", volume, currentPrice);
                break;
                
            case "BEARISH":
                System.out.println("[决策引擎] 📉 预测: 下一根K线看跌");
                if (currentPrice < calculateAveragePrice()) {
                    System.out.println("[决策引擎] 💡 决策: 价格低于均线，执行卖出");
                    executeTrade(symbol, "SELL", volume * 0.5, currentPrice);
                } else {
                    System.out.println("[决策引擎] ⏸️ 决策: 观望等待更好入场点");
                }
                break;
                
            case "NEUTRAL":
                System.out.println("[决策引擎] ➖ 预测: 下一根K线震荡");
                System.out.println("[决策引擎] ⏸️ 决策: 保持观望");
                break;
        }
    }
    
    /**
     * 获取当前实时价格
     */
    private double getCurrentPrice() {
        try {
            return connector.getCurrentPrice(config.getSymbol());
        } catch (Exception e) {
            System.err.println("[决策引擎] 获取实时价格失败，使用默认值");
            return 35000.0;
        }
    }
    
    /**
     * 计算平均价格（用于趋势分析）
     */
    private double calculateAveragePrice() {
        double sum = 0;
        int count = 0;
        for (double price : priceHistory) {
            if (price > 0) {
                sum += price;
                count++;
            }
        }
        return count > 0 ? sum / count : getCurrentPrice();
    }
    
    /**
     * 重写的执行交易方法，包含当前价格
     */
    private void executeTrade(String symbol, String action, double volume, double currentPrice) {
        System.out.println("[决策引擎] 执行" + action + "交易: " + symbol + 
                         " 数量: " + volume + " 价格: " + currentPrice);
        
        // 在测试模式下，只打印日志，不真正执行
        if (config.isTestMode()) {
            System.out.println("[决策引擎] 🧪 测试模式：模拟交易执行");
            System.out.println("[决策引擎] 📊 交易详情:");
            System.out.println("   品种: " + symbol);
            System.out.println("   方向: " + action);
            System.out.println("   手数: " + volume);
            System.out.println("   入场价: " + currentPrice);
            System.out.println("[决策引擎] 实际交易已跳过 (测试模式)");
        } else {
            // 实际执行交易
            try {
                // 设置止损止盈（使用配置的点数）
                double stopLoss = action.equals("BUY") ? 
                    currentPrice - config.getStopLossPips() : 
                    currentPrice + config.getStopLossPips();
                double takeProfit = action.equals("BUY") ? 
                    currentPrice + config.getTakeProfitPips() : 
                    currentPrice - config.getTakeProfitPips();
                
                connector.sendOrder(symbol, action, volume, currentPrice, stopLoss, takeProfit);
                System.out.println("[决策引擎] ✅ 交易指令已发送");
                System.out.println("[决策引擎] 🛡️ 止损: " + stopLoss + " 🎯 止盈: " + takeProfit);
            } catch (Exception e) {
                System.err.println("[决策引擎] ❌ 交易发送失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void executeTrade(String symbol, String action, double volume) {
        // 调用重载版本，使用当前价格
        executeTrade(symbol, action, volume, getCurrentPrice());
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}