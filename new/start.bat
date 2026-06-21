@echo off
chcp 65001 >nul
cd /d "%~dp0new"

echo =================================
echo   俄罗斯方块 - 客户端
echo =================================

echo [编译] 正在编译 Java 源文件...
javac -encoding UTF-8 -d bin -cp "lib\sqlite-jdbc-3.36.0.3.jar" src\com\he\main\*.java src\com\he\config\*.java src\com\he\model\*.java src\com\he\view\*.java src\com\he\controller\*.java src\com\he\network\*.java src\com\he\service\*.java src\com\he\ai\*.java
if errorlevel 1 goto error

echo [启动] 游戏启动中...
echo.
java -cp "bin;lib\sqlite-jdbc-3.36.0.3.jar" com.he.main.Main
pause
exit /b 0

:error
echo [错误] 编译失败，请检查 JDK 是否安装！
pause
exit /b 1