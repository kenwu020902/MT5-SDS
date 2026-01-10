package main.java.com.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
    
    private static Logger instance;
    private LogLevel currentLevel;
    private String logFilePath;
    private boolean writeToFile;
    private boolean writeToConsole;
    
    // Thread-safe singleton with double-checked locking
    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }
    
    private Logger() {
        // Default configuration
        this.currentLevel = LogLevel.INFO;
        this.logFilePath = "trading_system.log";
        this.writeToFile = true;
        this.writeToConsole = true;
        
        // Initialize log file
        initializeLogFile();
    }
    
    private void initializeLogFile() {
        if (writeToFile) {
            try {
                FileWriter fileWriter = new FileWriter(logFilePath, true);
                BufferedWriter writer = new BufferedWriter(fileWriter);
                writer.write("\n" + getTimestamp() + " [INFO] Logging initialized");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.err.println("Failed to initialize log file: " + e.getMessage());
                writeToFile = false;
            }
        }
    }
    
    // Configuration methods
    public void setLogLevel(LogLevel level) {
        this.currentLevel = level;
    }
    
    public void setLogFilePath(String filePath) {
        this.logFilePath = filePath;
    }
    
    public void setWriteToFile(boolean writeToFile) {
        this.writeToFile = writeToFile;
    }
    
    public void setWriteToConsole(boolean writeToConsole) {
        this.writeToConsole = writeToConsole;
    }
    
    // Logging methods
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    public void fatal(String message) {
        log(LogLevel.FATAL, message, null);
    }
    
    public void fatal(String message, Throwable throwable) {
        log(LogLevel.FATAL, message, throwable);
    }
    
    // Trading-specific logging methods
    public void logTrade(String symbol, String action, double price, double volume, String reason) {
        String message = String.format("TRADE: %s %s %.2f @ %.2f - %s", 
                                     symbol, action, volume, price, reason);
        info(message);
    }
    
    public void logDecision(String decision, String symbol, double confidence) {
        String message = String.format("DECISION: %s for %s (confidence: %.2f)", 
                                     decision, symbol, confidence);
        info(message);
    }
    
    public void logSignal(String signalType, String symbol, String timeframe, String details) {
        String message = String.format("SIGNAL: %s detected on %s %s - %s", 
                                     signalType, symbol, timeframe, details);
        debug(message);
    }
    
    // Private logging implementation
    private void log(LogLevel level, String message, Throwable throwable) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return; // Skip if below current log level
        }
        
        String logEntry = formatLogEntry(level, message);
        
        // Write to console
        if (writeToConsole) {
            writeToConsole(level, logEntry, throwable);
        }
        
        // Write to file
        if (writeToFile) {
            writeToFile(logEntry, throwable);
        }
    }
    
    private String formatLogEntry(LogLevel level, String message) {
        return String.format("%s [%s] %s", 
                           getTimestamp(), 
                           level.name(), 
                           message);
    }
    
    private void writeToConsole(LogLevel level, String logEntry, Throwable throwable) {
        switch (level) {
            case ERROR:
            case FATAL:
                System.err.println(logEntry);
                if (throwable != null) {
                    throwable.printStackTrace(System.err);
                }
                break;
            default:
                System.out.println(logEntry);
                if (throwable != null) {
                    throwable.printStackTrace(System.out);
                }
        }
    }
    
    private void writeToFile(String logEntry, Throwable throwable) {
        try (FileWriter fileWriter = new FileWriter(logFilePath, true);
             BufferedWriter writer = new BufferedWriter(fileWriter)) {
            
            writer.write(logEntry);
            writer.newLine();
            
            if (throwable != null) {
                writer.write("Exception: " + throwable.getMessage());
                writer.newLine();
                for (StackTraceElement element : throwable.getStackTrace()) {
                    writer.write("\tat " + element.toString());
                    writer.newLine();
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }
    
    // Utility methods
    public void logSeparator() {
        String separator = "========================================";
        if (writeToConsole) {
            System.out.println(separator);
        }
        if (writeToFile) {
            try (FileWriter fileWriter = new FileWriter(logFilePath, true);
                 BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write(separator);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Failed to write separator to log file: " + e.getMessage());
            }
        }
    }
    
    public void logSection(String sectionTitle) {
        String section = String.format("========== %s ==========", sectionTitle);
        if (writeToConsole) {
            System.out.println(section);
        }
        if (writeToFile) {
            try (FileWriter fileWriter = new FileWriter(logFilePath, true);
                 BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write(section);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Failed to write section to log file: " + e.getMessage());
            }
        }
    }
    
    // Clear log file
    public void clearLogFile() {
        if (writeToFile) {
            try (FileWriter fileWriter = new FileWriter(logFilePath, false)) {
                // Opening with false truncates the file
                fileWriter.write("");
            } catch (IOException e) {
                System.err.println("Failed to clear log file: " + e.getMessage());
            }
        }
    }
    
    // Get log file path
    public String getLogFilePath() {
        return logFilePath;
    }
    
    @Override
    public String toString() {
        return String.format("Logger{level=%s, file='%s', toFile=%s, toConsole=%s}", 
                           currentLevel, logFilePath, writeToFile, writeToConsole);
    }
}