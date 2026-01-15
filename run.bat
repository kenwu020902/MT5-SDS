#!/bin/bash

# 创建日志目录
mkdir -p logs

echo "========================================"
echo "启动 MT5-SDS 交易系统"
echo "========================================"

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装"
    exit 1
fi

# 编译并运行
mvn clean compile exec:java

echo "程序已结束"