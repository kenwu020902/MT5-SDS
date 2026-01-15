package com.mt5trading.mt5.connector;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.CandleData;
import com.mt5trading.models.OrderInfo;
import com.mt5trading.services.DecisionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MT5Connector {
    private static final Logger logger = LoggerFactory.getLogger(MT5Connector.class);
    
    private final TradingConfig config;
    private MT5WebSocketClient websocketClient;
    private final ScheduledExecutorService scheduler;
    private boolean webSocketConnected;
    
    // 订单管理相关字段
    private final Map<Integer, OrderInfo> activeOrders = new ConcurrentHashMap<>();
    private final Map<Integer, OrderInfo> pendingUserOrders = new ConcurrentHashMap<>();
    private Consumer<OrderInfo> onNewUserOrder;
    private boolean isMonitoringOrders = false;
    
    public MT5Connector(TradingConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.webSocketConnected = false;
    }
    
    /**
     * 关闭连接
     */
    public void close() {
        disconnect();
    }
    
    /**
     * 获取历史数据（支持整数时间框架）
     */
    public List<CandleData> getHistoricalData(String symbol, int timeframeSeconds, int bars) {
        logger.info("获取历史数据: {} {}秒 {} bars", symbol, timeframeSeconds, bars);
        
        // 将秒转换为MT5时间框架字符串
        String mt5Timeframe = convertSecondsToMT5Timeframe(timeframeSeconds);
        return getHistoricalData(symbol, mt5Timeframe, bars);
    }
    
    /**
     * 转换秒数为MT5时间框架字符串
     */
    private String convertSecondsToMT5Timeframe(int seconds) {
        switch (seconds) {
            case 1: return "PERIOD_M1";
            case 60: return "PERIOD_M1";
            case 300: return "PERIOD_M5";
            case 900: return "PERIOD_M15";
            case 1800: return "PERIOD_M30";
            case 3600: return "PERIOD_H1";
            case 14400: return "PERIOD_H4";
            case 86400: return "PERIOD_D1";
            case 604800: return "PERIOD_W1";
            default: 
                logger.warn("未知的时间框架: {}秒，使用PERIOD_M1", seconds);
                return "PERIOD_M1";
        }
    }
    
    /**
     * 执行订单（用于批准执行用户订单）
     */
    public boolean executeOrder(int ticket, String symbol, String type, double volume,
                               double price, double stopLoss, double takeProfit, String comment) {
        logger.info("执行订单 #{}", ticket);
        
        if (!webSocketConnected) {
            logger.warn("WebSocket未连接，模拟执行订单");
            addMockOrderToActive(ticket, symbol, type, volume, price, stopLoss, takeProfit, comment);
            return true;
        }
        
        try {
            // 构建执行订单的JSON消息
            String orderJson = String.format(
                "{\"type\":\"execute_order\",\"ticket\":%d,\"symbol\":\"%s\",\"action\":\"%s\"," +
                "\"volume\":%.2f,\"price\":%.5f,\"sl\":%.5f,\"tp\":%.5f,\"comment\":\"%s\",\"timestamp\":%d}",
                ticket, symbol, type, volume, price, stopLoss, takeProfit, comment, System.currentTimeMillis()
            );
            
            websocketClient.send(orderJson);
            logger.info("订单执行指令已发送: {}", orderJson);
            
            // 从待处理列表移除，添加到活跃列表
            OrderInfo pendingOrder = pendingUserOrders.remove(ticket);
            if (pendingOrder != null) {
                pendingOrder.setStatus("EXECUTED");
                pendingOrder.setComment(comment);
                pendingOrder.setTimeSetup(LocalDateTime.now());
                activeOrders.put(ticket, pendingOrder);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("执行订单失败", e);
            return false;
        }
    }
    
    /**
     * 发送交易指令
     */
    public CompletableFuture<Boolean> sendOrder(String symbol, String orderType, 
                                              double volume, double price, 
                                              double stopLoss, double takeProfit, String comment) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        if (!webSocketConnected || websocketClient == null) {
            logger.error("WebSocket未连接，无法发送订单");
            result.complete(false);
            return result;
        }
        
        try {
            // 构建订单JSON
            String orderJson = String.format(
                "{\"type\":\"trade\",\"action\":\"%s\",\"symbol\":\"%s\",\"volume\":%.2f," +
                "\"price\":%.5f,\"sl\":%.5f,\"tp\":%.5f,\"comment\":\"%s\",\"timestamp\":%d}",
                orderType, symbol, volume, price, stopLoss, takeProfit, comment, System.currentTimeMillis()
            );
            
            websocketClient.send(orderJson);
            logger.info("交易指令已发送: {}", orderJson);
            
            // 记录系统订单
            int ticket = generateMockTicket();
            OrderInfo systemOrder = new OrderInfo();
            systemOrder.setTicket(ticket);
            systemOrder.setSymbol(symbol);
            systemOrder.setType(orderType);
            systemOrder.setVolume(volume);
            systemOrder.setPrice(price);
            systemOrder.setStopLoss(stopLoss);
            systemOrder.setTakeProfit(takeProfit);
            systemOrder.setComment(comment);
            systemOrder.setTimeSetup(LocalDateTime.now());
            systemOrder.setStatus("PENDING");
            
            activeOrders.put(ticket, systemOrder);
            
            result.complete(true);
            
        } catch (Exception e) {
            logger.error("订单发送失败", e);
            result.complete(false);
        }
        
        return result;
    }
    
    /**
     * 获取模拟的待处理订单
     */
    private List<OrderInfo> getMockPendingOrders() {
        List<OrderInfo> orders = new ArrayList<>();
        
        // 随机生成1-3个模拟订单
        int numOrders = new Random().nextInt(3) + 1;
        
        for (int i = 0; i < numOrders; i++) {
            OrderInfo order = new OrderInfo();
            order.setTicket(100000 + new Random().nextInt(900000));
            order.setSymbol(config.getSymbol());
            
            // 随机选择订单类型
            String[] types = {"BUY_LIMIT", "SELL_LIMIT", "BUY_STOP", "SELL_STOP"};
            order.setType(types[new Random().nextInt(types.length)]);
            
            order.setVolume(0.1 + new Random().nextDouble() * 0.9);
            order.setPrice(getBasePrice(config.getSymbol()) + (new Random().nextDouble() * 100 - 50));
            order.setStopLoss(order.getPrice() * (order.getType().contains("BUY") ? 0.995 : 1.005));
            order.setTakeProfit(order.getPrice() * (order.getType().contains("BUY") ? 1.010 : 0.990));
            order.setComment("USER_ORDER_" + System.currentTimeMillis());
            order.setTimeSetup(LocalDateTime.now().minusSeconds(new Random().nextInt(30)));
            order.setStatus("PENDING");
            
            orders.add(order);
        }
        
        return orders;
    }
    
    /**
     * 添加模拟活跃订单
     */
    private void addMockOrderToActive(int ticket, String symbol, String type, double volume,
                                     double price, double stopLoss, double takeProfit, String comment) {
        OrderInfo order = new OrderInfo();
        order.setTicket(ticket);
        order.setSymbol(symbol);
        order.setType(type);
        order.setVolume(volume);
        order.setPrice(price);
        order.setStopLoss(stopLoss);
        order.setTakeProfit(takeProfit);
        order.setComment(comment);
        order.setTimeSetup(LocalDateTime.now());
        order.setStatus("EXECUTED");
        
        activeOrders.put(ticket, order);
        pendingUserOrders.remove(ticket);
    }
    
    /**
     * 获取所有待处理订单（挂单）
     */
    public List<OrderInfo> getPendingOrders() {
        logger.info("获取待处理订单列表");
        
        if (!webSocketConnected) {
            logger.warn("WebSocket未连接，返回模拟订单数据");
            return getMockPendingOrders();
        }
        
        try {
            // 通过WebSocket发送获取订单请求
            String request = String.format(
                "{\"type\":\"get_orders\",\"symbol\":\"%s\",\"status\":\"pending\",\"timestamp\":%d}",
                config.getSymbol(), System.currentTimeMillis()
            );
            
            websocketClient.send(request);
            
            return getMockPendingOrders();
            
        } catch (Exception e) {
            logger.error("获取待处理订单失败", e);
            return getMockPendingOrders();
        }
    }
    
    /**
     * 获取所有持仓订单
     */
    public List<OrderInfo> getOpenPositions() {
        logger.info("获取持仓订单列表");
        
        try {
            // 通过WebSocket发送获取持仓请求
            String request = String.format(
                "{\"type\":\"get_positions\",\"symbol\":\"%s\",\"timestamp\":%d}",
                config.getSymbol(), System.currentTimeMillis()
            );
            
            if (webSocketConnected && websocketClient != null) {
                websocketClient.send(request);
            }
            
            return new ArrayList<>(activeOrders.values());
            
        } catch (Exception e) {
            logger.error("获取持仓失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 修改订单（用于暂停/恢复订单）
     */
    public boolean modifyOrder(int ticket, String symbol, String type, double volume, 
                              double price, double stopLoss, double takeProfit, String comment) {
        logger.info("修改订单 #{}", ticket);
        
        if (!webSocketConnected) {
            logger.warn("WebSocket未连接，模拟修改订单");
            return true;
        }
        
        try {
            // 构建修改订单的JSON消息
            String orderJson = String.format(
                "{\"type\":\"modify_order\",\"ticket\":%d,\"symbol\":\"%s\",\"action\":\"%s\"," +
                "\"volume\":%.2f,\"price\":%.5f,\"sl\":%.5f,\"tp\":%.5f,\"comment\":\"%s\",\"timestamp\":%d}",
                ticket, symbol, type, volume, price, stopLoss, takeProfit, comment, System.currentTimeMillis()
            );
            
            websocketClient.send(orderJson);
            logger.info("订单修改指令已发送: {}", orderJson);
            
            // 更新本地订单信息
            OrderInfo order = findOrderByTicket(ticket);
            if (order != null) {
                order.setComment(comment);
                order.setStatus("MODIFIED");
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("修改订单失败", e);
            return false;
        }
    }
    
    /**
     * 取消订单
     */
    public boolean cancelOrder(int ticket, String reason) {
        logger.info("取消订单 #{}，原因: {}", ticket, reason);
        
        if (!webSocketConnected) {
            logger.warn("WebSocket未连接，模拟取消订单");
            return true;
        }
        
        try {
            // 构建取消订单的JSON消息
            String cancelJson = String.format(
                "{\"type\":\"cancel_order\",\"ticket\":%d,\"reason\":\"%s\",\"timestamp\":%d}",
                ticket, reason, System.currentTimeMillis()
            );
            
            websocketClient.send(cancelJson);
            logger.info("订单取消指令已发送");
            
            // 从本地列表中移除
            pendingUserOrders.remove(ticket);
            activeOrders.remove(ticket);
            
            return true;
            
        } catch (Exception e) {
            logger.error("取消订单失败", e);
            return false;
        }
    }
    
    /**
     * 开始监控用户订单
     */
    public void startOrderMonitoring(Consumer<OrderInfo> onNewUserOrderCallback) {
        if (isMonitoringOrders) {
            logger.warn("订单监控已经在运行中");
            return;
        }
        
        this.onNewUserOrder = onNewUserOrderCallback;
        this.isMonitoringOrders = true;
        
        // 定期扫描订单
        scheduler.scheduleAtFixedRate(() -> {
            if (webSocketConnected) {
                scanForNewUserOrders();
            }
        }, 0, config.getOrderScanInterval(), TimeUnit.SECONDS);
        
        logger.info("用户订单监控已启动，扫描间隔: {}秒", config.getOrderScanInterval());
    }
    
    /**
     * 扫描新的用户订单
     */
    private void scanForNewUserOrders() {
        try {
            List<OrderInfo> pendingOrders = getPendingOrders();
            
            for (OrderInfo order : pendingOrders) {
                // 检查是否为新的用户订单
                if (!this.pendingUserOrders.containsKey(order.getTicket()) && 
                    !this.activeOrders.containsKey(order.getTicket()) &&
                    isUserOrder(order)) {
                    
                    logger.info("检测到新的用户订单: {}", order);
                    
                    // 添加到待处理列表
                    this.pendingUserOrders.put(order.getTicket(), order);
                    
                    // 回调通知
                    if (onNewUserOrder != null) {
                        onNewUserOrder.accept(order);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("扫描用户订单失败", e);
        }
    }
    
    /**
     * 判断是否为用户订单（非系统订单）
     */
    private boolean isUserOrder(OrderInfo order) {
        // 排除系统订单（通过注释判断）
        if (order.getComment() != null) {
            String comment = order.getComment().toUpperCase();
            if (comment.contains("AUTO") || 
                comment.contains("SYSTEM") || 
                comment.contains("BOT") ||
                comment.contains("APPROVED_BY_SYSTEM") ||
                comment.contains("PAUSED_BY_SYSTEM")) {
                return false;
            }
        }
        
        // 只关注配置的品种
        return order.getSymbol().equals(config.getSymbol());
    }
    
    /**
     * 根据订单号查找订单
     */
    private OrderInfo findOrderByTicket(int ticket) {
        OrderInfo order = pendingUserOrders.get(ticket);
        if (order == null) {
            order = activeOrders.get(ticket);
        }
        return order;
    }
    
    /**
     * 获取历史数据（字符串时间框架版本）
     */
    public List<CandleData> getHistoricalData(String symbol, String timeframe, int bars) {
        logger.info("获取历史数据: {} {} {} bars", symbol, timeframe, bars);
        
        // 生成模拟数据
        List<CandleData> data = new ArrayList<>();
        double basePrice = getBasePrice(symbol);
        
        // 根据时间框架确定时间间隔
        int minutesPerBar = getMinutesFromTimeframe(timeframe);
        
        for (int i = bars - 1; i >= 0; i--) {
            double open = basePrice + (Math.random() * 0.002 - 0.001);
            double high = open + Math.random() * 0.001;
            double low = open - Math.random() * 0.001;
            double close = low + Math.random() * (high - low);
            long volume = (long)(1000000 + Math.random() * 500000);
            
            CandleData candle = new CandleData(
                LocalDateTime.now().minusMinutes(i * minutesPerBar),
                open, high, low, close, volume
            );
            candle.setSymbol(symbol);
            data.add(candle);
        }
        
        return data;
    }
    
    /**
     * 从时间框架字符串获取分钟数
     */
    private int getMinutesFromTimeframe(String timeframe) {
        switch (timeframe.toUpperCase()) {
            case "PERIOD_M1": return 1;
            case "PERIOD_M5": return 5;
            case "PERIOD_M15": return 15;
            case "PERIOD_M30": return 30;
            case "PERIOD_H1": return 60;
            case "PERIOD_H4": return 240;
            case "PERIOD_D1": return 1440;
            default: return 1;
        }
    }
    
    /**
     * 获取当前价格
     */
    public double getCurrentPrice(String symbol) {
        return getBasePrice(symbol) + (Math.random() * 0.001 - 0.0005);
    }
    
    /**
     * 获取基础价格
     */
    private double getBasePrice(String symbol) {
        switch (symbol) {
            case "EURUSD": return 1.09500;
            case "GBPUSD": return 1.28000;
            case "USDJPY": return 150.000;
            case "XAUUSD": return 1950.00;
            case "US30": return 35000.00;
            default: return 1.10000;
        }
    }
    
    /**
     * 初始化WebSocket连接
     */
    public boolean initializeWebSocket(Consumer<CandleData> onNewCandle, DecisionEngine decisionEngine) {
        try {
            URI websocketUri = new URI(config.getMt5WebSocketUrl());
            this.websocketClient = new MT5WebSocketClient(websocketUri, config, onNewCandle, decisionEngine);
            
            // 连接WebSocket
            websocketClient.connect();
            
            // 等待连接建立
            for (int i = 0; i < 10; i++) {
                if (websocketClient.isOpen()) {
                    webSocketConnected = true;
                    logger.info("WebSocket连接成功");
                    
                    // 启动心跳检测
                    startHeartbeat();
                    
                    return true;
                }
                Thread.sleep(500);
            }
            
            logger.error("WebSocket连接超时");
            return false;
            
        } catch (Exception e) {
            logger.error("WebSocket初始化失败", e);
            return false;
        }
    }
    
    /**
     * 发送交易指令（兼容版本）
     */
    public CompletableFuture<Boolean> sendOrder(String symbol, String orderType, 
                                              double volume, double price, 
                                              double stopLoss, double takeProfit) {
        return sendOrder(symbol, orderType, volume, price, stopLoss, takeProfit, "AUTO_TRADE");
    }
    
    /**
     * 获取账户余额
     */
    public CompletableFuture<Double> getAccountBalance() {
        CompletableFuture<Double> result = new CompletableFuture<>();
        result.complete(10000.0);
        return result;
    }
    
    /**
     * 启动心跳检测
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (websocketClient != null && websocketClient.isOpen()) {
                    // 发送心跳消息
                    String heartbeat = "{\"type\":\"ping\",\"time\":" + System.currentTimeMillis() + "}";
                    websocketClient.send(heartbeat);
                } else {
                    webSocketConnected = false;
                    logger.warn("WebSocket连接丢失，尝试重新连接...");
                    attemptReconnect();
                }
            } catch (Exception e) {
                logger.error("心跳检测失败", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 尝试重新连接
     */
    private void attemptReconnect() {
        try {
            if (websocketClient != null) {
                websocketClient.reconnect();
                Thread.sleep(2000);
                
                if (websocketClient.isOpen()) {
                    webSocketConnected = true;
                    logger.info("WebSocket重新连接成功");
                }
            }
        } catch (Exception e) {
            logger.error("重新连接失败", e);
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        webSocketConnected = false;
        isMonitoringOrders = false;
        
        if (websocketClient != null) {
            websocketClient.close();
        }
        
        scheduler.shutdown();
        logger.info("MT5连接器已断开");
    }
    
    public boolean isWebSocketConnected() {
        return webSocketConnected;
    }
    
    public TradingConfig getConfig() {
        return config;
    }
    
    /**
     * 生成模拟订单号
     */
    private int generateMockTicket() {
        return 500000 + new Random().nextInt(500000);
    }
}