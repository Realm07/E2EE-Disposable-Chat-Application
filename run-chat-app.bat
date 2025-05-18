@echo off
cd /d "%~dp0"
cd target
start cmd /k "java -jar E2EE-Disposable-Chat-App-1.0-SNAPSHOT-jar-with-dependencies.jar"
