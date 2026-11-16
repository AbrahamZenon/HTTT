@echo off
setlocal
cd /d "%~dp0" || exit /b 1
chcp 65001 >nul
echo ===============================================================
echo  HỆ THỐNG QUẢN LÝ BÁN HÀNG VÀ TỒN KHO NHÀ THUỐC LONG CHÂU
echo  UC02 - Thanh toán hóa đơn ^| UC03 - Tiếp nhận và lập phiếu nhập hàng
echo ===============================================================

set "JAVA_CMD=java"
set "JAVAC_CMD=javac"

if exist "%USERPROFILE%\.jdks\openjdk-25.0.1\bin\javac.exe" (
    set "JAVAC_CMD=%USERPROFILE%\.jdks\openjdk-25.0.1\bin\javac.exe"
    set "JAVA_CMD=%USERPROFILE%\.jdks\openjdk-25.0.1\bin\java.exe"
)

if not exist "bin" mkdir bin

echo [1/2] Đang biên dịch mã nguồn Java...
"%JAVAC_CMD%" -encoding UTF-8 -d bin -sourcepath src\main\java src\main\java\com\longchau\Main.java src\main\java\com\longchau\TestPaymentRunner.java
if errorlevel 1 (
    echo [LỖI] Biên dịch thất bại! Vui lòng kiểm tra lại JDK.
    if /i not "%~1"=="--test" pause
    exit /b 1
)

if /i "%~1"=="--test" (
    "%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp bin com.longchau.TestPaymentRunner
    exit /b
)

echo [2/2] Đang khởi chạy giao diện Java Swing...
"%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp bin com.longchau.Main

