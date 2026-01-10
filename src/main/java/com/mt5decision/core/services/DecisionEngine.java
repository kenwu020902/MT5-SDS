package main.java.com.mt5decision.core.services;

import com.mt5decision.core.models.*;

public class DecisionEngine {
    private TrendAnalyzer trendAnalyzer;
    private MACDCalculator macdCalculator;
    private MT5DataFetcher dataFetcher;
    private TradingConfig config;
    
    public DecisionEngine(TradingConfig config) {
        this.config = config;
        this.trendAnalyzer = new TrendAnalyzer(
            config.isUseStrictConfirmation(),
            config.isUseMACDConfirmation()
        );
        this.macdCalculator = new MACDCalculator();
        this.dataFetcher = new MT5DataFetcher(config.getSymbol(), config.getTimeframe());
    }
    
    public OrderDecision makeDecision() {
        // Fetch required data
        CandleData previousCandle = dataFetcher.getPreviousCandle();
        CandleData currentCandle = dataFetcher.getCurrentCandle();
        
        if (previousCandle == null || currentCandle == null) {
            return OrderDecision.noDecision("Failed to fetch candle data");
        }
        
        // Check if new candle has opened
        if (!dataFetcher.isNewCandleOpen(previousCandle)) {
            return OrderDecision.noDecision("Waiting for new candle");
        }
        
        // Calculate MACD if needed
        MACDData macdData = null;
        if (config.isUseMACDConfirmation()) {
            List<CandleData> recentCandles = dataFetcher.fetchCandles(50);
            macdData = macdCalculator.calculateMACD(recentCandles);
        }
        
        // Analyze trend
        TrendDirection trend;
        if (config.isCheckMarketStructure()) {
            List<CandleData> recentCandles = dataFetcher.fetchCandles(10);
            trend = trendAnalyzer.confirmTrendWithStructure(
                previousCandle, currentCandle, recentCandles, macdData
            );
        } else {
            trend = trendAnalyzer.confirmTrend(previousCandle, currentCandle, macdData);
        }
        
        // Make order decision based on trend
        switch (trend) {
            case UPTREND:
                return createBuyDecision(previousCandle, currentCandle);
            case DOWNTREND:
                return createSellDecision(previousCandle, currentCandle);
            default:
                return OrderDecision.noDecision("No clear trend confirmation");
        }
    }
    
    private OrderDecision createBuyDecision(CandleData previous, CandleData current) {
        // Calculate position size, stop loss, take profit
        double entryPrice = current.getOpen();
        double stopLoss = previous.getLow() - config.getStopLossBuffer();
        double takeProfit = entryPrice + (entryPrice - stopLoss) * config.getRiskRewardRatio();
        
        return OrderDecision.buy(
            config.getSymbol(),
            entryPrice,
            stopLoss,
            takeProfit,
            calculatePositionSize(entryPrice, stopLoss),
            "Uptrend confirmed with new candle"
        );
    }
    
    private OrderDecision createSellDecision(CandleData previous, CandleData current) {
        double entryPrice = current.getOpen();
        double stopLoss = previous.getHigh() + config.getStopLossBuffer();
        double takeProfit = entryPrice - (stopLoss - entryPrice) * config.getRiskRewardRatio();
        
        return OrderDecision.sell(
            config.getSymbol(),
            entryPrice,
            stopLoss,
            takeProfit,
            calculatePositionSize(entryPrice, stopLoss),
            "Downtrend confirmed with new candle"
        );
    }
    
    private double calculatePositionSize(double entryPrice, double stopLoss) {
        double riskPerTrade = config.getAccountBalance() * config.getRiskPerTrade();
        double riskAmount = Math.abs(entryPrice - stopLoss);
        return riskPerTrade / riskAmount;
    }
}