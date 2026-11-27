@echo off
setlocal
set "APP_JAR=escape-room-ia-0.0.1-SNAPSHOT.jar"
set "JAR=%~dp0%APP_JAR%"
if not exist "%JAR%" set "JAR=%~dp0target\%APP_JAR%"
if not exist "%JAR%" (
    echo No se encuentra %APP_JAR%. Ejecuta mvn clean package desde el proyecto.
    exit /b 1
)

set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
    echo Instala Java 21 o configura JAVA_HOME.
    exit /b 1
)

set "CRIPTA_URL=http://localhost:8080"
start "" /b powershell.exe -NoProfile -WindowStyle Hidden -Command "$url = $env:CRIPTA_URL; for ($i = 0; $i -lt 60; $i++) { try { $r = Invoke-WebRequest -Uri ($url + '/juego') -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200 -and $r.Content.Contains('Cripta de Nocturnia')) { Start-Process $url; break } } catch {}; Start-Sleep -Seconds 1 }"
"%JAVA%" -jar "%JAR%" --spring.profiles.active=ollama --server.port=8080 --server.address=localhost
exit /b %errorlevel%
