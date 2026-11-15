@echo off
chcp 65001 >nul
echo ===============================================================
echo  CHẠY KIỂM THỬ TỰ ĐỘNG CÁC QUY TẮC NGHIỆP VỤ BR04, BR05, BR10
echo ===============================================================

set "JAVA_CMD=java"
set "JAVAC_CMD=javac"

if exist "%USERPROFILE%\.jdks\openjdk-25.0.1\bin\javac.exe" (
    set "JAVAC_CMD=%USERPROFILE%\.jdks\openjdk-25.0.1\bin\javac.exe"
    set "JAVA_CMD=%USERPROFILE%\.jdks\openjdk-25.0.1\bin\java.exe"
)

if not exist "bin" mkdir bin

dir /s /b src\main\java\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d bin @sources.txt
del sources.txt

"%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp bin com.longchau.TestPaymentRunner
pause
