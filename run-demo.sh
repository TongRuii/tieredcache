#!/bin/bash

# TieredCache 演示应用启动脚本

echo "🚀 启动 TieredCache 演示应用..."
echo ""

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java环境，请先安装Java 8或更高版本"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到Maven环境，请先安装Maven"
    exit 1
fi

echo "✅ Java环境检查通过"
echo "✅ Maven环境检查通过"
echo ""

# 编译项目
echo "📦 编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ 编译失败，请检查代码"
    exit 1
fi

echo "✅ 编译成功"
echo ""

# 提供启动选项
echo "请选择启动方式:"
echo "1. 命令行演示 (推荐)"
echo "2. Web服务演示"
echo ""
read -p "请输入选择 (1 或 2): " choice

case $choice in
    1)
        echo ""
        echo "🎯 启动命令行演示..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        mvn exec:java -Dexec.mainClass="com.cache.plugin.example.demo.DemoApplication" -Dspring.profiles.active=demo -q
        ;;
    2)
        echo ""
        echo "🌐 启动Web服务演示..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "🔗 访问地址: http://localhost:8080/api/demo/"
        echo "📊 监控地址: http://localhost:8080/actuator/health"
        echo ""
        echo "💡 常用API:"
        echo "   GET  /api/demo/user/1001              - 获取用户信息"
        echo "   GET  /api/demo/product/PROD-001       - 获取产品信息"
        echo "   GET  /api/demo/performance             - 性能测试"
        echo "   GET  /api/demo/strategies              - 策略对比"
        echo ""
        echo "按 Ctrl+C 停止服务"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        mvn spring-boot:run -Dspring.profiles.active=demo -q
        ;;
    *)
        echo "❌ 无效选择，退出"
        exit 1
        ;;
esac