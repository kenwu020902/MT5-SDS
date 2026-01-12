package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DecisionEngineTest {
    
    @Mock
    private TradingConfig config;
    
    @Mock
    private MT5Connector connector;
    
    @Mock
    private MT5DataFetcher dataFetcher;
    
    private DecisionEngine decisionEngine;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置mock行为
        when(config.getSymbol()).thenReturn("EURUSD");
        when(config.getRiskPercentage()).thenReturn(1.0);
        when(config.isTestMode()).thenReturn(true);
        when(config.getMaxPositionSize()).thenReturn(10.0);
        
        // 创建决策引擎实例（使用匿名类实现抽象类）
        decisionEngine = new DecisionEngine(config, connector) {
            @Override
            public void analyzeNewCandle(CandleData candle) {
                // 简单的测试实现：当价格超过特定阈值时交易
                if (candle.getClose() > 1.10500) {
                    executeTrade(config.getSymbol(), "BUY", 0.1);
                } else if (candle.getClose() < 1.09500) {
                    executeTrade(config.getSymbol(), "SELL", 0.1);
                }
            }
            
            @Override
            public void executeTrade(String symbol, String action, double volume) {
                // 调用真实的connector发送订单
                connector.sendOrder(symbol, action, volume, 0, 0, 0);
            }
        };
    }
    
    @Test
    void testDecisionEngineCreation() {
        assertNotNull(decisionEngine, "决策引擎应该被成功创建");
    }
    
    @Test
    void testAnalyzeNewCandle_BuySignal() {
        // 创建测试K线数据（高价触发买入）
        CandleData highPriceCandle = new CandleData(
            LocalDateTime.now(),
            1.10400,  // open
            1.10600,  // high
            1.10300,  // low
            1.10550,  // close - 超过买入阈值
            1000      // volume
        );
        
        // 模拟connector行为
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // 执行分析
        decisionEngine.analyzeNewCandle(highPriceCandle);
        
        // 验证是否调用了买入
        verify(connector, times(1)).sendOrder(
            eq("EURUSD"), eq("BUY"), eq(0.1), eq(0.0), eq(0.0), eq(0.0)
        );
    }
    
    @Test
    void testAnalyzeNewCandle_SellSignal() {
        // 创建测试K线数据（低价触发卖出）
        CandleData lowPriceCandle = new CandleData(
            LocalDateTime.now(),
            1.09600,  // open
            1.09700,  // high
            1.09400,  // low
            1.09450,  // close - 低于卖出阈值
            1000      // volume
        );
        
        // 模拟connector行为
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // 执行分析
        decisionEngine.analyzeNewCandle(lowPriceCandle);
        
        // 验证是否调用了卖出
        verify(connector, times(1)).sendOrder(
            eq("EURUSD"), eq("SELL"), eq(0.1), eq(0.0), eq(0.0), eq(0.0)
        );
    }
    
    @Test
    void testAnalyzeNewCandle_NoSignal() {
        // 创建测试K线数据（价格在中间，不触发任何信号）
        CandleData middlePriceCandle = new CandleData(
            LocalDateTime.now(),
            1.10000,  // open
            1.10100,  // high
            1.09900,  // low
            1.10050,  // close - 在阈值之间
            1000      // volume
        );
        
        // 执行分析
        decisionEngine.analyzeNewCandle(middlePriceCandle);
        
        // 验证没有调用交易
        verify(connector, never()).sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }
    
    @Test
    void testExecuteTrade_Success() {
        // 模拟成功的交易响应
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // 执行交易
        decisionEngine.executeTrade("EURUSD", "BUY", 0.5);
        
        // 验证connector被调用
        verify(connector, times(1)).sendOrder(
            eq("EURUSD"), eq("BUY"), eq(0.5), eq(0.0), eq(0.0), eq(0.0)
        );
    }
    
    @Test
    void testExecuteTrade_Failure() {
        // 模拟失败的交易响应
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(false));
        
        // 执行交易
        decisionEngine.executeTrade("EURUSD", "SELL", 0.2);
        
        // 验证connector被调用
        verify(connector, times(1)).sendOrder(
            eq("EURUSD"), eq("SELL"), eq(0.2), eq(0.0), eq(0.0), eq(0.0)
        );
    }
    
    @Test
    void testMultipleCandlesAnalysis() {
        // 创建多个测试K线数据
        List<CandleData> candles = new ArrayList<>();
        candles.add(new CandleData(LocalDateTime.now().minusMinutes(3), 1.09900, 1.10000, 1.09800, 1.09950, 1000));
        candles.add(new CandleData(LocalDateTime.now().minusMinutes(2), 1.10000, 1.10100, 1.09900, 1.10050, 1000));
        candles.add(new CandleData(LocalDateTime.now().minusMinutes(1), 1.10100, 1.10200, 1.10000, 1.10150, 1000));
        candles.add(new CandleData(LocalDateTime.now(), 1.10600, 1.10700, 1.10500, 1.10650, 1000)); // 触发买入
        
        // 模拟connector行为
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // 分析所有K线
        for (CandleData candle : candles) {
            decisionEngine.analyzeNewCandle(candle);
        }
        
        // 验证只有最后一根K线触发了交易
        verify(connector, times(1)).sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }
    
    @Test
    void testCandleDataFields() {
        // 测试CandleData类的功能
        LocalDateTime now = LocalDateTime.now();
        CandleData candle = new CandleData(now, 1.10000, 1.10100, 1.09900, 1.10050, 1500);
        
        assertEquals(now, candle.getTime(), "时间应该匹配");
        assertEquals(1.10000, candle.getOpen(), 0.00001, "开盘价应该匹配");
        assertEquals(1.10100, candle.getHigh(), 0.00001, "最高价应该匹配");
        assertEquals(1.09900, candle.getLow(), 0.00001, "最低价应该匹配");
        assertEquals(1.10050, candle.getClose(), 0.00001, "收盘价应该匹配");
        assertEquals(1500, candle.getVolume(), "成交量应该匹配");
        
        // 测试toString方法
        assertNotNull(candle.toString(), "toString不应该返回null");
        assertTrue(candle.toString().contains("1.10050"), "toString应该包含收盘价");
    }
    
    @Test
    void testConfigValues() {
        // 验证配置值
        assertEquals("EURUSD", config.getSymbol(), "交易品种应该匹配");
        assertEquals(1.0, config.getRiskPercentage(), 0.001, "风险比例应该匹配");
        assertTrue(config.isTestMode(), "测试模式应该为true");
        assertEquals(10.0, config.getMaxPositionSize(), 0.001, "最大仓位应该匹配");
    }
    
    // 简单的集成测试
    @Test
    void testIntegrationWithDataFetcher() {
        // 创建模拟的历史数据
        List<CandleData> mockData = new ArrayList<>();
        mockData.add(new CandleData(LocalDateTime.now().minusMinutes(10), 1.09500, 1.09600, 1.09400, 1.09550, 1000));
        mockData.add(new CandleData(LocalDateTime.now().minusMinutes(5), 1.10700, 1.10800, 1.10600, 1.10750, 1000)); // 触发买入
        
        // 模拟dataFetcher行为
        when(dataFetcher.fetchHistoricalData()).thenReturn(mockData);
        when(dataFetcher.getLastCandle()).thenReturn(mockData.get(mockData.size() - 1));
        
        // 模拟connector行为
        when(connector.sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // 获取并分析历史数据
        List<CandleData> historicalData = dataFetcher.fetchHistoricalData();
        assertNotNull(historicalData, "历史数据不应该为null");
        assertEquals(2, historicalData.size(), "应该有2根历史K线");
        
        // 分析每根K线
        for (CandleData candle : historicalData) {
            decisionEngine.analyzeNewCandle(candle);
        }
        
        // 验证交易被触发
        verify(connector, times(1)).sendOrder(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        
        // 验证最后K线
        CandleData lastCandle = dataFetcher.getLastCandle();
        assertNotNull(lastCandle, "最后K线不应该为null");
        assertEquals(1.10750, lastCandle.getClose(), 0.00001, "最后K线的收盘价应该匹配");
    }
}