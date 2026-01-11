package main.java.com.mt5trading.mt5.models;

import java.util.Map;
import java.util.HashMap;

public class MT5Response {
    private boolean success;
    private String error;
    private Object data;
    private Map<String, Object> metadata;
    private long timestamp;
    
    public MT5Response() {
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }
    
    public MT5Response(boolean success, String error, Object data) {
        this.success = success;
        this.error = error;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }
    
    public MT5Response(boolean success, String error, Object data, Map<String, Object> metadata) {
        this.success = success;
        this.error = error;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public Object getData() { return data; }
    public Map<String, Object> getMetadata() { return metadata; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setError(String error) { this.error = error; }
    public void setData(Object data) { this.data = data; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Helper methods
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    public String getStringData() {
        return data != null ? data.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMapData() {
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return null;
    }
    
    public Double getDoubleData() {
        if (data instanceof Number) {
            return ((Number) data).doubleValue();
        } else if (data instanceof String) {
            try {
                return Double.parseDouble((String) data);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Integer getIntegerData() {
        if (data instanceof Number) {
            return ((Number) data).intValue();
        } else if (data instanceof String) {
            try {
                return Integer.parseInt((String) data);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public boolean hasData() {
        return data != null;
    }
    
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(success);
        sb.append(",\"timestamp\":").append(timestamp);
        
        if (error != null) {
            sb.append(",\"error\":\"").append(error).append("\"");
        }
        
        if (data != null) {
            sb.append(",\"data\":");
            if (data instanceof String) {
                sb.append("\"").append(data).append("\"");
            } else {
                sb.append(data.toString());
            }
        }
        
        if (!metadata.isEmpty()) {
            sb.append(",\"metadata\":").append(metadata.toString());
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return toJsonString();
    }
    
    // Factory methods for common responses
    public static MT5Response success(Object data) {
        return new MT5Response(true, null, data);
    }
    
    public static MT5Response success(String message) {
        return new MT5Response(true, null, message);
    }
    
    public static MT5Response success(Object data, Map<String, Object> metadata) {
        return new MT5Response(true, null, data, metadata);
    }
    
    public static MT5Response error(String error) {
        return new MT5Response(false, error, null);
    }
    
    public static MT5Response error(String error, Object data) {
        return new MT5Response(false, error, data);
    }
    
    public static MT5Response connectionError() {
        return new MT5Response(false, "Connection failed", null);
    }
    
    public static MT5Response timeoutError() {
        return new MT5Response(false, "Request timeout", null);
    }
    
    public static MT5Response authenticationError() {
        return new MT5Response(false, "Authentication failed", null);
    }
    
    public static MT5Response invalidRequestError() {
        return new MT5Response(false, "Invalid request", null);
    }
}
