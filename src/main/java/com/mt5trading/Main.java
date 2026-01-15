package com.mt5trading;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.SimpleDecisionEngine;
import com.mt5trading.services.UserOrderDecisionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("启动MT5交易系统...");
        
        try {
            // 加载配置
            TradingConfig config = TradingConfig.load();
            
            // 显示配置摘要
            config.printConfigSummary();
            
            // 验证配置
            if (!config.validateConfig()) {
                logger.error("配置验证失败，请检查配置文件");
                return;
            }
            
            // 创建MT5连接器
            MT5Connector mt5Connector = new MT5Connector(config);
            
            // 创建决策引擎
            DecisionEngine decisionEngine = createDecisionEngine(config, mt5Connector);
            
            // 定义K线数据处理器
            Consumer<CandleData> onNewCandle = candle -> {
                logger.info("新K线: {} - 收盘价: {:.5f}", 
                    candle.getTime(), candle.getClose());
                decisionEngine.analyzeNewCandle(candle);
            };
            
            // 初始化WebSocket连接
            if (!mt5Connector.initializeWebSocket(onNewCandle, decisionEngine)) {
                logger.error("WebSocket初始化失败");
                return;
            }
            
            // 等待连接
            int attempts = 0;
            while (!mt5Connector.isWebSocketConnected() && attempts < 10) {
                Thread.sleep(1000);
                attempts++;
                logger.info("等待WebSocket连接... ({}/10)", attempts);
            }
            
            if (!mt5Connector.isWebSocketConnected()) {
                logger.error("连接超时");
                return;
            }
            
            logger.info("✅ 连接成功，系统运行中...");
            
            // 如果是用户订单检测模式，启动订单监控
            if (decisionEngine instanceof UserOrderDecisionEngine) {
                logger.info("启动用户订单监控...");
                ((UserOrderDecisionEngine) decisionEngine).displayStatus();
            }
            
            // 简单的主循环
            runMainLoop(mt5Connector, config, decisionEngine);
            
        } catch (Exception e) {
            logger.error("系统启动失败", e);
            e.printStackTrace();
        }
        
        logger.info("系统停止");
    }
    
    /**
     * 创建决策引擎
     */
    private static DecisionEngine createDecisionEngine(TradingConfig config, MT5Connector connector) {
        // 根据配置选择决策引擎
        if (config.isEnableOrderMonitoring()) {
            logger.info("使用用户订单检测引擎");
            return new UserOrderDecisionEngine(config, connector);
        } else {
            logger.info("使用简单决策引擎");
            return new SimpleDecisionEngine(config, connector);
        }
    }
    
    /**
     * 获取历史数据
     */
    private static List<CandleData> getHistoricalData(MT5Connector connector, TradingConfig config) {
        try {
            // 尝试获取历史数据
            return connector.getHistoricalData(
                config.getSymbol(), 
                config.getTimeframe(), 
                config.getDataHistoryBars()
            );
        } catch (Exception e) {
            logger.warn("获取历史数据失败: {}", e.getMessage());
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 主循环
     */
    private static void runMainLoop(MT5Connector mt5Connector, TradingConfig config, DecisionEngine decisionEngine) {
        try {
            while (true) {
                // 保持连接
                if (!mt5Connector.isWebSocketConnected()) {
                    logger.warn("连接断开");
                    break;
                }
                
                // 每分钟显示一次状态
                if (System.currentTimeMillis() % 60000 < config.getPollingInterval()) {
                    displaySystemStatus(mt5Connector, config, decisionEngine);
                }
                
                Thread.sleep(config.getPollingInterval());
            }
        } catch (InterruptedException e) {
            logger.info("系统被中断");
            Thread.currentThread().interrupt();
        } finally {
            // 优雅关闭
            shutdown(mt5Connector, decisionEngine);
        }
    }
    
    /**
     * 显示系统状态
     */
    private static void displaySystemStatus(MT5Connector mt5Connector, TradingConfig config, DecisionEngine decisionEngine) {
        try {
            // 获取当前价格
            double currentPrice = mt5Connector.getCurrentPrice(config.getSymbol());
            logger.info("系统状态 - 品种: {} 当前价: {:.5f} 模式: {}", 
                config.getSymbol(), currentPrice, 
                config.isTestMode() ? "测试" : "实盘");
            
            // 如果是用户订单决策引擎，显示额外信息
            if (decisionEngine instanceof UserOrderDecisionEngine) {
                ((UserOrderDecisionEngine) decisionEngine).displayStatus();
            }
            
        } catch (Exception e) {
            logger.debug("获取系统状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * 优雅关闭
     */
    private static void shutdown(MT5Connector mt5Connector, DecisionEngine decisionEngine) {
        logger.info("正在关闭系统...");
        
        try {
            // 关闭决策引擎
            try {
                decisionEngine.getClass().getMethod("shutdown").invoke(decisionEngine);
            } catch (Exception e) {
                // 如果决策引擎没有shutdown方法，忽略
            }
            
            // 关闭连接器
            mt5Connector.close();
            
            logger.info("系统已关闭");
        } catch (Exception e) {
            logger.error("关闭系统时出错", e);
        }
    }
    
    // 添加关闭钩子
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("收到关闭信号...");
        }));
    }
}