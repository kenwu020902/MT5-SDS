package com.mt5trading.mt5.connector;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MT5Connector {
    private static final Logger logger = LoggerFactory.getLogger(MT5Connector.class);
    
    private final TradingConfig config;
    private final HttpClient httpClient;
    private MT5WebSocketClient websocketClient;
    private final ScheduledExecutorService scheduler;
    private boolean webSocketConnected;
    
    public MT5Connector(TradingConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.webSocketConnected = false;
    }
    
    // 初始化WebSocket连接
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
    
    // 发送交易指令（通过WebSocket）
    public CompletableFuture<Boolean> sendOrder(String symbol, String orderType, 
                                              double volume, double price, 
                                              double stopLoss, double takeProfit) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        if (!webSocketConnected || websocketClient == null) {
            logger.error("WebSocket未连接，无法发送订单");
            result.complete(false);
            return result;
        }
        
        try {
            // 通过WebSocket发送订单
            websocketClient.sendTradeOrder(orderType, symbol, volume, price, stopLoss, takeProfit);
            
            // 注意：这里需要等待交易响应
            // 实际实现中应该使用回调或响应队列
            result.complete(true);
            
        } catch (Exception e) {
            logger.error("订单发送失败", e);
            result.complete(false);
        }
        
        return result;
    }
    
    // 获取账户余额（通过WebSocket）
    public CompletableFuture<Double> getAccountBalance() {
        CompletableFuture<Double> result = new CompletableFuture<>();
        
        if (!webSocketConnected || websocketClient == null) {
            logger.warn("WebSocket未连接，使用默认余额");
            result.complete(10000.0);  // 默认值
            return result;
        }
        
        // 请求账户信息
        websocketClient.requestAccountInfo();
        
        // 注意：这里需要等待响应
        // 实际实现中应该使用回调或响应队列
        result.complete(10000.0);  // 临时返回值
        
        return result;
    }
    
    // 启动心跳检测
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
    
    // 尝试重新连接
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
    
    // 断开连接
    public void disconnect() {
        webSocketConnected = false;
        
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
}