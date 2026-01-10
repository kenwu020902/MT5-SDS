package main.java.com.mt5decision.mt5.connector;

import com.mt5decision.mt5.models.MT5Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class MT5WebSocketClient {
    private final String server;
    private final int port;
    private final String login;
    private final String password;
    private WebSocket webSocket;
    private HttpClient httpClient;
    private boolean authenticated;
    
    // For synchronous response handling
    private final AtomicReference<String> lastResponse = new AtomicReference<>();
    private final CountDownLatch responseLatch = new CountDownLatch(1);
    
    public MT5WebSocketClient(String server, int port, String login, String password) {
        this.server = server;
        this.port = port;
        this.login = login;
        this.password = password;
        this.authenticated = false;
    }
    
    public boolean connect() {
        try {
            httpClient = HttpClient.newHttpClient();
            
            String wsUrl = String.format("wss://%s:%d/ws", server, port);
            System.out.println("Connecting to WebSocket: " + wsUrl);
            
            CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocketListener());
            
            // Wait for connection with timeout
            webSocket = wsFuture.get(10, TimeUnit.SECONDS);
            
            // Send authentication
            String authMessage = String.format(
                "{\"action\":\"auth\",\"login\":\"%s\",\"password\":\"%s\"}",
                login, password
            );
            
            webSocket.sendText(authMessage, true);
            
            // Wait for authentication response
            if (responseLatch.await(10, TimeUnit.SECONDS)) {
                String response = lastResponse.get();
                if (response != null && response.contains("\"success\":true")) {
                    authenticated = true;
                    System.out.println("Authentication successful");
                    return true;
                } else {
                    System.err.println("Authentication failed: " + response);
                    return false;
                }
            } else {
                System.err.println("Authentication timeout");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("WebSocket connection error: " + e.getMessage());
            return false;
        }
    }
    
    public MT5Response sendRequest(String request) {
        if (!authenticated) {
            return new MT5Response(false, "Not authenticated", null);
        }
        
        try {
            // Reset latch and response
            lastResponse.set(null);
            responseLatch.countDown(); // Reset if needed
            responseLatch = new CountDownLatch(1);
            
            // Send request
            webSocket.sendText(request, true);
            
            // Wait for response with timeout
            if (responseLatch.await(10, TimeUnit.SECONDS)) {
                String response = lastResponse.get();
                return parseResponse(response);
            } else {
                return new MT5Response(false, "Request timeout", null);
            }
        } catch (Exception e) {
            return new MT5Response(false, "Error: " + e.getMessage(), null);
        }
    }
    
    private MT5Response parseResponse(String response) {
        try {
            // Simple JSON parsing (in production, use a proper JSON library)
            if (response == null) {
                return new MT5Response(false, "Empty response", null);
            }
            
            if (response.contains("\"success\":true") || response.contains("\"status\":\"ok\"")) {
                // Extract data part
                String data = extractData(response);
                return new MT5Response(true, null, data);
            } else {
                // Extract error message
                String error = extractError(response);
                return new MT5Response(false, error, null);
            }
        } catch (Exception e) {
            return new MT5Response(false, "Parse error: " + e.getMessage(), null);
        }
    }
    
    private String extractData(String json) {
        // Simplified extraction - in production, use JSON parser
        if (json.contains("\"data\":")) {
            int start = json.indexOf("\"data\":") + 7;
            int end = json.lastIndexOf("}");
            if (start < end) {
                return json.substring(start, end).trim();
            }
        }
        return json;
    }
    
    private String extractError(String json) {
        if (json.contains("\"error\":")) {
            int start = json.indexOf("\"error\":") + 8;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (start < end) {
                return json.substring(start, end).replace("\"", "").trim();
            }
        }
        return "Unknown error";
    }
    
    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing");
            authenticated = false;
        }
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    private class WebSocketListener implements Listener {
        private StringBuilder responseBuilder = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("WebSocket opened");
            Listener.super.onOpen(webSocket);
        }
        
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            responseBuilder.append(data);
            
            if (last) {
                String fullResponse = responseBuilder.toString();
                lastResponse.set(fullResponse);
                responseLatch.countDown();
                responseBuilder.setLength(0); // Reset for next message
            }
            
            return Listener.super.onText(webSocket, data, last);
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
            Listener.super.onError(webSocket, error);
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("WebSocket closed: " + reason);
            authenticated = false;
            return Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}