@echo off
rem Jalankan Stok Gudang di Windows. Build otomatis bila jar belum ada (butuh JDK 17+ dan Maven).
cd /d "%~dp0"
if not exist "target\stok-gudang.jar" (
  echo Membangun aplikasi, tunggu sebentar...
  call mvn -q -DskipTests package
  if errorlevel 1 goto gagal
)
start "" javaw -jar "target\stok-gudang.jar"
exit /b 0
:gagal
echo.
echo Build gagal. Pastikan JDK 17 atau lebih baru dan Apache Maven sudah terpasang.
pause
