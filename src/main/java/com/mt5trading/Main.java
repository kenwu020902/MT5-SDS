package com.mt5trading;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.SimpleDecisionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("启动MT5-SDS交易系统...");
        
        try {
            // 加载配置
            TradingConfig config = TradingConfig.load();
            logger.info("配置加载完成: {}", config.getSymbol());
            
            // 创建MT5连接器
            MT5Connector mt5Connector = new MT5Connector(config);
            
            // 创建具体的决策引擎实现
            DecisionEngine decisionEngine = new SimpleDecisionEngine(config, mt5Connector);
            
            // 定义K线数据处理器
            Consumer<CandleData> onNewCandle = candle -> {
                logger.info("接收到新K线: {} - O:{:.5f}, H:{:.5f}, L:{:.5f}, C:{:.5f}, V:{}", 
                    candle.getTime(), 
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    candle.getVolume());
                
                // 传递给决策引擎分析
                decisionEngine.analyzeNewCandle(candle);
            };
            
            // 初始化WebSocket连接
            boolean initialized = mt5Connector.initializeWebSocket(onNewCandle, decisionEngine);
            if (!initialized) {
                logger.error("WebSocket初始化失败");
                return;
            }
            
            // 等待连接建立
            int attempts = 0;
            while (!mt5Connector.isWebSocketConnected() && attempts < 10) {
                Thread.sleep(1000);
                attempts++;
                logger.info("等待WebSocket连接... ({}/10)", attempts);
            }
            
            if (mt5Connector.isWebSocketConnected()) {
                logger.info("✅ MT5连接成功！系统准备就绪");
                
                // 获取历史数据用于初始化
                var historicalData = mt5Connector.getHistoricalData(
                    config.getSymbol(), 
                    config.getTimeframe(), 
                    config.getDataHistoryBars());
                logger.info("获取到 {} 根历史K线数据", historicalData.size());
                
                // 处理历史数据
                for (CandleData candle : historicalData) {
                    decisionEngine.analyzeNewCandle(candle);
                }
                
                // 主循环保持程序运行
                while (true) {
                    // 定期检查连接状态
                    if (!mt5Connector.isWebSocketConnected()) {
                        logger.warn("WebSocket连接断开，尝试重连...");
                        mt5Connector.initializeWebSocket(onNewCandle, decisionEngine);
                    }
                    
                    // 获取当前价格（测试）
                    if (config.isTestMode()) {
                        double currentPrice = mt5Connector.getCurrentPrice(config.getSymbol());
                        logger.debug("当前价格: {:.5f}", currentPrice);
                    }
                    
                    Thread.sleep(config.getPollingInterval());
                }
            } else {
                logger.error("MT5连接超时");
            }
            
        } catch (Exception e) {
            logger.error("系统启动失败", e);
            e.printStackTrace();
        }
    }
}