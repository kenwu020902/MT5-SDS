package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;

public abstract class DecisionEngine {
    protected final TradingConfig config;
    protected final MT5Connector connector;
    
    public DecisionEngine(TradingConfig config, MT5Connector connector) {
        this.config = config;
        this.connector = connector;
    }
    
    public abstract void analyzeNewCandle(CandleData candle);
    public abstract void executeTrade(String symbol, String action, double volume);
}