package com.mt5trading.models;

import java.time.LocalDateTime;

/**
 * 订单信息模型类
 * 对应MT5中的订单/挂单信息
 */
public class OrderInfo {
    
    // 订单唯一标识
    private int ticket;
    
    // 交易品种
    private String symbol;
    
    // 订单类型
    // BUY, SELL, BUY_LIMIT, SELL_LIMIT, BUY_STOP, SELL_STOP
    private String type;
    
    // 交易手数
    private double volume;
    
    // 订单价格
    private double price;
    
    // 止损价格
    private double stopLoss;
    
    // 止盈价格
    private double takeProfit;
    
    // 订单注释
    private String comment;
    
    // 订单创建时间
    private LocalDateTime timeSetup;
    
    // 订单过期时间
    private LocalDateTime timeExpiration;
    
    // 订单状态
    // PENDING, OPEN, CLOSED, DELETED
    private String status;
    
    // 魔术码（用于标识EA）
    private int magicNumber;
    
    // 当前盈亏
    private double profit;
    
    // 持仓方向（用于区分多空）
    private String positionType;
    
    // 构造函数
    public OrderInfo() {
        this.timeSetup = LocalDateTime.now();
        this.status = "PENDING";
        this.magicNumber = 0;
    }
    
    public OrderInfo(int ticket, String symbol, String type, double volume, double price) {
        this();
        this.ticket = ticket;
        this.symbol = symbol;
        this.type = type;
        this.volume = volume;
        this.price = price;
    }
    
