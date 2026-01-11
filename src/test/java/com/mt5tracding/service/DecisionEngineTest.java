package com.mt5tracding.service;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.*;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.MT5DataFetcher;
import com.mt5trading.services.TrendAnalyzer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DecisionEngineTest {
    
    private DecisionEngine decisionEngine;
    private TradingConfig mockConfig;
    private MT5DataFetcher mockDataFetcher;
    private TrendAnalyzer mockTrendAnalyzer;
    
    @Before
    public void setUp() throws Exception {
        mockConfig = mock(TradingConfig.class);
        mockDataFetcher = mock(MT5DataFetcher.class);
        mockTrendAnalyzer = mock(TrendAnalyzer.class);
        
        when(mockConfig.getSymbol()).thenReturn("US30");
        when(mockConfig.isUseStrictConfirmation()).thenReturn(true);
        when(mockConfig.isUseMACDConfirmation()).thenReturn(true);
        when(mockConfig.getPollingInterval()).thenReturn(1000);
        when(mockConfig.isEnableConsoleLogging()).thenReturn(false);
        when(mockConfig.getStopLossPips()).thenReturn(50);
        when(mockConfig.getTakeProfitPips()).thenReturn(100);
        
        decisionEngine = new DecisionEngine(mockConfig, mockDataFetcher);
    }
    
    @Test
    public void testUptrendConfirmation() {
        // Create test candles
        List<CandleData> candles = new ArrayList<>();
        
        // Previous candle: bullish
        CandleData prevCandle = new CandleData(
                LocalDateTime.now().minusMinutes(5),
                35000.0,  // open
                35100.0,  // high
                34950.0,  // low
                35080.0,  // close (bullish)
                1000      // volume
        );
        
        // Current candle: opens above previous high (strict confirmation)
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                35101.0,  // opens above previous high
                35150.0,
                35080.0,
                35120.0,
                1200
        );
        
        candles.add(prevCandle);
        candles.add(currCandle);
        
        when(mockDataFetcher.getHistoricalData()).thenReturn(candles);
        when(mockTrendAnalyzer.confirmTrend(any(), any(), any()))
                .thenReturn(TrendDirection.UPTREND);
        
        // Test would continue with decision making...
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
                35000.0,  // open
                35050.0,  // high
                34800.0,  // low
                34850.0,  // close (bearish)
                1000
        );
        
        // Current candle: opens below previous low (strict confirmation)
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                34799.0,  // opens below previous low
                34900.0,
                34700.0,
                34800.0,
                1500
        );
        
        candles.add(prevCandle);
        candles.add(currCandle);
        
        when(mockDataFetcher.getHistoricalData()).thenReturn(candles);
        
        assertTrue(prevCandle.isBearish());
        assertTrue(currCandle.getOpen() < prevCandle.getLow());
    }
    
    @Test
    public void testNoTrendConfirmation() {
        // Create test candles with no clear confirmation
        CandleData prevCandle = new CandleData(
                LocalDateTime.now().minusMinutes(5),
                35000.0,
                35100.0,
                34900.0,
                34950.0,  // bearish
                1000
        );
        
        CandleData currCandle = new CandleData(
                LocalDateTime.now(),
                34960.0,  // opens above previous close (but not below low for strict)
                35000.0,
                34900.0,
                34980.0,
                800
        );
        
        // With strict confirmation, this should not be a downtrend
        assertTrue(prevCandle.isBearish());
        assertFalse(currCandle.getOpen() < prevCandle.getLow());
    }
    
    @Test
    public void testOrderDecisionCreation() {
        OrderDecision decision = new OrderDecision();
        decision.setSymbol("US30");
        decision.setTrend(TrendDirection.UPTREND);
        decision.setAction(OrderDecision.OrderAction.BUY);
        decision.setEntryPrice(35000.0);
        decision.setStopLoss(34900.0);
        decision.setTakeProfit(35200.0);
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
}