#!/bin/bash

# Скрипт для просмотра и анализа логов Messenger

LOG_DIR="${LOG_DIR:-logs}"

show_help() {
    cat << EOF
Messenger Log Viewer

Usage: $0 [OPTION] [PATTERN]

Options:
    -a, --app           Показать application логи (по умолчанию)
    -e, --error         Показать только ошибки
    -u, --audit         Показать аудит логи
    -s, --security      Показать security логи
    -w, --websocket     Показать websocket логи
    -p, --performance   Показать performance логи
    -j, --json          Форматировать JSON логи (требуется jq)
    -f, --follow        Следить за новыми записями (tail -f)
    -n, --lines N       Показать последние N строк (по умолчанию: 50)
    -g, --grep PATTERN  Искать по шаблону
    -t, --trace ID      Искать по traceId
    -d, --date DATE     Фильтр по дате (YYYY-MM-DD)
    -c, --count         Посчитать количество записей
    --clear             Очистить все логи
    -h, --help          Показать эту справку

Examples:
    $0                          # Последние 50 строк application.log
    $0 -f                       # Следить за логами в реальном времени
    $0 -e                       # Показать последние ошибки
    $0 -u -j                    # Аудит логи в JSON формате
    $0 -g "ERROR"               # Искать ошибки в логах
    $0 -t "abc-123"             # Найти все записи с traceId
    $0 -u -d "2024-01-15"       # Аудит за конкретную дату
    $0 -p -c                    # Посчитать записи performance

EOF
}

# Параметры по умолчанию
LOG_FILE="application"
FORMAT="text"
FOLLOW=false
LINES=50
GREP_PATTERN=""
TRACE_ID=""
DATE_FILTER=""
COUNT_ONLY=false

# Разбор аргументов
while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--app)
            LOG_FILE="application"
            shift
            ;;
        -e|--error)
            LOG_FILE="error"
            shift
            ;;
        -u|--audit)
            LOG_FILE="audit"
            shift
            ;;
        -s|--security)
            LOG_FILE="security"
            shift
            ;;
        -w|--websocket)
            LOG_FILE="websocket"
            shift
            ;;
        -p|--performance)
            LOG_FILE="performance"
            shift
            ;;
        -j|--json)
            FORMAT="json"
            shift
            ;;
        -f|--follow)
            FOLLOW=true
            shift
            ;;
        -n|--lines)
            LINES="$2"
            shift 2
            ;;
        -g|--grep)
            GREP_PATTERN="$2"
            shift 2
            ;;
        -t|--trace)
            TRACE_ID="$2"
            shift 2
            ;;
        -d|--date)
            DATE_FILTER="$2"
            shift 2
            ;;
        -c|--count)
            COUNT_ONLY=true
            shift
            ;;
        --clear)
            read -p "⚠️  Очистить все логи? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                rm -f ${LOG_DIR}/*.log ${LOG_DIR}/*.json 2>/dev/null
                echo "✅ Логи очищены"
            else
                echo "Отменено"
            fi
            exit 0
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            if [ -z "$GREP_PATTERN" ]; then
                GREP_PATTERN="$1"
            fi
            shift
            ;;
    esac
done

# Проверка наличия директории логов
if [ ! -d "$LOG_DIR" ]; then
    echo "❌ Директория логов не найдена: $LOG_DIR"
    exit 1
fi

# Определение файла
if [ -n "$DATE_FILTER" ]; then
    # Ищем архивированные логи за конкретную дату
    FILE_PATTERN="${LOG_DIR}/${LOG_FILE}.${DATE_FILTER}.*"
    FILES=$(ls -1 ${FILE_PATTERN} 2>/dev/null | head -1)
    if [ -z "$FILES" ]; then
        echo "❌ Логи за дату $DATE_FILTER не найдены"
        exit 1
    fi
else
    # Текущий лог
    if [ "$FORMAT" == "json" ] && [ "$LOG_FILE" == "application" ]; then
        FILES="${LOG_DIR}/${LOG_FILE}.json"
    else
        FILES="${LOG_DIR}/${LOG_FILE}.log"
    fi
fi

# Проверка существования файла
if [ ! -f "$FILES" ]; then
    echo "❌ Файл логов не найден: $FILES"
    exit 1
fi

# Построение команды
CMD=""

# Добавляем фильтрацию по traceId
if [ -n "$TRACE_ID" ]; then
    if [ "$FORMAT" == "json" ]; then
        if command -v jq &> /dev/null; then
            CMD="grep '$TRACE_ID' $FILES | jq -r '[.timestamp, .level, .logger, .message] | @tsv' 2>/dev/null"
        else
            CMD="grep '$TRACE_ID' $FILES"
        fi
    else
        CMD="grep '$TRACE_ID' $FILES"
    fi
# Добавляем grep если нужен
elif [ -n "$GREP_PATTERN" ]; then
    CMD="grep '$GREP_PATTERN' $FILES"
else
    CMD="cat $FILES"
fi

# Добавляем tail если нужно
if [ "$FOLLOW" == true ]; then
    CMD="tail -f $FILES"
    if [ -n "$GREP_PATTERN" ] || [ -n "$TRACE_ID" ]; then
        CMD="$CMD | grep --line-buffered '${GREP_PATTERN:-$TRACE_ID}'"
    fi
elif [ "$COUNT_ONLY" == true ]; then
    if [ -n "$GREP_PATTERN" ] || [ -n "$TRACE_ID" ]; then
        CMD="$CMD | wc -l"
    else
        CMD="wc -l $FILES"
    fi
else
    if [ -z "$GREP_PATTERN" ] && [ -z "$TRACE_ID" ]; then
        CMD="tail -n $LINES $FILES"
    else
        CMD="$CMD | tail -n $LINES"
    fi
fi

# Форматирование JSON
if [ "$FORMAT" == "json" ] && [ "$COUNT_ONLY" == false ]; then
    if command -v jq &> /dev/null; then
        if [ -z "$CMD" ] || [[ ! "$CMD" == *"jq"* ]]; then
            CMD="$CMD | jq -r '[.timestamp, .level, .logger, .message] | @tsv' 2>/dev/null || $CMD"
        fi
    else
        echo "⚠️  Для форматирования JSON установите jq: sudo apt-get install jq"
    fi
fi

# Цветной вывод для консоли
if [ "$COUNT_ONLY" == false ] && [ "$FORMAT" == "text" ]; then
    CMD="$CMD | awk '
    /ERROR/ {print \"\033[0;31m\" \$0 \"\033[0m\"; next}
    /WARN/  {print \"\033[0;33m\" \$0 \"\033[0m\"; next}
    /INFO/  {print \"\033[0;32m\" \$0 \"\033[0m\"; next}
    /DEBUG/ {print \"\033[0;34m\" \$0 \"\033[0m\"; next}
    {print}
    '" 2>/dev/null
fi

# Выполнение команды
eval $CMD
