package com.mt5trading;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.MT5DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    private static MT5Connector connector;
    private static MT5DataFetcher dataFetcher;
    private static DecisionEngine decisionEngine;
    private static MT5BridgeServer bridgeServer;
    private static ScheduledExecutorService scheduler;
    private static boolean systemRunning = false;
    
    public static void main(String[] args) {
        logger.info("=== MT5-SDS 系统启动 ===");
        logger.info("版本: 1.0.0");
        logger.info("启动时间: {}", LocalDateTime.now());
        
        try {
            // 1. 加载配置
            TradingConfig config = loadConfiguration();
            
            // 2. 启动WebSocket桥接服务器
            startWebSocketServer();
            
            // 3. 初始化系统组件
            initializeComponents(config);
            
            // 4. 等待MT5连接
            waitForMT5Connection();
            
            // 5. 启动数据获取和处理
            startDataProcessing(config);
            
            // 6. 启动系统监控
            startSystemMonitor();
            
            // 7. 注册关闭钩子
            registerShutdownHook();
            
            // 8. 主循环
            runMainLoop();
            
        } catch (Exception e) {
            logger.error("系统启动失败", e);
            shutdownSystem();
        }
    }
    
    private static TradingConfig loadConfiguration() throws IOException {
        logger.info("加载配置文件...");
        TradingConfig config = TradingConfig.load();
        
        logger.info("配置信息:");
        logger.info("  - MT5账户: {}", config.getMt5Login());
        logger.info("  - 服务器: {}", config.getMt5Server());
        logger.info("  - 交易品种: {}", config.getSymbol());
        logger.info("  - 时间框架: {}", config.getTimeframe());
        logger.info("  - WebSocket URL: {}", config.getMt5WebSocketUrl());
        logger.info("  - 风险比例: {}%", config.getRiskPercentage());
        logger.info("  - 测试模式: {}", config.isTestMode());
        
        return config;
    }
    
    private static void startWebSocketServer() {
        logger.info("启动WebSocket桥接服务器...");
        
        try {
            bridgeServer = new MT5BridgeServer(8080);
            bridgeServer.start();
            
            logger.info("WebSocket服务器已启动在端口 8080");
            logger.info("等待MT5 EA连接...");
            
        } catch (Exception e) {
            logger.error("WebSocket服务器启动失败", e);
            throw new RuntimeException("无法启动WebSocket服务器", e);
        }
    }
    
    private static void initializeComponents(TradingConfig config) {
        logger.info("初始化系统组件...");
        
        // 创建连接器
        connector = new MT5Connector(config);
        logger.info("  ✓ MT5连接器已创建");
        
        // 创建数据获取器
        dataFetcher = new MT5DataFetcher(config, connector);
        logger.info("  ✓ 数据获取器已创建");
        
        // 创建决策引擎 - 使用正确的构造函数
        decisionEngine = new SimpleDecisionEngine(config, connector);
        logger.info("  ✓ 决策引擎已创建");
        
        // 创建调度器
        scheduler = Executors.newScheduledThreadPool(3);
        logger.info("  ✓ 任务调度器已创建");
    }
    
    private static void waitForMT5Connection() {
        logger.info("等待MT5 EA连接...");
        
        int maxWaitTime = 120; // 最大等待120秒
        int waitTime = 0;
        
        while (!bridgeServer.hasMT5Connection() && waitTime < maxWaitTime) {
            try {
                TimeUnit.SECONDS.sleep(1);
                waitTime++;
                
                if (waitTime % 10 == 0) {
                    logger.info("已等待 {} 秒，请确保MT5 EA已加载并运行", waitTime);
                    logger.info("MT5 EA参数检查:");
                    logger.info("  - 端口: 8080");
                    logger.info("  - 服务器地址: 127.0.0.1");
                    logger.info("  - TestMode: true");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (bridgeServer.hasMT5Connection()) {
            logger.info("✓ MT5 EA连接成功");
        } else {
            logger.error("✗ MT5 EA连接超时");
            throw new RuntimeException("MT5连接超时");
        }
    }
    
    private static void startDataProcessing(TradingConfig config) {
        logger.info("启动数据处理...");
        
        // 1. 获取历史数据
        scheduler.schedule(() -> {
            logger.info("获取历史数据...");
            List<CandleData> historicalData = dataFetcher.fetchHistoricalData();
            
            if (historicalData != null && !historicalData.isEmpty()) {
                logger.info("成功获取历史数据: {} 根K线", historicalData.size());
                
                // 分析历史数据
                for (CandleData candle : historicalData) {
                    decisionEngine.analyzeNewCandle(candle);
                }
            } else {
                logger.warn("历史数据获取失败，使用模拟数据");
            }
        }, 2, TimeUnit.SECONDS);
        
        // 2. 初始化WebSocket连接
        scheduler.schedule(() -> {
            logger.info("初始化WebSocket连接...");
            
            boolean connected = connector.initializeWebSocket(
                candle -> {
                    // 实时K线回调
                    logger.debug("[实时数据] 新K线: {}", candle.getTime());
                    
                    // 更新数据获取器
                    dataFetcher.updateLastCandle(candle);
                    
                    // 传递给决策引擎
                    decisionEngine.analyzeNewCandle(candle);
                },
                decisionEngine
            );
            
            if (connected) {
                systemRunning = true;
                logger.info("✓ 系统启动完成，开始实时交易监控");
            } else {
                logger.error("✗ WebSocket连接失败");
            }
        }, 3, TimeUnit.SECONDS);
    }
    
    private static void startSystemMonitor() {
        logger.info("启动系统监控...");
        
        // 监控系统状态（每30秒）
        scheduler.scheduleAtFixedRate(() -> {
            if (systemRunning) {
                logger.info("[系统监控] 状态检查:");
                logger.info("  - MT5连接: {}", bridgeServer.hasMT5Connection() ? "正常" : "断开");
                logger.info("  - WebSocket连接: {}", connector.isWebSocketConnected() ? "正常" : "断开");
                logger.info("  - 最后K线时间: {}", 
                    dataFetcher.getLastCandle() != null ? 
                    dataFetcher.getLastCandle().getTime() : "无数据");
                
                // 获取当前价格
                double currentPrice = dataFetcher.fetchCurrentPrice();
                logger.info("  - 当前价格: {:.5f}", currentPrice);
                
                // 获取账户余额
                connector.getAccountBalance().thenAccept(balance -> {
                    logger.info("  - 账户余额: ${:.2f}", balance);
                });
            }
        }, 60, 30, TimeUnit.SECONDS);
        
        // 健康检查（每10秒）
        scheduler.scheduleAtFixedRate(() -> {
            if (systemRunning) {
                if (!bridgeServer.hasMT5Connection()) {
                    logger.warn("[健康检查] MT5连接丢失");
                }
                if (!connector.isWebSocketConnected()) {
                    logger.warn("[健康检查] WebSocket连接丢失");
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("收到关闭信号，正在停止系统...");
            shutdownSystem();
        }));
    }
    
    private static void runMainLoop() {
        logger.info("系统进入主运行循环...");
        logger.info("按 Ctrl+C 停止系统");
        
        // 简单的控制台交互
        if (System.console() != null) {
            new Thread(() -> {
                try {
                    while (systemRunning) {
                        System.out.print("\n命令 (help/status/exit): ");
                        String command = System.console().readLine();
                        
                        if (command == null) continue;
                        
                        switch (command.trim().toLowerCase()) {
                            case "help":
                                showHelp();
                                break;
                            case "status":
                                showStatus();
                                break;
                            case "exit":
                                logger.info("收到退出命令");
                                shutdownSystem();
                                System.exit(0);
                                break;
                            case "test":
                                runTestTrade();
                                break;
                            case "price":
                                showCurrentPrice();
                                break;
                            default:
                                System.out.println("未知命令，输入 'help' 查看可用命令");
                        }
                    }
                } catch (Exception e) {
                    logger.error("控制台交互错误", e);
                }
            }).start();
        }
        
        // 主线程保持运行
        try {
            while (systemRunning) {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("主循环被中断");
        }
    }
    
    private static void showHelp() {
        System.out.println("\n=== 可用命令 ===");
        System.out.println("help     - 显示此帮助信息");
        System.out.println("status   - 显示系统状态");
        System.out.println("price    - 显示当前价格");
        System.out.println("test     - 执行测试交易");
        System.out.println("exit     - 停止并退出系统");
        System.out.println("================\n");
    }
    
    private static void showStatus() {
        System.out.println("\n=== 系统状态 ===");
        System.out.println("运行状态: " + (systemRunning ? "运行中" : "已停止"));
        System.out.println("MT5连接: " + (bridgeServer.hasMT5Connection() ? "已连接" : "断开"));
        System.out.println("WebSocket: " + (connector.isWebSocketConnected() ? "已连接" : "断开"));
        
        CandleData lastCandle = dataFetcher.getLastCandle();
        if (lastCandle != null) {
            System.out.println("最后K线: " + lastCandle.getTime());
            System.out.println("收盘价: " + lastCandle.getClose());
        } else {
            System.out.println("最后K线: 无数据");
        }
        System.out.println("================\n");
    }
    
    private static void showCurrentPrice() {
        double price = dataFetcher.fetchCurrentPrice();
        System.out.println("\n当前价格: " + price);
    }
    
    private static void runTestTrade() {
        if (!systemRunning) {
            System.out.println("系统未运行");
            return;
        }
        
        System.out.println("\n执行测试交易...");
        
        TradingConfig config = connector.getConfig();
        connector.sendOrder(
            config.getSymbol(),
            "BUY",
            0.1,
            0,
            0,
            0
        ).thenAccept(success -> {
            if (success) {
                System.out.println("测试交易指令发送成功");
            } else {
                System.out.println("测试交易指令发送失败");
            }
        });
    }
    
    private static void shutdownSystem() {
        logger.info("正在停止系统...");
        systemRunning = false;
        
        // 停止调度器
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("任务调度器已停止");
        }
        
        // 断开连接
        if (connector != null) {
            connector.disconnect();
            logger.info("MT5连接器已断开");
        }
        
        // 停止WebSocket服务器
        if (bridgeServer != null) {
            try {
                bridgeServer.stop();
                logger.info("WebSocket服务器已停止");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("停止WebSocket服务器时被中断");
            }
        }
        
        logger.info("系统停止完成");
        logger.info("=== MT5-SDS 系统停止 ===");
    }
}

// 简单的决策引擎实现
class SimpleDecisionEngine extends com.mt5trading.services.DecisionEngine {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleDecisionEngine.class);
    
    public SimpleDecisionEngine(com.mt5trading.config.TradingConfig config, 
                               com.mt5trading.mt5.connector.MT5Connector connector) {
        super(config, connector);
    }
    
    @Override
    public void analyzeNewCandle(com.mt5trading.models.CandleData candle) {
        logger.info("[决策引擎] 分析新K线: {}", candle.getTime());
        logger.info("[决策引擎] 收盘价: {}", candle.getClose());
        
        // 简单的交易逻辑示例
        double currentPrice = candle.getClose();
        
        if (currentPrice > 1.10500) {
            logger.info("[决策引擎] 检测到买入信号");
            executeTrade(config.getSymbol(), "BUY", 0.1);
        } else if (currentPrice < 1.09500) {
            logger.info("[决策引擎] 检测到卖出信号");
            executeTrade(config.getSymbol(), "SELL", 0.1);
        }
    }
    
    @Override
    public void executeTrade(String symbol, String action, double volume) {
        logger.info("[决策引擎] 执行交易: {} {} @ {}", action, symbol, volume);
        
        // 发送交易指令
        connector.sendOrder(symbol, action, volume, 0, 0, 0)
            .thenAccept(success -> {
                if (success) {
                    logger.info("[决策引擎] 交易指令发送成功");
                } else {
                    logger.error("[决策引擎] 交易指令发送失败");
                }
            });
    }
}

// 桥接服务器类
class MT5BridgeServer extends org.java_websocket.server.WebSocketServer {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MT5BridgeServer.class);
    
    private final java.util.Map<String, org.java_websocket.WebSocket> mt5Clients = new java.util.HashMap<>();
    private final java.util.Map<String, org.java_websocket.WebSocket> javaClients = new java.util.HashMap<>();
    
    public MT5BridgeServer(int port) {
        super(new java.net.InetSocketAddress(port));
    }
    
    @Override
    public void onOpen(org.java_websocket.WebSocket conn, org.java_websocket.handshake.ClientHandshake handshake) {
        String clientId = conn.getRemoteSocketAddress().toString();
        String userAgent = handshake.getFieldValue("User-Agent");
        
        if (userAgent != null && userAgent.contains("MT5")) {
            mt5Clients.put(clientId, conn);
            logger.info("MT5客户端连接: {}", clientId);
        } else {
            javaClients.put(clientId, conn);
            logger.info("Java客户端连接: {}", clientId);
        }
    }
    
    @Override
    public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
        String clientId = conn.getRemoteSocketAddress().toString();
        
        if (mt5Clients.containsKey(clientId)) {
            mt5Clients.remove(clientId);
            logger.info("MT5客户端断开: {}", clientId);
        } else if (javaClients.containsKey(clientId)) {
            javaClients.remove(clientId);
            logger.info("Java客户端断开: {}", clientId);
        }
    }
    
    @Override
    public void onMessage(org.java_websocket.WebSocket conn, String message) {
        String clientId = conn.getRemoteSocketAddress().toString();
        
        // 转发消息
        if (mt5Clients.containsKey(clientId)) {
            // 来自MT5的消息，转发给所有Java客户端
            forwardToJavaClients(message);
            logger.debug("MT5 -> Java: {}", message.length() > 100 ? message.substring(0, 100) + "..." : message);
        } else if (javaClients.containsKey(clientId)) {
            // 来自Java的消息，转发给所有MT5客户端
            forwardToMT5Clients(message);
            logger.debug("Java -> MT5: {}", message.length() > 100 ? message.substring(0, 100) + "..." : message);
        }
    }
    
    @Override
    public void onError(org.java_websocket.WebSocket conn, Exception ex) {
        logger.error("WebSocket错误", ex);
    }
    
    @Override
    public void onStart() {
        logger.info("WebSocket服务器启动成功，端口: {}", getPort());
    }
    
    private void forwardToJavaClients(String message) {
        for (org.java_websocket.WebSocket client : javaClients.values()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }
    
    private void forwardToMT5Clients(String message) {
        for (org.java_websocket.WebSocket client : mt5Clients.values()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }
    
    public boolean hasMT5Connection() {
        return !mt5Clients.isEmpty();
    }
    
    public boolean hasJavaConnection() {
        return !javaClients.isEmpty();
    }
}