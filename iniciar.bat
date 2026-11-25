@echo off
setlocal
set "JAR=%~dp0target\escape-room-ia-0.0.1-SNAPSHOT.jar"
if not exist "%JAR%" (
    echo No se encuentra el JAR. Ejecuta mvnw.cmd clean package.
    exit /b 1
)
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar "%JAR%" %*
) else (
    java -jar "%JAR%" %*
)
exit /b %errorlevel%
