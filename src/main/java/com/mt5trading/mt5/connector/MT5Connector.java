package com.mt5trading.mt5.connector;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.models.MT5Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class MT5Connector {
    private static final Logger logger = LoggerFactory.getLogger(MT5Connector.class);
    
    private final TradingConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    public MT5Connector(TradingConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = config.getMt5ApiUrl();
    }
    
    public CompletableFuture<MT5Response> getHistoricalData(String symbol, String timeframe, int bars) {
        String url = String.format("%s/historical?symbol=%s&timeframe=%s&bars=%d",
                baseUrl, symbol, timeframe, bars);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getMt5Password())
                .header("X-MT5-Login", config.getMt5Login())
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return objectMapper.readValue(response.body(), MT5Response.class);
                        } else {
                            logger.error("Failed to get historical data: HTTP {}", response.statusCode());
                            return createErrorResponse("HTTP " + response.statusCode());
                        }
                    } catch (Exception e) {
                        logger.error("Error parsing historical data response", e);
                        return createErrorResponse(e.getMessage());
                    }
                });
    }
    
    public CompletableFuture<MT5Response> getCurrentPrice(String symbol) {
        String url = String.format("%s/price?symbol=%s", baseUrl, symbol);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getMt5Password())
                .header("X-MT5-Login", config.getMt5Login())
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return objectMapper.readValue(response.body(), MT5Response.class);
                        } else {
                            logger.error("Failed to get current price: HTTP {}", response.statusCode());
                            return createErrorResponse("HTTP " + response.statusCode());
                        }
                    } catch (Exception e) {
                        logger.error("Error parsing current price response", e);
                        return createErrorResponse(e.getMessage());
                    }
                });
    }
    
    public CompletableFuture<Boolean> sendOrder(String symbol, String orderType, 
                                              double volume, double price, 
                                              double stopLoss, double takeProfit) {
        String url = String.format("%s/order", baseUrl);
        
        String orderJson = String.format(
                "{\"symbol\": \"%s\", \"type\": \"%s\", \"volume\": %.2f, \"price\": %.5f, " +
                "\"sl\": %.5f, \"tp\": %.5f, \"magic\": %d, \"comment\": \"MT5-SDS Auto Trade\"}",
                symbol, orderType, volume, price, stopLoss, takeProfit, config.getMagicNumber());
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getMt5Password())
                .header("X-MT5-Login", config.getMt5Login())
                .POST(HttpRequest.BodyPublishers.ofString(orderJson))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            MT5Response mt5Response = objectMapper.readValue(response.body(), MT5Response.class);
                            return mt5Response.isSuccess();
                        } else {
                            logger.error("Failed to send order: HTTP {}", response.statusCode());
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("Error sending order", e);
                        return false;
                    }
                });
    }
    
    public CompletableFuture<Double> getAccountBalance() {
        String url = String.format("%s/account/balance", baseUrl);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getMt5Password())
                .header("X-MT5-Login", config.getMt5Login())
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return objectMapper.readTree(response.body()).get("balance").asDouble();
                        } else {
                            logger.error("Failed to get account balance: HTTP {}", response.statusCode());
                            return 0.0;
                        }
                    } catch (Exception e) {
                        logger.error("Error getting account balance", e);
                        return 0.0;
                    }
                });
    }
    
    private MT5Response createErrorResponse(String error) {
        MT5Response response = new MT5Response();
        response.setStatus("ERROR");
        response.setError(error);
        return response;
    }
    
    public void disconnect() {
        logger.info("MT5Connector disconnected");
    }
    
    public boolean isConnected() {
        // Implement connection check logic
        return true;
    }

    public TradingConfig getConfig() {
        return config;
    }
}