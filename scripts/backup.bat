@echo off
REM ==============================================================================
REM Windows Automated Encrypted Backup Script for ERPS Database
REM Usage: scripts\backup.bat
REM ==============================================================================

setlocal enabledelayedexpansion

set BACKUP_DIR=backups
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set TIMESTAMP=%mydate%_%mytime: =0%

set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=erps_db
set DB_USER=erps_user
set PGPASSWORD=erps_secret_password
set RAW_FILE=%BACKUP_DIR%\erps_backup_%TIMESTAMP%.sql
set ENC_FILE=%BACKUP_DIR%\erps_backup_%TIMESTAMP%.sql.enc
set ENCRYPTION_PASSPHRASE=ERPS_Secure_Backup_Passphrase_2026

echo Starting ERPS Database Dump: %DB_NAME%...
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -F p > "%RAW_FILE%"

echo Encrypting database dump with AES-256...
openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -in "%RAW_FILE%" -out "%ENC_FILE%" -pass pass:"%ENCRYPTION_PASSPHRASE%"

if exist "%ENC_FILE%" (
    del "%RAW_FILE%"
    echo Backup completed successfully: %ENC_FILE%
) else (
    echo Encryption failed or openssl not found in PATH. Raw file preserved at %RAW_FILE%
)

endlocal
