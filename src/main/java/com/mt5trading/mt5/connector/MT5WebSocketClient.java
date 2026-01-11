package com.mt5trading.mt5.connector;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MT5WebSocketClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(MT5WebSocketClient.class);
    
    private final TradingConfig config;
    private final Consumer<CandleData> onNewCandle;
    private final DecisionEngine decisionEngine;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<String> messageQueue;
    private boolean authenticated;
    private String sessionId;
    
    public MT5WebSocketClient(URI serverUri, TradingConfig config, 
                             Consumer<CandleData> onNewCandle, DecisionEngine decisionEngine) {
        super(serverUri);
        this.config = config;
        this.onNewCandle = onNewCandle;
        this.decisionEngine = decisionEngine;
        this.objectMapper = new ObjectMapper();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.authenticated = false;
        this.sessionId = null;
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("WebSocket连接已建立，状态码: {}", handshake.getHttpStatus());
        
        // 发送认证消息
        authenticate();
        
        // 订阅市场数据
        subscribeToMarketData();
        
        // 启动消息处理线程
        startMessageProcessor();
    }
    
    @Override
    public void onMessage(String message) {
        try {
            logger.debug("收到消息: {}", message);
            messageQueue.put(message);
        } catch (InterruptedException e) {
            logger.error("消息队列插入中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("WebSocket连接关闭. 代码: {}, 原因: {}, 远程关闭: {}", 
                   code, reason, remote);
        
        authenticated = false;
        sessionId = null;
        
        // 尝试重新连接
        if (remote) {
            reconnectWithDelay(5000);
        }
    }
    
    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket错误", ex);
    }
    
    private void authenticate() {
        try {
            ObjectNode authMsg = objectMapper.createObjectNode();
            authMsg.put("type", "auth");
            authMsg.put("login", config.getMt5Login());
            authMsg.put("password", config.getMt5Password());
            authMsg.put("server", config.getMt5Server());
            
            send(authMsg.toString());
            logger.info("已发送认证请求");
        } catch (Exception e) {
            logger.error("认证请求发送失败", e);
        }
    }
    
    private void subscribeToMarketData() {
        try {
            ObjectNode subscribeMsg = objectMapper.createObjectNode();
            subscribeMsg.put("type", "subscribe");
            subscribeMsg.put("symbol", config.getSymbol());
            subscribeMsg.put("timeframe", config.getTimeframe());
            
            send(subscribeMsg.toString());
            logger.info("已订阅 {} 的 {} 周期数据", config.getSymbol(), config.getTimeframe());
        } catch (Exception e) {
            logger.error("订阅请求发送失败", e);
        }
    }
    
    private void startMessageProcessor() {
        Thread processorThread = new Thread(() -> {
            while (!isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        processMessage(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("消息处理错误", e);
                }
            }
        });
        
        processorThread.setName("WebSocket-Message-Processor");
        processorThread.setDaemon(true);
        processorThread.start();
    }
    
    private void processMessage(String message) {
        try {
            ObjectNode json = (ObjectNode) objectMapper.readTree(message);
            String type = json.has("type") ? json.get("type").asText() : "";
            
            switch (type) {
                case "auth_response":
                    handleAuthResponse(json);
                    break;
                case "market":
                    handleMarketData(json);
                    break;
                case "trade":
                    handleTradeResponse(json);
                    break;
                case "account":
                    handleAccountInfo(json);
                    break;
                case "error":
                    handleError(json);
                    break;
                default:
                    logger.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("消息解析失败", e);
        }
    }
    
    private void handleAuthResponse(ObjectNode json) {
        boolean success = json.has("status") && "success".equals(json.get("status").asText());
        
        if (success) {
            authenticated = true;
            sessionId = json.has("session_id") ? json.get("session_id").asText() : null;
            logger.info("认证成功，会话ID: {}", sessionId);
        } else {
            authenticated = false;
            String error = json.has("error") ? json.get("error").asText() : "未知错误";
            logger.error("认证失败: {}", error);
        }
    }
    
    private void handleMarketData(ObjectNode json) {
        if (!json.has("data")) return;
        
        try {
            // 解析市场数据
            if (json.get("data").isArray()) {
                for (var item : json.get("data")) {
                    String symbol = item.get("symbol").asText();
                    double bid = item.get("bid").asDouble();
                    double ask = item.get("ask").asDouble();
                    long timestamp = item.get("time").asLong();
                    
                    // 创建蜡烛数据（简化）
                    LocalDateTime time = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                    
                    CandleData candle = new CandleData(
                        time, bid, bid + 0.0002, bid - 0.0001, ask, 1000
                    );
                    
                    // 通知决策引擎
                    if (onNewCandle != null) {
                        onNewCandle.accept(candle);
                    }
                    
                    // 如果是订阅的品种，触发分析
                    if (symbol.equals(config.getSymbol())) {
                        decisionEngine.analyzeNewCandle(candle);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("市场数据处理失败", e);
        }
    }
    
    private void handleTradeResponse(ObjectNode json) {
        String status = json.has("status") ? json.get("status").asText() : "unknown";
        int ticket = json.has("ticket") ? json.get("ticket").asInt() : 0;
        
        if ("success".equals(status)) {
            logger.info("订单执行成功，单号: {}", ticket);
        } else {
            String error = json.has("error") ? json.get("error").asText() : "未知错误";
            logger.error("订单执行失败: {}，单号: {}", error, ticket);
        }
    }
    
    private void handleAccountInfo(ObjectNode json) {
        // 处理账户信息更新
        double balance = json.has("balance") ? json.get("balance").asDouble() : 0;
        double equity = json.has("equity") ? json.get("equity").asDouble() : 0;
        
        logger.debug("账户更新 - 余额: ${}, 净值: ${}", balance, equity);
    }
    
    private void handleError(ObjectNode json) {
        String error = json.has("message") ? json.get("message").asText() : "未知错误";
        logger.error("服务器错误: {}", error);
    }
    
    // 发送交易指令到MT5
    public void sendTradeOrder(String action, String symbol, double volume, 
                              double price, double stopLoss, double takeProfit) {
        if (!authenticated) {
            logger.error("未认证，无法发送交易指令");
            return;
        }
        
        try {
            ObjectNode orderMsg = objectMapper.createObjectNode();
            orderMsg.put("type", "trade");
            orderMsg.put("action", action);
            orderMsg.put("symbol", symbol);
            orderMsg.put("volume", volume);
            orderMsg.put("price", price);
            orderMsg.put("sl", stopLoss);
            orderMsg.put("tp", takeProfit);
            orderMsg.put("magic", config.getMagicNumber());
            orderMsg.put("comment", "MT5-SDS Auto Trade");
            
            send(orderMsg.toString());
            logger.info("已发送交易指令: {} {} @ {}", action, symbol, price);
        } catch (Exception e) {
            logger.error("交易指令发送失败", e);
        }
    }
    
    // 请求账户信息
    public void requestAccountInfo() {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("type", "request");
            request.put("request", "account");
            
            send(request.toString());
        } catch (Exception e) {
            logger.error("账户信息请求发送失败", e);
        }
    }
    
    private void reconnectWithDelay(int delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                logger.info("尝试重新连接...");
                reconnect();
            } catch (Exception e) {
                logger.error("重新连接失败", e);
            }
        }).start();
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getSessionId() {
        return sessionId;
    }
}