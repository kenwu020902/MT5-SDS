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
    
    // ========== WebSocket 配置 ==========
    public String getMt5WebSocketUrl() {
        return properties.getProperty("mt5.websocket.url", "ws://localhost:8080");
    }
    
    public int getWebSocketReconnectInterval() {
        return Integer.parseInt(properties.getProperty("mt5.websocket.reconnect.interval", "5000"));
    }
    
    public int getWebSocketHeartbeatInterval() {
        return Integer.parseInt(properties.getProperty("mt5.websocket.heartbeat.interval", "30000"));
    }
    
    // ========== MT5 连接配置 ==========
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
    
    // ========== 交易品种配置 ==========
    public String getSymbol() {
        return properties.getProperty("mt5.symbol", "US30");
    }
    
    // 添加时间框架配置（秒）
    public int getTimeframe() {
        return Integer.parseInt(properties.getProperty("mt5.timeframe.seconds", "60"));
    }
    
    // ========== 用户订单检测系统配置 ==========
    // 订单管理配置
    public boolean isAutoPauseOrders() {
        return Boolean.parseBoolean(properties.getProperty("trading.auto.pause.orders", "true"));
    }
    
    public boolean isAutoCancelOrders() {
        return Boolean.parseBoolean(properties.getProperty("trading.auto.cancel.orders", "false"));
    }
    
    public int getOrderScanInterval() {
        return Integer.parseInt(properties.getProperty("trading.order.scan.interval", "3"));
    }
    
    public int getMaxOrderHoldTime() {
        return Integer.parseInt(properties.getProperty("trading.max.order.hold.time", "120"));
    }
    
    // 趋势分析配置
    public double getStrongBullishThreshold() {
        return Double.parseDouble(properties.getProperty("trading.strong.bullish.threshold", "0.10"));
    }
    
    public double getBullishThreshold() {
        return Double.parseDouble(properties.getProperty("trading.bullish.threshold", "0.04"));
    }
    
    public double getBearishThreshold() {
        return Double.parseDouble(properties.getProperty("trading.bearish.threshold", "-0.04"));
    }
    
    public double getStrongBearishThreshold() {
        return Double.parseDouble(properties.getProperty("trading.strong.bearish.threshold", "-0.10"));
    }
    
    // 价格分析配置
    public double getPriceTolerance() {
        return Double.parseDouble(properties.getProperty("trading.price.tolerance", "20.0"));
    }
    
    public int getPriceHistorySize() {
        return Integer.parseInt(properties.getProperty("trading.price.history.size", "10"));
    }
    
    // 决策参数
    public double getNeutralBuyAdvantage() {
        return Double.parseDouble(properties.getProperty("trading.neutral.buy.advantage", "15.0"));
    }
    
    public double getNeutralSellAdvantage() {
        return Double.parseDouble(properties.getProperty("trading.neutral.sell.advantage", "15.0"));
    }
    
    // ========== 通用交易参数 ==========
    public double getTradeVolume() {
        return Double.parseDouble(properties.getProperty("trading.volume", "0.1"));
    }
    
    public double getRiskPercentage() {
        return Double.parseDouble(properties.getProperty("trading.risk.percentage", "1.0"));
    }
    
    public boolean isUseStrictConfirmation() {
        return Boolean.parseBoolean(properties.getProperty("trading.use.strict.confirmation", "true"));
    }
    
    public boolean isUseMACDConfirmation() {
        return Boolean.parseBoolean(properties.getProperty("trading.use.macd.confirmation", "true"));
    }
    
    public double getSlippage() {
        return Double.parseDouble(properties.getProperty("trading.slippage", "2.0"));
    }
    
    public double getMaxPositionSize() {
        return Double.parseDouble(properties.getProperty("trading.max.position.size", "5.0"));
    }
    
    public int getStopLossPips() {
        return Integer.parseInt(properties.getProperty("trading.stop.loss.pips", "80"));
    }
    
    public int getTakeProfitPips() {
        return Integer.parseInt(properties.getProperty("trading.take.profit.pips", "120"));
    }
    
    // ========== MACD 参数 ==========
    public int getMacdFast() {
        return Integer.parseInt(properties.getProperty("trading.macd.fast", "12"));
    }
    
    public int getMacdSlow() {
        return Integer.parseInt(properties.getProperty("trading.macd.slow", "26"));
    }
    
    public int getMacdSignal() {
        return Integer.parseInt(properties.getProperty("trading.macd.signal", "9"));
    }
    
    // ========== 应用设置 ==========
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
    
    // ========== 新增：系统监控配置 ==========
    public boolean isEnableOrderMonitoring() {
        return Boolean.parseBoolean(properties.getProperty("app.enable.order.monitoring", "true"));
    }
    
    public int getCandleAnalysisSecond() {
        return Integer.parseInt(properties.getProperty("app.candle.analysis.second", "45"));
    }
    
    public boolean isLogDetailedAnalysis() {
        return Boolean.parseBoolean(properties.getProperty("app.log.detailed.analysis", "true"));
    }
    
    public String getSystemOrderComment() {
        return properties.getProperty("app.system.order.comment", "AUTO_TRADE");
    }
    
    public String getUserOrderComment() {
        return properties.getProperty("app.user.order.comment", "USER_ORDER");
    }
    
    // ========== 新增：风险管理配置 ==========
    public double getMaxRiskPerTrade() {
        return Double.parseDouble(properties.getProperty("trading.max.risk.per.trade", "0.02"));
    }
    
    public double getMaxDailyLoss() {
        return Double.parseDouble(properties.getProperty("trading.max.daily.loss", "0.10"));
    }
    
    public boolean isEnableRiskManagement() {
        return Boolean.parseBoolean(properties.getProperty("trading.enable.risk.management", "true"));
    }
    
    // ========== 新增：时间配置 ==========
    public String getTradingStartTime() {
        return properties.getProperty("trading.start.time", "09:30");
    }
    
    public String getTradingEndTime() {
        return properties.getProperty("trading.end.time", "16:00");
    }
    
    public boolean isTradeOnWeekends() {
        return Boolean.parseBoolean(properties.getProperty("trading.on.weekends", "false"));
    }
    
    // ========== 工具方法 ==========
    public void printConfigSummary() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("交易系统配置摘要");
        System.out.println("=".repeat(50));
        
        System.out.println("🎯 交易品种: " + getSymbol());
        System.out.println("⏰ 时间框架: " + getTimeframe() + "秒");
        System.out.println("🔍 分析时间: 第" + getCandleAnalysisSecond() + "秒");
        System.out.println("🧪 测试模式: " + (isTestMode() ? "是" : "否"));
        
        System.out.println("\n📋 订单管理:");
        System.out.println("   自动暂停订单: " + (isAutoPauseOrders() ? "是" : "否"));
        System.out.println("   自动取消订单: " + (isAutoCancelOrders() ? "是" : "否"));
        System.out.println("   订单扫描间隔: " + getOrderScanInterval() + "秒");
        
        System.out.println("\n📈 趋势分析阈值:");
        System.out.println("   强烈看涨: " + getStrongBullishThreshold() + "%");
        System.out.println("   看涨: " + getBullishThreshold() + "%");
        System.out.println("   看跌: " + getBearishThreshold() + "%");
        System.out.println("   强烈看跌: " + getStrongBearishThreshold() + "%");
        
        System.out.println("\n⚙️ 交易参数:");
        System.out.println("   默认手数: " + getTradeVolume());
        System.out.println("   止损点数: " + getStopLossPips());
        System.out.println("   止盈点数: " + getTakeProfitPips());
        System.out.println("   价格容忍度: " + getPriceTolerance() + "点");
        
        System.out.println("=".repeat(50));
    }
    
    // 验证配置有效性
    public boolean validateConfig() {
        try {
            // 检查必需配置
            if (getSymbol() == null || getSymbol().trim().isEmpty()) {
                System.err.println("错误: 交易品种未配置");
                return false;
            }
            
            if (getTimeframe() <= 0) {
                System.err.println("错误: 时间框架必须大于0");
                return false;
            }
            
            if (getCandleAnalysisSecond() <= 0 || getCandleAnalysisSecond() >= getTimeframe()) {
                System.err.println("错误: 分析时间必须在0到" + getTimeframe() + "秒之间");
                return false;
            }
            
            // 检查阈值逻辑
            if (getStrongBullishThreshold() <= getBullishThreshold()) {
                System.err.println("警告: 强烈看涨阈值应大于看涨阈值");
            }
            
            if (getBearishThreshold() <= getStrongBearishThreshold()) {
                System.err.println("警告: 看跌阈值应大于强烈看跌阈值");
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("配置验证失败: " + e.getMessage());
            return false;
        }
    }
}