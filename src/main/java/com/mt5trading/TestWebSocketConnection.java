package com.mt5trading;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.mt5.connector.MT5Connector;
import com.mt5trading.models.CandleData;
import com.mt5trading.services.DecisionEngine;
import com.mt5trading.services.MT5DataFetcher;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestWebSocketConnection {
    public static void main(String[] args) {
        System.out.println("=== MT5 WebSocket连接测试 ===\n");
        
        try {
            // 加载配置
            TradingConfig config = TradingConfig.load();
            System.out.println("配置加载成功:");
            System.out.println("  WebSocket URL: " + config.getMt5WebSocketUrl());
            System.out.println("  账户: " + config.getMt5Login());
            System.out.println("  服务器: " + config.getMt5Server());
            
            // 创建连接器
            MT5Connector connector = new MT5Connector(config);
            
            // 创建决策引擎（简化）
            DecisionEngine dummyEngine = new DecisionEngine(config, null) {
                @Override
                public void analyzeNewCandle(CandleData candle) {
                    System.out.println("收到新K线: " + candle);
                }
            };
            
            // 初始化WebSocket连接
            System.out.println("\n1. 初始化WebSocket连接...");
            boolean connected = connector.initializeWebSocket(candle -> {
                System.out.println("K线数据: " + candle);
            }, dummyEngine);
            
            if (!connected) {
                System.out.println("❌ WebSocket连接失败");
                return;
            }
            
            System.out.println("✅ WebSocket连接成功");
            
            // 等待认证
            System.out.println("\n2. 等待MT5服务器认证...");
            TimeUnit.SECONDS.sleep(3);
            
            // 测试订单发送
            System.out.println("\n3. 测试交易指令发送...");
            System.out.print("发送测试订单？(y/n): ");
            // 这里可以添加用户输入
            
            if (false) {  // 暂时禁用
                connector.sendOrder(
                    config.getSymbol(),
                    "BUY",
                    0.1,
                    1.10000,
                    1.09900,
                    1.10200
                ).thenAccept(success -> {
                    if (success) {
                        System.out.println("✅ 订单发送成功");
                    } else {
                        System.out.println("❌ 订单发送失败");
                    }
                });
                
                TimeUnit.SECONDS.sleep(2);
            }
            
            // 保持运行
            System.out.println("\n4. 系统运行中，按Ctrl+C停止...");
            System.out.println("正在接收实时市场数据...");
            
            // 运行10秒后停止
            TimeUnit.SECONDS.sleep(10);
            
            // 断开连接
            System.out.println("\n5. 断开连接...");
            connector.disconnect();
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (IOException e) {
            System.err.println("配置加载失败: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("测试被中断");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}