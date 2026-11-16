@echo off
setlocal
call "%~dp0run.bat" --test
set "TEST_EXIT=%ERRORLEVEL%"
pause
exit /b %TEST_EXIT%
