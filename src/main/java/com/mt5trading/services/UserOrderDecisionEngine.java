package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.models.OrderInfo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 用户订单检测与延迟执行决策引擎
 * 1. 检测用户手动下的订单
 * 2. 在K线第45秒分析趋势
 * 3. 决定是否执行用户订单
 */
public class UserOrderDecisionEngine extends DecisionEngine {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private LocalDateTime lastCandleTime;
    private final Map<Integer, PendingUserOrder> pendingOrders = new ConcurrentHashMap<>();
    private final Map<Integer, OrderInfo> activeSystemOrders = new ConcurrentHashMap<>();
    private double[] priceHistory = new double[10];
    private int priceHistoryIndex = 0;
    private boolean isAnalyzing = false;
    
    // 等待执行的用户订单
    private static class PendingUserOrder {
        OrderInfo order;
        LocalDateTime detectedTime;
        LocalDateTime candleTime; // 订单被检测时的K线时间
        boolean approved = false;
        
        PendingUserOrder(OrderInfo order, LocalDateTime detectedTime, LocalDateTime candleTime) {
            this.order = order;
            this.detectedTime = detectedTime;
            this.candleTime = candleTime;
        }
    }
    
    public UserOrderDecisionEngine(TradingConfig config, MT5Connector connector) {
        super(config, connector);
        initializeServices();
    }
    
