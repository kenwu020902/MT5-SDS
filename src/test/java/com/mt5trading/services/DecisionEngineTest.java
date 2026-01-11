package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.*;
import com.mt5trading.mt5.connector.MT5Connector;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DecisionEngineTest {
    
    private DecisionEngine decisionEngine;
    private TradingConfig mockConfig;
    private MT5DataFetcher mockDataFetcher;
    private MT5Connector mockConnector;
    
    @Before
    public void setUp() throws Exception {
        mockConfig = mock(TradingConfig.class);
        mockDataFetcher = mock(MT5DataFetcher.class);
        mockConnector = mock(MT5Connector.class);
        
        // 设置mock行为
        when(mockDataFetcher.getConnector()).thenReturn(mockConnector);
        when(mockConnector.getConfig()).thenReturn(mockConfig);
        
        // 模拟MT5Connector的getAccountBalance方法 - 返回CompletableFuture
        when(mockConnector.getAccountBalance()).thenReturn(
            CompletableFuture.completedFuture(10000.0)
        );
        
        // 配置TradingConfig
        when(mockConfig.getSymbol()).thenReturn("EURUSD");
        when(mockConfig.isUseStrictConfirmation()).thenReturn(true);
        when(mockConfig.isUseMACDConfirmation()).thenReturn(true);
        when(mockConfig.getPollingInterval()).thenReturn(1000);
        when(mockConfig.isEnableConsoleLogging()).thenReturn(false);
        when(mockConfig.getStopLossPips()).thenReturn(20);
        when(mockConfig.getTakeProfitPips()).thenReturn(40);
        when(mockConfig.getMacdFastPeriod()).thenReturn(12);
        when(mockConfig.getMacdSlowPeriod()).thenReturn(26);
        when(mockConfig.getMacdSignalPeriod()).thenReturn(9);
        when(mockConfig.getMaxPositionSize()).thenReturn(10.0);
        when(mockConfig.getRiskPercentage()).thenReturn(2.0);
        when(mockConfig.getDataHistoryBars()).thenReturn(100);
        when(mockConfig.getTimeframe()).thenReturn("PERIOD_H1");
        
        // 创建DecisionEngine实例
        decisionEngine = new DecisionEngine(mockConfig, mockDataFetcher);
    }
    
    @Test
    public void testUptrendConfirmation() {
        // Create test candles
        List<CandleData> candles = new ArrayList<>();
        
        // Previous candle: bullish
        CandleData prevCandle = new CandleData(
                LocalDateTime.now().minusMinutes(5),
                1.10000,  // open
                1.10100,  // high
                1.09950,  // low
                1.10080,  // close (bullish)
                1000      // volume
        );
        
        // Current candle: opens above previous high (strict confirmation)
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                1.10101,  // opens above previous high
                1.10150,
                1.10080,
                1.10120,
                1200
        );
        
        candles.add(prevCandle);
        candles.add(currCandle);
        
        // 模拟数据获取器返回历史数据 - 返回CompletableFuture
        when(mockDataFetcher.loadHistoricalData()).thenReturn(
            CompletableFuture.completedFuture(candles)
        );
        
        assertTrue(prevCandle.isBullish());
        assertTrue(currCandle.getOpen() > prevCandle.getHigh());
    }
    
    @Test
    public void testDowntrendConfirmation() {
        // Create test candles
        List<CandleData> candles = new ArrayList<>();
        
        // Previous candle: bearish
        CandleData prevCandle = new CandleData(
                LocalDateTime.now().minusMinutes(5),
                1.10000,  // open
                1.10050,  // high
                1.09800,  // low
                1.09850,  // close (bearish)
                1000
        );
        
        // Current candle: opens below previous low (strict confirmation)
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                1.09799,  // opens below previous low
                1.09900,
                1.09700,
                1.09800,
                1500
        );
        
        candles.add(prevCandle);
        candles.add(currCandle);
        
        // 注意：这里模拟的是loadHistoricalData()，而不是getHistoricalData()
        when(mockDataFetcher.loadHistoricalData()).thenReturn(
            CompletableFuture.completedFuture(candles)
        );
        
        assertTrue(prevCandle.isBearish());
        assertTrue(currCandle.getOpen() < prevCandle.getLow());
    }
    
    @Test
    public void testNoTrendConfirmation() {
        // Create test candles with no clear confirmation
        CandleData prevCandle = new CandleData(
                LocalDateTime.now().minusMinutes(5),
                1.10000,
                1.10100,
                1.09900,
                1.09950,  // bearish
                1000
        );
        
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                1.09960,  // opens above previous close (but not below low for strict)
                1.10000,
                1.09900,
                1.09980,
                800
        );
        
        // With strict confirmation, this should not be a downtrend
        assertTrue(prevCandle.isBearish());
        assertFalse(currCandle.getOpen() < prevCandle.getLow());
    }
    
    @Test
    public void testOrderDecisionCreation() {
        OrderDecision decision = new OrderDecision();
        decision.setSymbol("EURUSD");
        decision.setTrend(TrendDirection.UPTREND);
        decision.setAction(OrderDecision.OrderAction.BUY);
        decision.setEntryPrice(1.10000);
        decision.setStopLoss(1.09900);
        decision.setTakeProfit(1.10200);
        decision.setPositionSize(0.1);
        decision.setConfidence(0.75);
        decision.setReason("Strong uptrend confirmation");
        
        assertTrue(decision.isBuySignal());
        assertFalse(decision.isSellSignal());
        assertTrue(decision.shouldExecute());
        assertEquals(2.0, decision.getRiskRewardRatio(), 0.1);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testInvalidOrderDecision() {
        OrderDecision decision = new OrderDecision();
        decision.setEntryPrice(0); // Invalid
        decision.validate();
    }
    
    @Test
    public void testCandleDataMethods() {
        CandleData candle = new CandleData(
            LocalDateTime.now(),
            1.10000,
            1.10100,
            1.09900,
            1.10050,
            1000
        );
        
        assertEquals(0.0005, candle.getBodySize(), 0.00001);
        assertEquals(0.0020, candle.getTotalRange(), 0.00001);
        assertEquals(0.0005, candle.getUpperWick(), 0.00001);
        assertEquals(0.0010, candle.getLowerWick(), 0.00001);
        assertEquals(1.10025, candle.getBodyMidpoint(), 0.00001);
    }
    
    @Test
    public void testMACDDataMethods() {
        MACDData macd = new MACDData(
            0.0010,   // macdLine
            0.0005,   // signalLine
            0.0005,   // histogram
            12,       // fastPeriod
            26,       // slowPeriod
            9         // signalPeriod
        );
        
        assertTrue(macd.isBullish());
        assertTrue(macd.hasBullishCrossover());
        assertFalse(macd.hasBearishCrossover());
        assertEquals(0.0005, macd.getCrossoverValue(), 0.00001);
    }
    
    @Test
    public void testTrendDirectionEnum() {
        assertEquals("Uptrend", TrendDirection.UPTREND.getDescription());
        assertTrue(TrendDirection.UPTREND.isBullish());
        assertFalse(TrendDirection.UPTREND.isBearish());
        assertTrue(TrendDirection.DOWNTREND.isBearish());
        assertFalse(TrendDirection.DOWNTREND.isBullish());
        
        assertEquals(TrendDirection.UPTREND, TrendDirection.fromString("UPTREND"));
        assertEquals(TrendDirection.UPTREND, TrendDirection.fromString("uptrend"));
        assertEquals(TrendDirection.NONE, TrendDirection.fromString("invalid"));
    }
    
    @Test
    public void testDecisionEngineStart() {
        // 创建测试数据
        List<CandleData> testCandles = new ArrayList<>();
        testCandles.add(new CandleData(
            LocalDateTime.now().minusMinutes(10),
            1.10000,
            1.10100,
            1.09900,
            1.10050,
            1000
        ));
        testCandles.add(new CandleData(
            LocalDateTime.now().minusMinutes(5),
            1.10050,
            1.10150,
            1.10000,
            1.10100,
            1200
        ));
        
        // 模拟loadHistoricalData返回CompletableFuture
        when(mockDataFetcher.loadHistoricalData()).thenReturn(
            CompletableFuture.completedFuture(testCandles)
        );
        
        // 启动决策引擎
        decisionEngine.start();
        
        // 验证方法被调用
        verify(mockDataFetcher, atLeastOnce()).loadHistoricalData();
        
        // 停止引擎
        decisionEngine.stop();
    }
}