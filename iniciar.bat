@echo off
setlocal
if not defined SERVER_PORT set "SERVER_PORT=8081"
if "%~1"=="" (
    call :reutilizar
    if not errorlevel 1 exit /b 0
)
set "JAR=%~dp0target\escape-room-ia-0.0.1-SNAPSHOT.jar"
if not exist "%JAR%" (
    echo No se encuentra el JAR. Ejecuta mvnw.cmd clean package.
    exit /b 1
)
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
    echo No se encuentra Java. Instala Java 21 o configura JAVA_HOME.
    exit /b 1
)
if "%~1"=="" if not defined CRIPTA_NO_BROWSER (
    start "" /b powershell.exe -NoProfile -WindowStyle Hidden -Command "$url = 'http://127.0.0.1:' + $env:SERVER_PORT; for ($i = 0; $i -lt 60; $i++) { try { $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200 -and $r.Content.Contains('Cripta de Nocturnia')) { Start-Process $url; break } } catch {}; Start-Sleep -Seconds 1 }"
)
"%JAVA%" -jar "%JAR%" --spring.profiles.active=ollama %*
exit /b %errorlevel%

:reutilizar
powershell.exe -NoProfile -Command "$url = 'http://127.0.0.1:' + $env:SERVER_PORT; try { $r = Invoke-WebRequest -Uri ($url + '/juego') -UseBasicParsing -TimeoutSec 3 } catch { exit 1 }; if ($r.StatusCode -ne 200 -or -not $r.Content.Contains('Cripta de Nocturnia')) { exit 1 }; Write-Output ('Cripta de Nocturnia ya esta en ejecucion. ' + $url); if (-not $env:CRIPTA_NO_BROWSER) { Start-Process $url }; exit 0"
exit /b %errorlevel%
