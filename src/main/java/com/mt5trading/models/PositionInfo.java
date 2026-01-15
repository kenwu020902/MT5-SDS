package com.mt5trading.models;

import java.time.LocalDateTime;

/**
 * 持仓信息模型类
 */
public class PositionInfo {
    
    private int ticket;          // 持仓单号
    private String symbol;       // 交易品种
    private String type;         // 持仓类型：BUY, SELL
    private double volume;       // 手数
    private double openPrice;    // 开仓价
    private LocalDateTime openTime; // 开仓时间
    private double stopLoss;     // 止损价
    private double takeProfit;   // 止盈价
    private double currentPrice; // 当前价
    private double profit;       // 当前盈亏
    private double swap;         // 隔夜利息
    private double commission;   // 佣金
    private String comment;      // 注释
    private int magicNumber;     // 魔术码
    
    // 构造函数
    public PositionInfo() {
        this.openTime = LocalDateTime.now();
    }
    
    // Getter 和 Setter
    public int getTicket() {
        return ticket;
    }
    
    public void setTicket(int ticket) {
        this.ticket = ticket;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public double getVolume() {
        return volume;
    }
    
    public void setVolume(double volume) {
        this.volume = volume;
    }
    
    public double getOpenPrice() {
        return openPrice;
    }
    
    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }
    
    public LocalDateTime getOpenTime() {
        return openTime;
    }
    
    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }
    
    public double getStopLoss() {
        return stopLoss;
    }
    
    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }
    
    public double getTakeProfit() {
        return takeProfit;
    }
    
    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getProfit() {
        return profit;
    }
    
    public void setProfit(double profit) {
        this.profit = profit;
    }
    
    public double getSwap() {
        return swap;
    }
    
    public void setSwap(double swap) {
        this.swap = swap;
    }
    
    public double getCommission() {
        return commission;
    }
    
    public void setCommission(double commission) {
        this.commission = commission;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public int getMagicNumber() {
        return magicNumber;
    }
    
    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }
    
    // 业务方法
    
    /**
     * 计算当前盈亏
     */
    public void calculateProfit() {
        if (currentPrice == 0 || openPrice == 0) return;
        
        double points = currentPrice - openPrice;
        double pointValue = 10.0; // US30每点价值
        
        if ("SELL".equals(type)) {
            points = openPrice - currentPrice;
        }
        
        this.profit = points * pointValue * volume;
    }
    
    /**
     * 计算浮动盈亏百分比
     */
    public double getProfitPercentage() {
        if (openPrice == 0) return 0;
        
        double priceChange = Math.abs(currentPrice - openPrice);
        return (priceChange / openPrice) * 100;
    }
    
    /**
     * 计算到止损的距离（点数）
     */
    public double getDistanceToStopLoss() {
        if (stopLoss == 0) return 0;
        
        if ("BUY".equals(type)) {
            return openPrice - stopLoss;
        } else {
            return stopLoss - openPrice;
        }
    }
    
    /**
     * 计算到止盈的距离（点数）
     */
    public double getDistanceToTakeProfit() {
        if (takeProfit == 0) return 0;
        
        if ("BUY".equals(type)) {
            return takeProfit - openPrice;
        } else {
            return openPrice - takeProfit;
        }
    }
    
    /**
     * 计算风险回报比
     */
    public double getRiskRewardRatio() {
        double risk = getDistanceToStopLoss();
        double reward = getDistanceToTakeProfit();
        
        if (risk == 0) return 0;
        return reward / risk;
    }
    
    /**
     * 获取持仓时长（分钟）
     */
    public long getHoldDurationMinutes() {
        return java.time.Duration.between(openTime, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * 判断是否为亏损持仓
     */
    public boolean isLossPosition() {
        return profit < 0;
    }
    
    /**
     * 判断是否为盈利持仓
     */
    public boolean isProfitPosition() {
        return profit > 0;
    }
    
    /**
     * 获取持仓摘要
     */
    public String getSummary() {
        return String.format("Position#%d %s %s %.2f @ %.5f P&L: %.2f", 
            ticket, symbol, type, volume, openPrice, profit);
    }
    
    @Override
    public String toString() {
        return String.format(
            "PositionInfo{ticket=%d, symbol='%s', type='%s', volume=%.2f, " +
            "openPrice=%.5f, currentPrice=%.5f, profit=%.2f}",
            ticket, symbol, type, volume, openPrice, currentPrice, profit
        );
    }
}