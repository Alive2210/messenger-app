#!/bin/bash

# Настройка DuckDNS для автоматического обновления IP

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/.duckdns-config"

echo "🦆 DuckDNS Configuration"
echo "========================"
echo ""

# Проверяем, уже ли настроено
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
    echo "✅ DuckDNS already configured for domain: $DUCKDNS_DOMAIN"
    read -p "Reconfigure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
fi

# Запрашиваем данные
echo ""
echo "1. Go to https://www.duckdns.org and login"
echo "2. Create a subdomain (e.g., 'messenger-home')"
echo "3. Copy your token"
echo ""

read -p "Enter your DuckDNS subdomain (without .duckdns.org): " domain
read -p "Enter your DuckDNS token: " token

if [ -z "$domain" ] || [ -z "$token" ]; then
    echo "❌ Domain and token are required"
    exit 1
fi

# Сохраняем конфигурацию
cat > "$CONFIG_FILE" << EOF
# DuckDNS Configuration
DUCKDNS_DOMAIN=$domain
DUCKDNS_TOKEN=$token
EOF

chmod 600 "$CONFIG_FILE"

echo ""
echo "✅ Configuration saved"

# Проверяем сервер
API_URL="http://localhost:8080"
if ! curl -s "$API_URL/actuator/health" > /dev/null 2>&1; then
    echo "⚠️  Warning: Messenger server is not running"
    echo "   Start it first: docker-compose -f docker-compose.prod.yml up -d"
    exit 0
fi

# Тестируем обновление
echo ""
echo "🔄 Testing DNS update..."
response=$(curl -s -X POST "$API_URL/api/network/dns/duckdns" \
    -d "domain=$domain" \
    -d "token=$token")

if echo "$response" | grep -q '"success":true'; then
    echo "✅ DuckDNS updated successfully!"
    echo ""
    echo "Your server is now available at:"
    echo "  • turn:$domain.duckdns.org:3478"
    echo "  • turns:$domain.duckdns.org:5349"
    echo "  • http://$domain.duckdns.org:8080"
    echo ""
    echo "Next steps:"
    echo "1. Add to crontab for automatic updates:"
    echo "   */5 * * * * $SCRIPT_DIR/update-ip.sh"
    echo ""
    echo "2. Open ports on your router:"
    echo "   • 3478/UDP (TURN)"
    echo "   • 5349/TCP (TURNS)"
    echo "   • 10000-20000/UDP (TURN relay)"
    echo ""
else
    echo "❌ DNS update failed"
    echo "Response: $response"
    exit 1
fi
