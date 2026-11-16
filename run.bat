@echo off
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
dir /s /b src\main\java\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d bin @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [LỖI] Biên dịch thất bại! Vui lòng kiểm tra lại JDK.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Đang khởi chạy giao diện Java Swing...
start "" "%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp bin com.longchau.Main
echo [XONG] Giao diện đã được khởi động!

