@echo off
cd /d "%~dp0"
setlocal
rem === Añadir las rutas necesarias temporalmente ===
set PATH=%CD%\native;%CD%\runtime\bin;%PATH%

rem === Ejecutar la aplicación ===
runtime\bin\java.exe ^
--add-opens=javafx.graphics/com.sun.javafx.font.directwrite=ALL-UNNAMED ^
-Dprism.text=t2k ^
-Djava.library.path=.\native ^
-cp "app\fingerprint-desktop-1.0-SNAPSHOT-jar-with-dependencies.jar;lib\FDxSDKPro.jar" ^
com.softclass.fingerprint.MainApp

endlocal
pause