    private void initializeServices() {
        System.out.println("[系统] 启动用户订单监控与延迟执行系统");
        
        // 1. 每3秒扫描一次用户订单
        scheduler.scheduleAtFixedRate(this::scanUserOrders, 0, config.getOrderScanInterval(), TimeUnit.SECONDS);
        
        // 2. 每秒检查时间，在第45秒分析趋势
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            int second = now.getSecond();
            
            if (second == config.getCandleAnalysisSecond() && !isAnalyzing) {
                analyzeAndDecide(now);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // 3. 每10秒清理过期订单
        scheduler.scheduleAtFixedRate(this::cleanupExpiredOrders, 1, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 扫描用户手动下的订单
     */
    private void scanUserOrders() {
        try {
            List<OrderInfo> allOrders = connector.getPendingOrders();
            
            for (OrderInfo order : allOrders) {
                // 检查是否为新的用户挂单
                if (isNewUserOrder(order) && !pendingOrders.containsKey(order.getTicket())) {
                    LocalDateTime now = LocalDateTime.now();
                    
                    System.out.println("\n[订单检测] 🔍 发现用户手动订单!");
                    System.out.println("    订单号: " + order.getTicket());
                    System.out.println("    品种: " + order.getSymbol());
                    System.out.println("    类型: " + order.getType());
                    System.out.println("    手数: " + order.getVolume());
                    System.out.println("    价格: " + order.getPrice());
                    System.out.println("    时间: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    
                    // 添加到待处理列表
                    PendingUserOrder pendingOrder = new PendingUserOrder(order, now, lastCandleTime);
                    pendingOrders.put(order.getTicket(), pendingOrder);
                    
                    System.out.println("[订单处理] ⏸️ 订单已暂存，等待第" + config.getCandleAnalysisSecond() + "秒趋势分析...");
                    
                    // 根据配置，可以选择立即暂停用户订单
                    if (config.isAutoPauseOrders()) {
                        pauseUserOrder(order);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[订单扫描] ❌ 错误: " + e.getMessage());
        }
    }
    
    /**
     * 判断是否为新的用户订单
     */
    private boolean isNewUserOrder(OrderInfo order) {
        // 排除系统订单（通过注释标记）
        if (order.getComment() != null && 
            (order.getComment().contains("AUTO") || 
             order.getComment().contains("SYSTEM"))) {
            return false;
        }
        
        // 只关注特定品种
        if (!order.getSymbol().equals(config.getSymbol())) {
            return false;
        }
        
        // 检查是否已在活跃系统订单中
        if (activeSystemOrders.containsKey(order.getTicket())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 暂停用户订单（可选功能）
     */
    private void pauseUserOrder(OrderInfo order) {
        try {
            // 修改订单状态为暂停/等待
            boolean success = connector.modifyOrder(
                order.getTicket(), 
                order.getSymbol(), 
                order.getType(), 
                order.getVolume(), 
                order.getPrice(), 
                order.getStopLoss(), 
                order.getTakeProfit(), 
                "PAUSED_BY_SYSTEM"
            );
            
            if (success) {
                System.out.println("[订单处理] ⏸️ 用户订单已暂停，等待系统决策");
            }
        } catch (Exception e) {
            System.err.println("[订单处理] 暂停订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 在第45秒分析趋势并决策
     */
    private void analyzeAndDecide(LocalDateTime analysisTime) {
        isAnalyzing = true;
        
        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("[趋势分析] 🕐 第" + config.getCandleAnalysisSecond() + "秒分析开始");
            System.out.println("[分析时间] " + analysisTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            if (pendingOrders.isEmpty()) {
                System.out.println("[趋势分析] 📭 没有待处理的用户订单");
                return;
            }
            
            // 获取当前价格用于分析
            double currentPrice = connector.getCurrentPrice(config.getSymbol());
            System.out.println("[市场价格] 当前价: " + currentPrice);
            
            // 分析下一根K线趋势
            String trendPrediction = predictNextCandleTrend(currentPrice);
            System.out.println("[趋势预测] " + getTrendDescription(trendPrediction));
            
            // 对每个待处理订单做出决策
            Iterator<Map.Entry<Integer, PendingUserOrder>> iterator = pendingOrders.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<Integer, PendingUserOrder> entry = iterator.next();
                int ticket = entry.getKey();
                PendingUserOrder pendingOrder = entry.getValue();
                OrderInfo order = pendingOrder.order;
                
                System.out.println("\n[订单决策] 处理订单 #" + ticket);
                System.out.println("    方向: " + order.getType());
                System.out.println("    价格: " + order.getPrice());
                
                // 决定是否执行
                boolean shouldExecute = shouldExecuteOrder(order, trendPrediction, currentPrice);
                
                if (shouldExecute) {
                    executePendingOrder(pendingOrder, trendPrediction);
                    iterator.remove(); // 从待处理列表移除
                } else {
                    cancelOrKeepOrder(pendingOrder, trendPrediction);
                    iterator.remove(); // 从待处理列表移除
                }
            }
            
        } catch (Exception e) {
            System.err.println("[趋势分析] ❌ 分析失败: " + e.getMessage());
        } finally {
            isAnalyzing = false;
        }
    }
    
    /**
     * 预测下一根K线趋势
     */
    private String predictNextCandleTrend(double currentPrice) {
        double averagePrice = calculateAveragePrice();
        double priceChange = currentPrice - averagePrice;
        double percentageChange = (priceChange / averagePrice) * 100;
        
        System.out.println("[技术分析] 平均价: " + averagePrice + 
                         " | 变化: " + String.format("%.2f", percentageChange) + "%");
        
        // 使用配置的阈值
        if (percentageChange > config.getStrongBullishThreshold()) return "STRONG_BULLISH";
        if (percentageChange > config.getBullishThreshold()) return "BULLISH";
        if (percentageChange < config.getStrongBearishThreshold()) return "STRONG_BEARISH";
        if (percentageChange < config.getBearishThreshold()) return "BEARISH";
        return "NEUTRAL";
    }
    
    /**
     * 判断是否应该执行订单
     */
    private boolean shouldExecuteOrder(OrderInfo order, String trendPrediction, double currentPrice) {
        String orderType = order.getType().toUpperCase();
        
        // 检查订单方向与趋势是否一致
        switch (trendPrediction) {
            case "STRONG_BULLISH":
                return orderType.contains("BUY"); // 只执行买单
                
            case "BULLISH":
                if (orderType.contains("BUY")) {
                    // 买单：检查价格是否合适
                    double priceDiff = currentPrice - order.getPrice();
                    return priceDiff <= config.getPriceTolerance(); // 价格差不超过容忍度
                }
                return false;
                
            case "STRONG_BEARISH":
                return orderType.contains("SELL"); // 只执行卖单
                
            case "BEARISH":
                if (orderType.contains("SELL")) {
                    // 卖单：检查价格是否合适
                    double priceDiff = order.getPrice() - currentPrice;
                    return priceDiff <= config.getPriceTolerance(); // 价格差不超过容忍度
                }
                return false;
                
            case "NEUTRAL":
                // 震荡行情：检查订单是否有足够的价格优势
                if (orderType.contains("BUY")) {
                    return order.getPrice() < currentPrice - config.getNeutralBuyAdvantage();
                } else {
                    return order.getPrice() > currentPrice + config.getNeutralSellAdvantage();
                }
                
            default:
                return false;
        }
    }
    
    /**
     * 执行待处理的用户订单
     */
    private void executePendingOrder(PendingUserOrder pendingOrder, String trendPrediction) {
        OrderInfo order = pendingOrder.order;
        
        System.out.println("[订单执行] ✅ 批准执行订单 #" + order.getTicket());
        System.out.println("    原因: 订单方向与趋势预测一致 (" + trendPrediction + ")");
        
        if (config.isTestMode()) {
            System.out.println("[测试模式] 🧪 模拟执行用户订单");
            System.out.println("    品种: " + order.getSymbol());
            System.out.println("    方向: " + order.getType());
            System.out.println("    手数: " + order.getVolume());
            System.out.println("    价格: " + order.getPrice());
        } else {
            try {
                // 恢复/执行用户订单
                boolean success = connector.executeOrder(
                    order.getTicket(),
                    order.getSymbol(),
                    order.getType(),
                    order.getVolume(),
                    order.getPrice(),
                    order.getStopLoss(),
                    order.getTakeProfit(),
                    "APPROVED_BY_SYSTEM_" + trendPrediction
                );
                
                if (success) {
                    System.out.println("[订单执行] ✅ 用户订单已执行");
                    activeSystemOrders.put(order.getTicket(), order);
                } else {
                    System.out.println("[订单执行] ❌ 订单执行失败");
                }
            } catch (Exception e) {
                System.err.println("[订单执行] 错误: " + e.getMessage());
            }
        }
        
        pendingOrder.approved = true;
    }
    
    /**
     * 取消或保留订单
     */
    private void cancelOrKeepOrder(PendingUserOrder pendingOrder, String trendPrediction) {
        OrderInfo order = pendingOrder.order;
        
        if (config.isAutoCancelOrders()) {
            System.out.println("[订单处理] ❌ 取消订单 #" + order.getTicket());
            System.out.println("    原因: 订单方向与趋势预测不一致 (" + trendPrediction + ")");
            
            if (!config.isTestMode()) {
                try {
                    connector.cancelOrder(order.getTicket(), "CANCELLED_BY_SYSTEM");
                } catch (Exception e) {
                    System.err.println("[订单取消] 错误: " + e.getMessage());
                }
            }
        } else {
            System.out.println("[订单处理] ⏸️ 保留订单 #" + order.getTicket() + " (等待下一次分析)");
            // 如果不清除，订单会保留到下一次分析
        }
    }
    
    /**
     * 清理过期订单
     */
    private void cleanupExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<Integer, PendingUserOrder>> iterator = pendingOrders.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Integer, PendingUserOrder> entry = iterator.next();
            PendingUserOrder pendingOrder = entry.getValue();
            
            // 如果订单等待超过最大等待时间，清理掉
            if (pendingOrder.detectedTime.plusSeconds(config.getMaxOrderHoldTime()).isBefore(now)) {
                System.out.println("[清理] 移除过期订单 #" + pendingOrder.order.getTicket());
                iterator.remove();
            }
        }
    }
    
    @Override
    public void analyzeNewCandle(CandleData candle) {
        LocalDateTime candleTime = candle.getTime();
        System.out.println("\n[K线更新] 📊 新K线开始: " + 
                         candleTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                         " 开盘价: " + candle.getOpen());
        
        lastCandleTime = candleTime;
        
        // 更新价格历史
        if (priceHistoryIndex < priceHistory.length) {
            priceHistory[priceHistoryIndex] = candle.getOpen();
            priceHistoryIndex++;
        } else {
            System.arraycopy(priceHistory, 1, priceHistory, 0, priceHistory.length - 1);
            priceHistory[priceHistory.length - 1] = candle.getOpen();
        }
    }
    
    @Override
    public void executeTrade(String symbol, String action, double volume) {
        // 这个系统不自动创建订单，所以重写为空
        System.out.println("[注意] 本系统不自动创建订单，只管理用户订单");
    }
    
    /**
     * 获取趋势描述
     */
    private String getTrendDescription(String trend) {
        switch (trend) {
            case "STRONG_BULLISH": return "📈📈 强烈看涨";
            case "BULLISH": return "📈 看涨";
            case "STRONG_BEARISH": return "📉📉 强烈看跌";
            case "BEARISH": return "📉 看跌";
            case "NEUTRAL": return "➖ 震荡中性";
            default: return "未知";
        }
    }
    
    /**
     * 计算平均价格
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
        return count > 0 ? sum / count : connector.getCurrentPrice(config.getSymbol());
    }
    
    /**
     * 显示系统状态
     */
    public void displayStatus() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("[系统状态] 用户订单决策引擎");
        System.out.println("[待处理订单] " + pendingOrders.size() + " 个");
        System.out.println("[活跃订单] " + activeSystemOrders.size() + " 个");
        System.out.println("[当前价格] " + connector.getCurrentPrice(config.getSymbol()));
        System.out.println("[工作模式] " + (config.isTestMode() ? "测试" : "实盘"));
        System.out.println("[分析时间] 每根K线第" + config.getCandleAnalysisSecond() + "秒");
        System.out.println("=".repeat(50));
    }
    
    /**
     * 关闭系统
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
        System.out.println("[系统] 用户订单决策引擎已关闭");
    }
}