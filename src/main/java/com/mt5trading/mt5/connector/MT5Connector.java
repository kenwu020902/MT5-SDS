package main.java.com.mt5trading.mt5.connector;

import com.mt5trading.models.CandleData;
import com.mt5trading.mt5.models.MT5Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class MT5Connector {
    private String server;
    private int port;
    private String login;
    private String password;
    private boolean connected;
    private MT5WebSocketClient webSocketClient;
    
    public MT5Connector() {
        loadConfig();
        this.connected = false;
    }
    
    public MT5Connector(String server, int port, String login, String password) {
        this.server = server;
        this.port = port;
        this.login = login;
        this.password = password;
        this.connected = false;
    }
    
    private void loadConfig() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("config.properties"));
            this.server = prop.getProperty("mt5.server", "demo.mt5.com");
            this.port = Integer.parseInt(prop.getProperty("mt5.port", "443"));
            this.login = prop.getProperty("mt5.login", "");
            this.password = prop.getProperty("mt5.password", "");
        } catch (IOException e) {
            System.err.println("Failed to load config.properties, using defaults");
            this.server = "demo.mt5.com";
            this.port = 443;
            this.login = "";
            this.password = "";
        }
    }
    
    public boolean connect() {
        try {
            System.out.println("Connecting to MT5 server: " + server + ":" + port);
            
            // Initialize WebSocket client
            webSocketClient = new MT5WebSocketClient(server, port, login, password);
            connected = webSocketClient.connect();
            
            if (connected) {
                System.out.println("Successfully connected to MT5");
            } else {
                System.err.println("Failed to connect to MT5");
            }
            
            return connected;
        } catch (Exception e) {
            System.err.println("Error connecting to MT5: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        connected = false;
        System.out.println("Disconnected from MT5");
    }
    
    public List<CandleData> getCandles(String symbol, String timeframe, int count) {
        if (!connected) {
            System.err.println("Not connected to MT5. Please connect first.");
            return new ArrayList<>();
        }
        
        try {
            // Construct request for candles
            String request = String.format(
                "{\"action\":\"getCandles\",\"symbol\":\"%s\",\"timeframe\":\"%s\",\"count\":%d}",
                symbol, timeframe, count
            );
            
            // Send request via WebSocket
            MT5Response response = webSocketClient.sendRequest(request);
            
            if (response.isSuccess()) {
                return parseCandles(response.getData());
            } else {
                System.err.println("Failed to get candles: " + response.getError());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error getting candles: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<CandleData> parseCandles(Object data) {
        List<CandleData> candles = new ArrayList<>();
        
        try {
            if (data instanceof List) {
                List<?> candleList = (List<?>) data;
                for (Object obj : candleList) {
                    if (obj instanceof String) {
                        // Parse CSV format: time,open,high,low,close,volume
                        String[] parts = ((String) obj).split(",");
                        if (parts.length >= 6) {
                            LocalDateTime time = LocalDateTime.parse(parts[0]);
                            double open = Double.parseDouble(parts[1]);
                            double high = Double.parseDouble(parts[2]);
                            double low = Double.parseDouble(parts[3]);
                            double close = Double.parseDouble(parts[4]);
                            long volume = Long.parseLong(parts[5]);
                            
                            candles.add(new CandleData(open, high, low, close, volume, time));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing candle data: " + e.getMessage());
        }
        
        return candles;
    }
    
    public boolean sendOrder(String symbol, String orderType, double volume, 
                           double price, double stopLoss, double takeProfit, 
                           String comment) {
        if (!connected) {
            System.err.println("Not connected to MT5. Please connect first.");
            return false;
        }
        
        try {
            String request = String.format(
                "{\"action\":\"sendOrder\",\"symbol\":\"%s\",\"type\":\"%s\"," +
                "\"volume\":%.2f,\"price\":%.2f,\"sl\":%.2f,\"tp\":%.2f,\"comment\":\"%s\"}",
                symbol, orderType, volume, price, stopLoss, takeProfit, comment
            );
            
            MT5Response response = webSocketClient.sendRequest(request);
            
            if (response.isSuccess()) {
                System.out.println("Order sent successfully: " + response.getData());
                return true;
            } else {
                System.err.println("Failed to send order: " + response.getError());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error sending order: " + e.getMessage());
            return false;
        }
    }
    
    public MT5Response getAccountInfo() {
        if (!connected) {
            return new MT5Response(false, "Not connected", null);
        }
        
        try {
            String request = "{\"action\":\"getAccountInfo\"}";
            return webSocketClient.sendRequest(request);
        } catch (Exception e) {
            return new MT5Response(false, "Error: " + e.getMessage(), null);
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getServer() { return server; }
    public int getPort() { return port; }
    public String getLogin() { return login; }
    
    @Override
    public String toString() {
        return String.format("MT5Connector{server='%s', port=%d, connected=%s}", 
                           server, port, connected);
    }
}
