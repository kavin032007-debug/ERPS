#!/bin/bash
# ==============================================================================
# Daily Automated Encrypted Backup Script for ERPS Database
# Usage: ./scripts/backup.sh
# Requirements: pg_dump, gpg, openssl, or 7z
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/backups/erps}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-erps_db}"
DB_USER="${DB_USER:-erps_user}"
ENCRYPTION_PASSPHRASE="${BACKUP_PASSPHRASE:-ERPS_Secure_Backup_Passphrase_2026}"

mkdir -p "${BACKUP_DIR}"

RAW_FILE="${BACKUP_DIR}/erps_backup_${TIMESTAMP}.sql"
ENC_FILE="${BACKUP_DIR}/erps_backup_${TIMESTAMP}.sql.enc"

echo "[$(date)] Starting ERPS Database Dump: ${DB_NAME} on ${DB_HOST}:${DB_PORT}..."
PGPASSWORD="${DB_PASSWORD:-erps_secret_password}" pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -F p > "${RAW_FILE}"

echo "[$(date)] Encrypting database dump with AES-256-CBC..."
openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -in "${RAW_FILE}" -out "${ENC_FILE}" -pass pass:"${ENCRYPTION_PASSPHRASE}"

# Securely remove plain SQL dump
rm -f "${RAW_FILE}"

echo "[$(date)] Backup completed successfully: ${ENC_FILE}"
echo "[$(date)] SHA-256 Checksum: $(sha256sum "${ENC_FILE}")"

# Retention: Delete backups older than 30 days locally
find "${BACKUP_DIR}" -name "erps_backup_*.sql.enc" -mtime +30 -exec rm {} \;
echo "[$(date)] Cleanup of backups older than 30 days complete."
