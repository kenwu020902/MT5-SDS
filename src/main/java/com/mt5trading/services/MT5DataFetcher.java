package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.mt5.models.MT5Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MT5DataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(MT5DataFetcher.class);
    
    private final MT5Connector connector;
    private final TradingConfig config;
    private final ObjectMapper objectMapper;
    private final List<CandleData> historicalData;
    private CandleData lastCandle;
    
    public MT5DataFetcher(MT5Connector connector) {
        this.connector = connector;
        this.config = connector.getConfig();
        this.objectMapper = new ObjectMapper();
        this.historicalData = new ArrayList<>();
        this.lastCandle = null;
    }
    
    public CompletableFuture<List<CandleData>> loadHistoricalData() {
        logger.info("Loading historical data for {} ({} bars)", 
                   config.getSymbol(), config.getDataHistoryBars());
        
        return connector.getHistoricalData(config.getSymbol(), config.getTimeframe(), 
                                          config.getDataHistoryBars())
                .thenApply(response -> {
                    if (response.isSuccess() && response.getCandles() != null) {
                        List<CandleData> candles = convertToCandleDataList(response);
                        historicalData.clear();
                        historicalData.addAll(candles);
                        
                        if (!candles.isEmpty()) {
                            lastCandle = candles.get(candles.size() - 1);
                        }
                        
                        logger.info("Loaded {} historical candles", candles.size());
                        return candles;
                    } else {
                        logger.error("Failed to load historical data: {}", response.getError());
                        return new ArrayList<>();
                    }
                });
    }
    
    private List<CandleData> convertToCandleDataList(MT5Response response) {
        List<CandleData> candles = new ArrayList<>();
        
        if (response.getCandles() != null) {
            for (MT5Response.MT5CandleData mt5Candle : response.getCandles()) {
                CandleData candle = convertToCandleData(mt5Candle);
                if (candle != null) {
                    candles.add(candle);
                }
            }
        }
        
        return candles;
    }
    
    private CandleData convertToCandleData(MT5Response.MT5CandleData mt5Candle) {
        try {
            LocalDateTime time = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(mt5Candle.getTime())), 
                    ZoneId.systemDefault());
            
            return new CandleData(
                    time,
                    mt5Candle.getOpen(),
                    mt5Candle.getHigh(),
                    mt5Candle.getLow(),
                    mt5Candle.getClose(),
                    mt5Candle.getVolume()
            );
        } catch (Exception e) {
            logger.error("Error converting MT5 candle data", e);
            return null;
        }
    }
    
    public CompletableFuture<CandleData> getCurrentCandle() {
        return connector.getCurrentPrice(config.getSymbol())
                .thenApply(response -> {
                    if (response.isSuccess() && response.getCandles() != null && 
                        response.getCandles().length > 0) {
                        CandleData candle = convertToCandleData(response.getCandles()[0]);
                        updateLastCandle(candle);
                        return candle;
                    }
                    return null;
                });
    }
    
    private void updateLastCandle(CandleData candle) {
        if (candle != null && isNewCandle(candle)) {
            updateCandleData(candle);
        }
    }
    
    private boolean isNewCandle(CandleData newCandle) {
        if (lastCandle == null) return true;
        return !newCandle.getTime().equals(lastCandle.getTime());
    }
    
    public void updateCandleData(CandleData newCandle) {
        if (newCandle != null && isNewCandle(newCandle)) {
            historicalData.add(newCandle);
            if (historicalData.size() > config.getDataHistoryBars()) {
                historicalData.remove(0);
            }
            lastCandle = newCandle;
            logger.debug("Updated candle data: {}", newCandle);
        }
    }
    
    public List<CandleData> getHistoricalData() {
        return new ArrayList<>(historicalData);
    }
    
    public CandleData getLastCandle() {
        return lastCandle;
    }
    
    public CandleData getPreviousCandle() {
        if (historicalData.size() >= 2) {
            return historicalData.get(historicalData.size() - 2);
        }
        return null;
    }
    
    public MT5Connector getConnector() {
        return connector;
    }
}