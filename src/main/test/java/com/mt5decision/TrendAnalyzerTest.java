package main.test.java.com.mt5decision;

import com.mt5decision.core.models.*;
import com.mt5decision.core.services.TrendAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrendAnalyzerTest {
    
    private TrendAnalyzer strictAnalyzer;
    private TrendAnalyzer moderateAnalyzer;
    private TrendAnalyzer macdAnalyzer;
    private TrendAnalyzer structureAnalyzer;
    
    @BeforeEach
    public void setUp() {
        strictAnalyzer = new TrendAnalyzer(true, false);  // Strict, no MACD
        moderateAnalyzer = new TrendAnalyzer(false, false); // Moderate, no MACD
        macdAnalyzer = new TrendAnalyzer(false, true);    // Moderate with MACD
        structureAnalyzer = new TrendAnalyzer(false, false); // For structure tests
    }
    
    @Test
    @DisplayName("Test bullish confirmation with strict rules")
    public void testBullishConfirmationStrict() {
        // Previous bullish candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens above previous high (strict requirement)
        CandleData currCandle = new CandleData(111.0, 115.0, 108.0, 113.0, 1200, 
                                              LocalDateTime.now());
        
        TrendDirection result = strictAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.UPTREND, result, "Should confirm uptrend with strict rules");
    }
    
    @Test
    @DisplayName("Test bullish confirmation with moderate rules")
    public void testBullishConfirmationModerate() {
        // Previous bullish candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens above previous close (moderate requirement)
        CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 1200, 
                                              LocalDateTime.now());
        
        TrendDirection result = moderateAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.UPTREND, result, "Should confirm uptrend with moderate rules");
    }
    
    @Test
    @DisplayName("Test bearish confirmation with strict rules")
    public void testBearishConfirmationStrict() {
        // Previous bearish candle
        CandleData prevCandle = new CandleData(105.0, 108.0, 95.0, 98.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens below previous low (strict requirement)
        CandleData currCandle = new CandleData(94.0, 97.0, 92.0, 95.0, 1200, 
                                              LocalDateTime.now());
        
        TrendDirection result = strictAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.DOWNTREND, result, "Should confirm downtrend with strict rules");
    }
    
    @Test
    @DisplayName("Test bearish confirmation with moderate rules")
    public void testBearishConfirmationModerate() {
        // Previous bearish candle
        CandleData prevCandle = new CandleData(105.0, 108.0, 95.0, 98.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens below previous close (moderate requirement)
        CandleData currCandle = new CandleData(97.0, 100.0, 96.0, 99.0, 1200, 
                                              LocalDateTime.now());
        
        TrendDirection result = moderateAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.DOWNTREND, result, "Should confirm downtrend with moderate rules");
    }
    
    @Test
    @DisplayName("Test MACD confirmation for uptrend")
    public void testMACDConfirmationUptrend() {
        // Previous bullish candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens above previous close
        CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 1200, 
                                              LocalDateTime.now());
        
        // Bullish MACD (MACD line > Signal line)
        MACDData macd = new MACDData(0.5, 0.3, 0.2, 0.4, 0.35, 0.05);
        
        TrendDirection result = macdAnalyzer.confirmTrend(prevCandle, currCandle, macd);
        assertEquals(TrendDirection.UPTREND, result, "Should confirm uptrend with MACD");
    }
    
    @Test
    @DisplayName("Test MACD rejection for false uptrend")
    public void testMACDRejectionUptrend() {
        // Previous bullish candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle opens above previous close
        CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 1200, 
                                              LocalDateTime.now());
        
        // Bearish MACD (MACD line < Signal line)
        MACDData macd = new MACDData(0.3, 0.5, -0.2, 0.35, 0.45, -0.1);
        
        TrendDirection result = macdAnalyzer.confirmTrend(prevCandle, currCandle, macd);
        assertEquals(TrendDirection.NONE, result, "Should reject uptrend with bearish MACD");
    }
    
    @Test
    @DisplayName("Test no trend when previous candle is neutral")
    public void testNoTrendWithNeutralCandle() {
        // Previous neutral candle (doji)
        CandleData prevCandle = new CandleData(100.0, 102.0, 98.0, 100.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle
        CandleData currCandle = new CandleData(101.0, 103.0, 99.0, 102.0, 1200, 
                                              LocalDateTime.now());
        
        TrendDirection result = strictAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.NONE, result, "Should not confirm trend with neutral candle");
    }
    
    @Test
    @DisplayName("Test market structure with higher highs and lows")
    public void testMarketStructureUptrend() {
        // Create candles with higher highs and higher lows
        List<CandleData> recentCandles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(75);
        
        // Candles in descending order (most recent first)
        recentCandles.add(new CandleData(115.0, 120.0, 112.0, 118.0, 1200, baseTime.plusMinutes(60)));
        recentCandles.add(new CandleData(110.0, 118.0, 108.0, 116.0, 1100, baseTime.plusMinutes(45)));
        recentCandles.add(new CandleData(105.0, 115.0, 103.0, 113.0, 1000, baseTime.plusMinutes(30)));
        recentCandles.add(new CandleData(100.0, 110.0, 98.0, 108.0, 900, baseTime.plusMinutes(15)));
        recentCandles.add(new CandleData(95.0, 105.0, 93.0, 103.0, 800, baseTime));
        
        // Test previous and current candles
        CandleData prevCandle = recentCandles.get(1); // Second most recent
        CandleData currCandle = recentCandles.get(0); // Most recent
        
        TrendDirection result = structureAnalyzer.confirmTrendWithStructure(
            prevCandle, currCandle, recentCandles, null);
        
        assertEquals(TrendDirection.UPTREND, result, "Should confirm uptrend with valid structure");
    }
    
    @Test
    @DisplayName("Test market structure with lower highs and lows")
    public void testMarketStructureDowntrend() {
        // Create candles with lower highs and lower lows
        List<CandleData> recentCandles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(75);
        
        // Candles in descending order (most recent first)
        recentCandles.add(new CandleData(95.0, 98.0, 92.0, 96.0, 1200, baseTime.plusMinutes(60)));
        recentCandles.add(new CandleData(100.0, 103.0, 97.0, 101.0, 1100, baseTime.plusMinutes(45)));
        recentCandles.add(new CandleData(105.0, 108.0, 102.0, 106.0, 1000, baseTime.plusMinutes(30)));
        recentCandles.add(new CandleData(110.0, 113.0, 107.0, 111.0, 900, baseTime.plusMinutes(15)));
        recentCandles.add(new CandleData(115.0, 118.0, 112.0, 116.0, 800, baseTime));
        
        // Test previous and current candles
        CandleData prevCandle = recentCandles.get(1); // Second most recent (bearish)
        CandleData currCandle = recentCandles.get(0); // Most recent
        
        TrendDirection result = structureAnalyzer.confirmTrendWithStructure(
            prevCandle, currCandle, recentCandles, null);
        
        assertEquals(TrendDirection.DOWNTREND, result, "Should confirm downtrend with valid structure");
    }
    
    @Test
    @DisplayName("Test failed market structure validation")
    public void testFailedMarketStructure() {
        // Create candles with inconsistent structure
        List<CandleData> recentCandles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(75);
        
        recentCandles.add(new CandleData(115.0, 120.0, 112.0, 118.0, 1200, baseTime.plusMinutes(60)));
        recentCandles.add(new CandleData(110.0, 118.0, 108.0, 116.0, 1100, baseTime.plusMinutes(45)));
        recentCandles.add(new CandleData(105.0, 110.0, 103.0, 108.0, 1000, baseTime.plusMinutes(30))); // Lower high
        recentCandles.add(new CandleData(100.0, 115.0, 98.0, 113.0, 900, baseTime.plusMinutes(15))); // Higher high
        recentCandles.add(new CandleData(95.0, 105.0, 93.0, 103.0, 800, baseTime));
        
        CandleData prevCandle = recentCandles.get(1);
        CandleData currCandle = recentCandles.get(0);
        
        TrendDirection result = structureAnalyzer.confirmTrendWithStructure(
            prevCandle, currCandle, recentCandles, null);
        
        assertEquals(TrendDirection.NONE, result, "Should reject trend with invalid structure");
    }
    
    @Test
    @DisplayName("Test insufficient candles for structure analysis")
    public void testInsufficientCandlesForStructure() {
        // Only 2 candles - insufficient for structure analysis
        List<CandleData> recentCandles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(15);
        
        recentCandles.add(new CandleData(110.0, 115.0, 108.0, 113.0, 1200, baseTime.plusMinutes(15)));
        recentCandles.add(new CandleData(100.0, 110.0, 98.0, 108.0, 1000, baseTime));
        
        CandleData prevCandle = recentCandles.get(1);
        CandleData currCandle = recentCandles.get(0);
        
        TrendDirection result = structureAnalyzer.confirmTrendWithStructure(
            prevCandle, currCandle, recentCandles, null);
        
        assertEquals(TrendDirection.NONE, result, "Should reject with insufficient candles");
    }
    
    @Test
    @DisplayName("Test null input handling")
    public void testNullInputHandling() {
        // Test with null inputs
        TrendDirection result1 = strictAnalyzer.confirmTrend(null, null, null);
        assertEquals(TrendDirection.NONE, result1, "Should return NONE with null inputs");
        
        // Test with null current candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        TrendDirection result2 = strictAnalyzer.confirmTrend(prevCandle, null, null);
        assertEquals(TrendDirection.NONE, result2, "Should return NONE with null current candle");
        
        // Test with null previous candle
        CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 1200, 
                                              LocalDateTime.now());
        TrendDirection result3 = strictAnalyzer.confirmTrend(null, currCandle, null);
        assertEquals(TrendDirection.NONE, result3, "Should return NONE with null previous candle");
    }
    
    @Test
    @DisplayName("Test edge cases for strict confirmation")
    public void testStrictConfirmationEdgeCases() {
        // Previous bullish candle
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Test with opening exactly at previous high (should pass in strict mode)
        CandleData currCandle1 = new CandleData(110.0, 115.0, 108.0, 113.0, 1200, 
                                               LocalDateTime.now());
        TrendDirection result1 = strictAnalyzer.confirmTrend(prevCandle, currCandle1, null);
        assertEquals(TrendDirection.NONE, result1, "Should not confirm when open equals previous high");
        
        // Test with opening slightly above previous high
        CandleData currCandle2 = new CandleData(110.01, 115.0, 108.0, 113.0, 1200, 
                                               LocalDateTime.now());
        TrendDirection result2 = strictAnalyzer.confirmTrend(prevCandle, currCandle2, null);
        assertEquals(TrendDirection.UPTREND, result2, "Should confirm when open > previous high");
    }
    
    @Test
    @DisplayName("Test volume increase confirmation")
    public void testVolumeConfirmation() {
        // Previous bullish candle with low volume
        CandleData prevCandle = new CandleData(100.0, 110.0, 95.0, 105.0, 1000, 
                                              LocalDateTime.now().minusMinutes(15));
        
        // Current candle with increased volume (bullish confirmation)
        CandleData currCandle = new CandleData(106.0, 115.0, 104.0, 112.0, 2500, 
                                              LocalDateTime.now());
        
        TrendDirection result = moderateAnalyzer.confirmTrend(prevCandle, currCandle, null);
        assertEquals(TrendDirection.UPTREND, result, "Should confirm uptrend with volume increase");
        
        // Test with decreasing volume (still should confirm based on price)
        CandleData currCandleLowVolume = new CandleData(106.0, 115.0, 104.0, 112.0, 500, 
                                                       LocalDateTime.now());
        TrendDirection result2 = moderateAnalyzer.confirmTrend(prevCandle, currCandleLowVolume, null);
        assertEquals(TrendDirection.UPTREND, result2, "Should still confirm even with low volume");
    }
}