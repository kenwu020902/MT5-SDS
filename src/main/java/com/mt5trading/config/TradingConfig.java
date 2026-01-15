package com.mt5trading.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TradingConfig {
    private final Properties properties;
    
    private TradingConfig(Properties properties) {
        this.properties = properties;
    }
    
    public static TradingConfig load() throws IOException {
        Properties props = new Properties();
        try (InputStream input = TradingConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("无法找到 application.properties 文件");
            }
            props.load(input);
        }
        return new TradingConfig(props);
    }
    
    // WebSocket 配置
    public String getMt5WebSocketUrl() {
        return properties.getProperty("mt5.websocket.url", "ws://localhost:8080");
    }
    
    public int getWebSocketReconnectInterval() {
        return Integer.parseInt(properties.getProperty("mt5.websocket.reconnect.interval", "5000"));
    }
    
    public int getWebSocketHeartbeatInterval() {
        return Integer.parseInt(properties.getProperty("mt5.websocket.heartbeat.interval", "30000"));
    }
    
    // MT5 连接配置
    public String getMt5ApiUrl() {
        return properties.getProperty("mt5.api.url", "http://localhost:8080/api");
    }
    
    public String getMt5Login() {
        return properties.getProperty("mt5.login", "1234567");
    }
    
    public String getMt5Password() {
        return properties.getProperty("mt5.password", "");
    }
    
    public String getMt5Server() {
        return properties.getProperty("mt5.server", "Demo");
    }
    
    public int getMagicNumber() {
        return Integer.parseInt(properties.getProperty("mt5.magic.number", "123456"));
    }
    
    // 交易品种配置
    public String getSymbol() {
        return properties.getProperty("mt5.symbol", "EURUSD");
    }
    
    public String getTimeframe() {
        return properties.getProperty("mt5.timeframe", "PERIOD_H1");
    }
    
    // 交易参数 - 需要添加的缺失方法
    public double getTradeVolume() {
        return Double.parseDouble(properties.getProperty("trading.volume", "0.01"));
    }
    
    public double getRiskPercentage() {
        return Double.parseDouble(properties.getProperty("trading.risk.percentage", "1.0"));
    }
    
    public boolean isUseStrictConfirmation() {
        return Boolean.parseBoolean(properties.getProperty("trading.useStrictConfirmation", "true"));
    }
    
    public boolean isUseMACDConfirmation() {
        return Boolean.parseBoolean(properties.getProperty("trading.useMACDConfirmation", "true"));
    }
    
    public double getSlippage() {
        return Double.parseDouble(properties.getProperty("trading.slippage", "2.0"));
    }
    
    public double getMaxPositionSize() {
        return Double.parseDouble(properties.getProperty("trading.max.position.size", "10.0"));
    }
    
    public int getStopLossPips() {
        return Integer.parseInt(properties.getProperty("trading.stop.loss.pips", "20"));
    }
    
    public int getTakeProfitPips() {
        return Integer.parseInt(properties.getProperty("trading.take.profit.pips", "40"));
    }
    
    // MACD 参数
    public int getMacdFast() {
        return Integer.parseInt(properties.getProperty("trading.macd.fast", "12"));
    }
    
    public int getMacdSlow() {
        return Integer.parseInt(properties.getProperty("trading.macd.slow", "26"));
    }
    
    public int getMacdSignal() {
        return Integer.parseInt(properties.getProperty("trading.macd.signal", "9"));
    }
    
    // 应用设置
    public int getPollingInterval() {
        return Integer.parseInt(properties.getProperty("app.polling.interval", "5000"));
    }
    
    public boolean isEnableConsoleLogging() {
        return Boolean.parseBoolean(properties.getProperty("app.enable.console.logging", "true"));
    }
    
    public int getDataHistoryBars() {
        return Integer.parseInt(properties.getProperty("app.data.history.bars", "100"));
    }
    
    public boolean isTestMode() {
        return Boolean.parseBoolean(properties.getProperty("app.test.mode", "true"));
    }
}