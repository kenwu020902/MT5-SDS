package com.mt5trading.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TradingConfig {
    private String mt5WebSocketUrl;
    private String symbol;
    private String timeframe;
    private boolean useStrictConfirmation;
    private boolean useMACDConfirmation;
    private int macdFastPeriod;
    private int macdSlowPeriod;
    private int macdSignalPeriod;
    private double riskPercentage;
    private double maxPositionSize;
    private int stopLossPips;
    private int takeProfitPips;
    private int pollingInterval;
    private boolean enableConsoleLogging;
    private int dataHistoryBars;
    
    // 新增字段
    private String mt5ApiUrl;
    private String mt5Login;
    private String mt5Password;
    private String mt5Server;
    private int magicNumber;
    private double slippage;
    
    private static TradingConfig instance;
    
    private TradingConfig() {
        // Private constructor for singleton
    }
    
    public static TradingConfig load() throws IOException {
        if (instance == null) {
            instance = new TradingConfig();
            instance.loadProperties();
        }
        return instance;
    }
    
    private void loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            props.load(input);
            
            // MT5连接配置
            mt5ApiUrl = props.getProperty("mt5.api.url", "http://localhost:8080/api");
            mt5WebSocketUrl = props.getProperty("mt5.websocket.url", "ws://localhost:8080");
            mt5Login = props.getProperty("mt5.login", "");
            mt5Password = props.getProperty("mt5.password", "");
            mt5Server = props.getProperty("mt5.server", "");
            magicNumber = Integer.parseInt(props.getProperty("mt5.magic.number", "123456"));
            slippage = Double.parseDouble(props.getProperty("trading.slippage", "2.0"));
            
            // Load configuration values
            symbol = props.getProperty("mt5.symbol", "EURUSD");
            timeframe = props.getProperty("mt5.timeframe", "PERIOD_H1");
            useStrictConfirmation = Boolean.parseBoolean(props.getProperty("trading.useStrictConfirmation", "true"));
            useMACDConfirmation = Boolean.parseBoolean(props.getProperty("trading.useMACDConfirmation", "true"));
            macdFastPeriod = Integer.parseInt(props.getProperty("trading.macd.fast", "12"));
            macdSlowPeriod = Integer.parseInt(props.getProperty("trading.macd.slow", "26"));
            macdSignalPeriod = Integer.parseInt(props.getProperty("trading.macd.signal", "9"));
            riskPercentage = Double.parseDouble(props.getProperty("trading.risk.percentage", "2.0"));
            maxPositionSize = Double.parseDouble(props.getProperty("trading.max.position.size", "10.0"));
            stopLossPips = Integer.parseInt(props.getProperty("trading.stop.loss.pips", "20"));
            takeProfitPips = Integer.parseInt(props.getProperty("trading.take.profit.pips", "40"));
            pollingInterval = Integer.parseInt(props.getProperty("app.polling.interval", "5000"));
            enableConsoleLogging = Boolean.parseBoolean(props.getProperty("app.enable.console.logging", "true"));
            dataHistoryBars = Integer.parseInt(props.getProperty("app.data.history.bars", "100"));
        }
    }
    
    // Getters
    public String getMt5WebSocketUrl() { return mt5WebSocketUrl; }
    public String getMt5ApiUrl() { return mt5ApiUrl; }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public boolean isUseStrictConfirmation() { return useStrictConfirmation; }
    public boolean isUseMACDConfirmation() { return useMACDConfirmation; }
    public int getMacdFastPeriod() { return macdFastPeriod; }
    public int getMacdSlowPeriod() { return macdSlowPeriod; }
    public int getMacdSignalPeriod() { return macdSignalPeriod; }
    public double getRiskPercentage() { return riskPercentage; }
    public double getMaxPositionSize() { return maxPositionSize; }
    public int getStopLossPips() { return stopLossPips; }
    public int getTakeProfitPips() { return takeProfitPips; }
    public int getPollingInterval() { return pollingInterval; }
    public boolean isEnableConsoleLogging() { return enableConsoleLogging; }
    public int getDataHistoryBars() { return dataHistoryBars; }
    public String getMt5Login() { return mt5Login; }
    public String getMt5Password() { return mt5Password; }
    public String getMt5Server() { return mt5Server; }
    public int getMagicNumber() { return magicNumber; }
    public double getSlippage() { return slippage; }
    
    @Override
    public String toString() {
        return "TradingConfig{" +
                "symbol='" + symbol + '\'' +
                ", timeframe='" + timeframe + '\'' +
                ", apiUrl='" + mt5ApiUrl + '\'' +
                ", useStrictConfirmation=" + useStrictConfirmation +
                ", useMACDConfirmation=" + useMACDConfirmation +
                ", riskPercentage=" + riskPercentage +
                '}';
    }
}