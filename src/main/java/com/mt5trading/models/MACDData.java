package main.java.com.mt5trading.core.models;

public class MACDData {
    private double macdLine;
    private double signalLine;
    private double histogram;
    private double previousMacd;
    private double previousSignal;
    private double previousHistogram;
    
    // Original constructor
    public MACDData(double macdLine, double signalLine, double histogram) {
        this.macdLine = macdLine;
        this.signalLine = signalLine;
        this.histogram = histogram;
    }
    
    // Constructor for tests
    public MACDData(double macdLine, double signalLine, double histogram,
                   double previousMacd, double previousSignal, double previousHistogram) {
        this.macdLine = macdLine;
        this.signalLine = signalLine;
        this.histogram = histogram;
        this.previousMacd = previousMacd;
        this.previousSignal = previousSignal;
        this.previousHistogram = previousHistogram;
    }
    
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
    
    // Getters
    public double getMacdLine() { return macdLine; }
    public double getSignalLine() { return signalLine; }
    public double getHistogram() { return histogram; }
    public double getPreviousMacd() { return previousMacd; }
    public double getPreviousSignal() { return previousSignal; }
    public double getPreviousHistogram() { return previousHistogram; }
    
    // Additional methods
    public boolean isCrossingUp() {
        return macdLine > signalLine && previousMacd <= previousSignal;
    }
    
    public boolean isCrossingDown() {
        return macdLine < signalLine && previousMacd >= previousSignal;
    }
    
    public boolean isHistogramIncreasing() {
        return histogram > previousHistogram;
    }
    
    public boolean isHistogramDecreasing() {
        return histogram < previousHistogram;
    }
    
    @Override
    public String toString() {
        return String.format("MACDData{macd=%.4f, signal=%.4f, hist=%.4f, bullish=%s}",
                           macdLine, signalLine, histogram, isBullish());
    }
}
