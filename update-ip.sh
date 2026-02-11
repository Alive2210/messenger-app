#!/bin/bash

# Автоматическое обновление IP адреса и DNS
# Запускать через cron: */5 * * * * /path/to/messenger-app/update-ip.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$SCRIPT_DIR/logs/ip-updates.log"
STATE_FILE="$SCRIPT_DIR/.last_ip"
API_URL="http://localhost:8080"

# Создаем директорию для логов
mkdir -p "$SCRIPT_DIR/logs"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Проверяем, запущен ли сервер
if ! curl -s "$API_URL/actuator/health" > /dev/null 2>&1; then
    log "❌ Server is not running"
    exit 1
fi

# Получаем текущий IP
current_ip=$(curl -s "$API_URL/api/network/info" | grep -o '"publicIp":"[^"]*"' | cut -d'"' -f4)

if [ -z "$current_ip" ]; then
    log "❌ Could not get current IP"
    exit 1
fi

# Проверяем, изменился ли IP
if [ -f "$STATE_FILE" ]; then
    last_ip=$(cat "$STATE_FILE")
    if [ "$current_ip" == "$last_ip" ]; then
        log "ℹ️  IP unchanged: $current_ip"
        exit 0
    fi
    log "🔄 IP changed from $last_ip to $current_ip"
else
    log "🆕 Initial IP: $current_ip"
fi

# Сохраняем новый IP
echo "$current_ip" > "$STATE_FILE"

# Обновляем конфигурацию на сервере
log "🔄 Refreshing server configuration..."
curl -s -X POST "$API_URL/api/network/refresh" > /dev/null 2>&1

# Обновляем DuckDNS (если настроен)
if [ -f "$SCRIPT_DIR/.duckdns-config" ]; then
    source "$SCRIPT_DIR/.duckdns-config"
    log "🔄 Updating DuckDNS: $DUCKDNS_DOMAIN"
    
    response=$(curl -s -X POST "$API_URL/api/network/dns/duckdns" \
        -d "domain=$DUCKDNS_DOMAIN" \
        -d "token=$DUCKDNS_TOKEN")
    
    if echo "$response" | grep -q '"success":true'; then
        log "✅ DuckDNS updated successfully"
    else
        log "❌ DuckDNS update failed: $response"
    fi
fi

# Обновляем No-IP (если настроен)
if [ -f "$SCRIPT_DIR/.noip-config" ]; then
    source "$SCRIPT_DIR/.noip-config"
    log "🔄 Updating No-IP: $NOIP_HOSTNAME"
    
    response=$(curl -s -X POST "$API_URL/api/network/dns/noip" \
        -d "hostname=$NOIP_HOSTNAME" \
        -d "username=$NOIP_USERNAME" \
        -d "password=$NOIP_PASSWORD")
    
    if echo "$response" | grep -q '"success":true'; then
        log "✅ No-IP updated successfully"
    else
        log "❌ No-IP update failed: $response"
    fi
fi

# Очищаем старые логи (оставляем последние 1000 строк)
if [ -f "$LOG_FILE" ]; then
    tail -n 1000 "$LOG_FILE" > "$LOG_FILE.tmp"
    mv "$LOG_FILE.tmp" "$LOG_FILE"
fi

log "✅ IP update completed: $current_ip"
