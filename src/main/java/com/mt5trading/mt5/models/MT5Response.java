package com.mt5trading.mt5.models;

public class MT5Response {
    private String command;
    private String symbol;
    private String timeframe;
    private MT5CandleData[] candles;
    private String status;
    private String error;
    private long timestamp;
    
    public static class MT5CandleData {
        private String time;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;
        
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public double getOpen() { return open; }
        public void setOpen(double open) { this.open = open; }
        
        public double getHigh() { return high; }
        public void setHigh(double high) { this.high = high; }
        
        public double getLow() { return low; }
        public void setLow(double low) { this.low = low; }
        
        public double getClose() { return close; }
        public void setClose(double close) { this.close = close; }
        
        public long getVolume() { return volume; }
        public void setVolume(long volume) { this.volume = volume; }
    }
    
    // Getters and Setters for MT5Response
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    
    public MT5CandleData[] getCandles() { return candles; }
    public void setCandles(MT5CandleData[] candles) { this.candles = candles; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isSuccess() {
        return "OK".equalsIgnoreCase(status);
    }
    
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
}