# fix_packages.ps1
Write-Host "修复Java文件包声明..." -ForegroundColor Green

$files = @(
    # config 文件
    @{Path="src\main\java\com\mt5trading\config\TradingConfig.java"; Package="com.mt5trading.config"},
    @{Path="src\main\java\com\mt5trading\config\RiskConfig.java"; Package="com.mt5trading.config"},
    
    # models 文件
    @{Path="src\main\java\com\mt5trading\models\CandleData.java"; Package="com.mt5trading.models"},
    @{Path="src\main\java\com\mt5trading\models\MACDData.java"; Package="com.mt5trading.models"},
    @{Path="src\main\java\com\mt5trading\models\OrderDecision.java"; Package="com.mt5trading.models"},
    @{Path="src\main\java\com\mt5trading\models\TrendDirection.java"; Package="com.mt5trading.models"},
    
    # mt5/connector 文件
    @{Path="src\main\java\com\mt5trading\mt5\connector\MT5Connector.java"; Package="com.mt5trading.mt5.connector"},
    @{Path="src\main\java\com\mt5trading\mt5\connector\MT5WebSocketClient.java"; Package="com.mt5trading.mt5.connector"},
    
    # mt5/models 文件
    @{Path="src\main\java\com\mt5trading\mt5\models\MT5Response.java"; Package="com.mt5trading.mt5.models"},
    
    # services 文件
    @{Path="src\main\java\com\mt5trading\services\DecisionEngine.java"; Package="com.mt5trading.services"},
    @{Path="src\main\java\com\mt5trading\services\MACDCalculator.java"; Package="com.mt5trading.services"},
    @{Path="src\main\java\com\mt5trading\services\MT5DataFetcher.java"; Package="com.mt5trading.services"},
    @{Path="src\main\java\com\mt5trading\services\TrendAnalyzer.java"; Package="com.mt5trading.services"},
    
    # Main 文件
    @{Path="src\main\java\com\mt5trading\Main.java"; Package="com.mt5trading"}
)

foreach ($fileInfo in $files) {
    $filePath = $fileInfo.Path
    $correctPackage = $fileInfo.Package
    
    if (Test-Path $filePath) {
        Write-Host "修复: $filePath -> $correctPackage" -ForegroundColor Yellow
        
        # 读取文件内容
        $content = Get-Content $filePath -Raw
        
        # 替换错误的包声明
        if ($content -match "^\s*package\s+main\.java\.com\.mt5trading\.core\.[^;]+;") {
            $content = $content -replace "^\s*package\s+main\.java\.com\.mt5trading\.core\.[^;]+;", "package $correctPackage;"
        }
        elseif ($content -match "^\s*package\s+[^;]+;") {
            $content = $content -replace "^\s*package\s+[^;]+;", "package $correctPackage;"
        }
        else {
            # 如果没有包声明，添加一个
            $content = "package $correctPackage;`n`n" + $content
        }
        
        # 写回文件
        $content | Out-File $filePath -Encoding UTF8
        Write-Host "  ✅ 已修复" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 文件不存在: $filePath" -ForegroundColor Red
    }
}

Write-Host "`n包声明修复完成！" -ForegroundColor Green