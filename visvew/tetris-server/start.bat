@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo =================================
echo   俄罗斯方块联机中转服务器
echo =================================

echo [编译] 正在编译 Java 源文件...
javac -encoding UTF-8 WebSocketUtil.java RelayServer.java
if %errorlevel% neq 0 (
    echo [错误] 编译失败，请检查 JDK 是否安装！
    pause
    exit /b 1
)

echo [启动] 服务已启动，监听端口 3202...
java RelayServer 3202

pause