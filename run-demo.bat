@echo off
chcp 65001 >nul

echo 🚀 启动 TieredCache 演示应用...
echo.

REM 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Java环境，请先安装Java 8或更高版本
    pause
    exit /b 1
)

REM 检查Maven环境
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Maven环境，请先安装Maven
    pause
    exit /b 1
)

echo ✅ Java环境检查通过
echo ✅ Maven环境检查通过
echo.

REM 编译项目
echo 📦 编译项目...
mvn clean compile -q
if errorlevel 1 (
    echo ❌ 编译失败，请检查代码
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.

REM 提供启动选项
echo 请选择启动方式:
echo 1. 命令行演示 (推荐)
echo 2. Web服务演示
echo.
set /p choice=请输入选择 (1 或 2): 

if "%choice%"=="1" (
    echo.
    echo 🎯 启动命令行演示...
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    mvn exec:java -Dexec.mainClass="com.cache.plugin.example.demo.DemoApplication" -Dspring.profiles.active=demo -q
) else if "%choice%"=="2" (
    echo.
    echo 🌐 启动Web服务演示...
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo 🔗 访问地址: http://localhost:8080/api/demo/
    echo 📊 监控地址: http://localhost:8080/actuator/health
    echo.
    echo 💡 常用API:
    echo    GET  /api/demo/user/1001              - 获取用户信息
    echo    GET  /api/demo/product/PROD-001       - 获取产品信息
    echo    GET  /api/demo/performance             - 性能测试
    echo    GET  /api/demo/strategies              - 策略对比
    echo.
    echo 按 Ctrl+C 停止服务
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    mvn spring-boot:run -Dspring.profiles.active=demo -q
) else (
    echo ❌ 无效选择，退出
    pause
    exit /b 1
)

pause