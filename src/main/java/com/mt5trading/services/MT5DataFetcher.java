package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;

import java.util.List;

public class MT5DataFetcher {
    private final TradingConfig config;
    private final MT5Connector connector;
    private CandleData lastCandle;
    
    public MT5DataFetcher(TradingConfig config, MT5Connector connector) {
        this.config = config;
        this.connector = connector;
        this.lastCandle = null;
    }
    
    // 获取历史数据 - 简化版本
    public List<CandleData> fetchHistoricalData() {
        try {
            return connector.getHistoricalData(
                config.getSymbol(),
                config.getTimeframe(),
                config.getDataHistoryBars()
            );
        } catch (Exception e) {
            System.err.println("获取历史数据失败: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    // 获取当前价格 - 简化版本
    public double fetchCurrentPrice() {
        try {
            return connector.getCurrentPrice(config.getSymbol());
        } catch (Exception e) {
            System.err.println("获取当前价格失败: " + e.getMessage());
            return 0.0;
        }
    }
    
    // 获取实时K线数据
    public CandleData fetchRealtimeCandle() {
        return lastCandle;
    }
    
    // 更新最后一根K线
    public void updateLastCandle(CandleData newCandle) {
        this.lastCandle = newCandle;
    }
    
    public CandleData getLastCandle() {
        return lastCandle;
    }
}