    // Getter 和 Setter 方法
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
        // 自动设置持仓方向
        if (type != null) {
            if (type.contains("BUY")) {
                this.positionType = "LONG";
            } else if (type.contains("SELL")) {
                this.positionType = "SHORT";
            }
        }
    }
    
    public double getVolume() {
        return volume;
    }
    
    public void setVolume(double volume) {
        this.volume = volume;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
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
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public LocalDateTime getTimeSetup() {
        return timeSetup;
    }
    
    public void setTimeSetup(LocalDateTime timeSetup) {
        this.timeSetup = timeSetup;
    }
    
    public LocalDateTime getTimeExpiration() {
        return timeExpiration;
    }
    
    public void setTimeExpiration(LocalDateTime timeExpiration) {
        this.timeExpiration = timeExpiration;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getMagicNumber() {
        return magicNumber;
    }
    
    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }
    
    public double getProfit() {
        return profit;
    }
    
    public void setProfit(double profit) {
        this.profit = profit;
    }
    
    public String getPositionType() {
        return positionType;
    }
    
    public void setPositionType(String positionType) {
        this.positionType = positionType;
    }
    
    // 业务方法
    
    /**
     * 判断是否为挂单
     */
    public boolean isPendingOrder() {
        return "PENDING".equals(status) && 
               (type.contains("LIMIT") || type.contains("STOP"));
    }
    
    /**
     * 判断是否为市价单
     */
    public boolean isMarketOrder() {
        return "OPEN".equals(status) && 
               ("BUY".equals(type) || "SELL".equals(type));
    }
    
    /**
     * 判断是否为买单
     */
    public boolean isBuyOrder() {
        return type != null && type.contains("BUY");
    }
    
    /**
     * 判断是否为卖单
     */
    public boolean isSellOrder() {
        return type != null && type.contains("SELL");
    }
    
    /**
     * 获取订单方向（简化版）
     */
    public String getDirection() {
        if (isBuyOrder()) return "BUY";
        if (isSellOrder()) return "SELL";
        return "UNKNOWN";
    }
    
    /**
     * 计算订单价值（USD）
     */
    public double calculateOrderValue(double currentPrice) {
        // 简化的计算：点数 × 每点价值 × 手数
        // US30: 每点 ≈ $1，每标准手 = $10/点
        double pointValue = 10.0; // 每标准手每点的美元价值
        double points = Math.abs(currentPrice - price);
        return points * pointValue * volume;
    }
    
    /**
     * 计算保证金要求
     */
    public double calculateMarginRequired() {
        // 简化计算：手数 × 合约大小 × 保证金比例
        // US30 标准合约：1标准手 = $10/点，保证金比例约1%
        double contractSize = 10.0;
        double marginRate = 0.01; // 1%
        return volume * contractSize * 100 * marginRate; // 100是点值基数
    }
    
    /**
     * 检查订单是否过期
     */
    public boolean isExpired() {
        if (timeExpiration == null) return false;
        return LocalDateTime.now().isAfter(timeExpiration);
    }
    
    /**
     * 获取订单摘要信息
     */
    public String getSummary() {
        return String.format("Order#%d %s %s %.2f @ %.5f %s", 
            ticket, symbol, type, volume, price, status);
    }
    
    @Override
    public String toString() {
        return String.format(
            "OrderInfo{ticket=%d, symbol='%s', type='%s', volume=%.2f, price=%.5f, " +
            "stopLoss=%.5f, takeProfit=%.5f, status='%s', comment='%s'}",
            ticket, symbol, type, volume, price, stopLoss, takeProfit, status, comment
        );
    }
    
    /**
     * 转换为CSV格式
     */
    public String toCsv() {
        return String.format("%d,%s,%s,%.2f,%.5f,%.5f,%.5f,%s,%s",
            ticket, symbol, type, volume, price, stopLoss, takeProfit,
            status, comment != null ? comment : "");
    }
    
    /**
     * 从CSV字符串创建订单
     */
    public static OrderInfo fromCsv(String csvLine) {
        try {
            String[] parts = csvLine.split(",");
            if (parts.length < 9) return null;
            
            OrderInfo order = new OrderInfo();
            order.setTicket(Integer.parseInt(parts[0]));
            order.setSymbol(parts[1]);
            order.setType(parts[2]);
            order.setVolume(Double.parseDouble(parts[3]));
            order.setPrice(Double.parseDouble(parts[4]));
            order.setStopLoss(Double.parseDouble(parts[5]));
            order.setTakeProfit(Double.parseDouble(parts[6]));
            order.setStatus(parts[7]);
            order.setComment(parts[8]);
            
            return order;
        } catch (Exception e) {
            System.err.println("解析CSV失败: " + e.getMessage());
            return null;
        }
    }
    
    // Builder 模式（可选）
    public static class Builder {
        private final OrderInfo order;
        
        public Builder() {
            order = new OrderInfo();
        }
        
        public Builder ticket(int ticket) {
            order.setTicket(ticket);
            return this;
        }
        
        public Builder symbol(String symbol) {
            order.setSymbol(symbol);
            return this;
        }
        
        public Builder type(String type) {
            order.setType(type);
            return this;
        }
        
        public Builder volume(double volume) {
            order.setVolume(volume);
            return this;
        }
        
        public Builder price(double price) {
            order.setPrice(price);
            return this;
        }
        
        public Builder stopLoss(double stopLoss) {
            order.setStopLoss(stopLoss);
            return this;
        }
        
        public Builder takeProfit(double takeProfit) {
            order.setTakeProfit(takeProfit);
            return this;
        }
        
        public Builder comment(String comment) {
            order.setComment(comment);
            return this;
        }
        
        public Builder magicNumber(int magicNumber) {
            order.setMagicNumber(magicNumber);
            return this;
        }
        
        public OrderInfo build() {
            return order;
        }
    }
    
    // 使用示例
    public static void main(String[] args) {
        // 使用构造函数
        OrderInfo order1 = new OrderInfo(123456, "US30", "BUY_LIMIT", 0.1, 35000.0);
        order1.setStopLoss(34950.0);
        order1.setTakeProfit(35100.0);
        order1.setComment("User manual order");
        
        // 使用Builder模式
        OrderInfo order2 = new OrderInfo.Builder()
            .ticket(123457)
            .symbol("US30")
            .type("SELL_LIMIT")
            .volume(0.2)
            .price(35100.0)
            .stopLoss(35150.0)
            .takeProfit(34950.0)
            .comment("System approved")
            .magicNumber(999)
            .build();
        
        System.out.println("Order 1: " + order1.getSummary());
        System.out.println("Order 2: " + order2.getSummary());
        System.out.println("Is order1 pending? " + order1.isPendingOrder());
        System.out.println("Is order2 sell order? " + order2.isSellOrder());
    }
}