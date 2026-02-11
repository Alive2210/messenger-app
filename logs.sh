#!/bin/bash

echo "📋 Logs for Secure Messenger"
echo ""

if [ "$1" == "app" ] || [ "$1" == "" ]; then
    echo "=== Application Logs ==="
    if [ -f logs/application.log ]; then
        tail -f logs/application.log
    else
        echo "No application logs found"
    fi
fi

if [ "$1" == "docker" ]; then
    echo "=== Docker Logs ==="
    docker-compose -f docker-compose.prod.yml logs -f
fi

if [ "$1" == "nginx" ]; then
    echo "=== Nginx Logs ==="
    if [ -f logs/nginx/access.log ]; then
        tail -f logs/nginx/access.log
    else
        docker-compose -f docker-compose.prod.yml logs -f nginx
    fi
fi
