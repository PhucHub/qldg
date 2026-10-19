@echo off
chcp 65001 >nul
:: Đảm bảo Maven có trong PATH
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    set "PATH=C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin;%PATH%"
)

echo [1/3] Khởi động MariaDB...
start "" "D:\Xampp\mysql\bin\mysqld.exe" --datadir="D:\Xuongptpm\qldg\mysql-data" --port=3306 --console

echo [2/3] Chạy Spring Boot Backend (Port 8080)...
cd /d D:\Xuongptpm\qldg\backend
start "QLDG Backend" cmd /k "mvn spring-boot:run"

echo [3/3] Chạy Vue.js Frontend (Port 5173)...
cd /d D:\Xuongptpm\qldg\frontend
start "QLDG Frontend" cmd /k "npm run dev"

echo.
echo ===================================================
echo Hệ thống QLDG đã khởi động:
echo - Frontend: http://localhost:5173
echo - Backend:  http://localhost:8080/api
echo - Tài khoản test: Chọn trên thanh Dev Banner đầu trang
echo ===================================================
