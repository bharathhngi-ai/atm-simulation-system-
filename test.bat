@echo off
title Smart ATM Simulation - Tests
echo ========================================================
echo        Running Smart ATM Simulation Unit Tests
echo ========================================================
echo.

cd /d "C:\Users\ELCOT\Downloads\atm project 2"

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "PATH=C:\Program Files\Java\jdk-26.0.1\bin;C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin;%PATH%"

call mvn test -o
pause
