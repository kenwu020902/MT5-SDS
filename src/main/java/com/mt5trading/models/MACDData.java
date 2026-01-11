package com.mt5trading.models;

public class MACDData {
    private double macdLine;
    private double signalLine;
    private double histogram;
    private int fastPeriod;
    private int slowPeriod;
    private int signalPeriod;
    
    public MACDData() {}
    
    public MACDData(double macdLine, double signalLine, double histogram, 
                   int fastPeriod, int slowPeriod, int signalPeriod) {
        this.macdLine = macdLine;
        this.signalLine = signalLine;
        this.histogram = histogram;
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }
    
    // Getters and Setters
    public double getMacdLine() { return macdLine; }
    public void setMacdLine(double macdLine) { this.macdLine = macdLine; }
    
    public double getSignalLine() { return signalLine; }
    public void setSignalLine(double signalLine) { this.signalLine = signalLine; }
    
    public double getHistogram() { return histogram; }
    public void setHistogram(double histogram) { this.histogram = histogram; }
    
    public int getFastPeriod() { return fastPeriod; }
    public void setFastPeriod(int fastPeriod) { this.fastPeriod = fastPeriod; }
    
    public int getSlowPeriod() { return slowPeriod; }
    public void setSlowPeriod(int slowPeriod) { this.slowPeriod = slowPeriod; }
    
    public int getSignalPeriod() { return signalPeriod; }
    public void setSignalPeriod(int signalPeriod) { this.signalPeriod = signalPeriod; }
    
    // Helper methods
    public boolean isBullish() {
        return macdLine > signalLine;
    }
    
    public boolean isBearish() {
        return macdLine < signalLine;
    }
    
    public boolean isHistogramRising() {
        return histogram > 0;
    }
    
    public boolean isHistogramFalling() {
        return histogram < 0;
    }
    
    public double getCrossoverValue() {
        return macdLine - signalLine;
    }
    
    public boolean hasBullishCrossover() {
        return getCrossoverValue() > 0;
    }
    
    public boolean hasBearishCrossover() {
        return getCrossoverValue() < 0;
    }
    
    @Override
    public String toString() {
        return String.format("MACD[Line: %.5f, Signal: %.5f, Histogram: %.5f, Bullish: %b]",
                macdLine, signalLine, histogram, isBullish());
    }
}