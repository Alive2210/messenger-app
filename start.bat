@echo off  
echo ========================================  
Messenger App - Выбор режима  
========================================  
  
1. SMALL режим - NAS / 1-2 пользователя  
2. LARGE режим - Сервер / 30-40 пользователей  
3. Выход  
  
set /p choice="Выберите режим (1-3): " 
if "%choice%"=="1" goto small  
if "%choice%"=="2" goto large  
if "%choice%"=="3" goto end  
goto menu  
  
:small  
call start-small.bat  
goto end  
  
:large  
call start-large.bat  
goto end  
  
:menu  
goto :EOF  
  
:end  
pause 
