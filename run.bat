@echo off
title Smart ATM Simulation - Runner
echo ========================================================
echo      Starting Smart ATM Simulation & Banking Website
echo ========================================================
echo.

cd /d "C:\Users\ELCOT\Downloads\atm project 2"

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "PATH=C:\Program Files\Java\jdk-26.0.1\bin;C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin;%PATH%"

echo [1/3] Checking MySQL Database on port 3307...
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe" -u root -proot -P 3307 ping >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [INFO] Starting MySQL Database on port 3307...
    start "MySQL-3307" /B "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --datadir="C:\Users\ELCOT\mysql_data" --port=3307 --mysqlx-port=33070 --console
    timeout /t 4 /nobreak >nul
) else (
    echo [OK] MySQL Database is already running on port 3307.
)

echo.
echo [2/3] Checking if JAR is built...
if not exist "target\atm-simulation-system-1.0.0.jar" (
    echo [INFO] Building application JAR...
    call mvn clean package -DskipTests -o
)

echo.
echo [3/3] Launching ATM Simulation System...
echo URL: http://localhost:8080/
echo Press Ctrl+C in this window to stop the server.
echo.

java -jar target\atm-simulation-system-1.0.0.jar
pause
