#!/bin/bash
set -e

# Function to log messages
echo_log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Check if OpenAI API key is provided
if [ -z "$OPENAI_API_KEY" ]; then
    echo_log "ERROR: OPENAI_API_KEY environment variable is required"
    exit 1
fi

# Set default directories if not set
REPORTS_DIR=${REPORTS_DIR:-"/app/reports"}
LOGS_DIR=${LOGS_DIR:-"/app/logs"}
INPUT_DIR=${INPUT_DIR:-"/app/input"}
JAVA_OPTS=${JAVA_OPTS:-"-Xmx512m -Xms256m"}

mkdir -p "$REPORTS_DIR" "$LOGS_DIR"

# Log startup info
echo_log "Starting Code Guard application..."
echo_log "Java Options: $JAVA_OPTS"
echo_log "Reports Directory: $REPORTS_DIR"
echo_log "Input Directory: $INPUT_DIR"

# If no arguments provided, show help
if [ $# -eq 0 ]; then
    set -- "--help"
fi

# If first argument is --scan, treat as scan mode
if [ "$1" = "--scan" ]; then
    shift
    echo_log "Scanning input directory: $INPUT_DIR"
    exec java $JAVA_OPTS -jar /app/app.jar --scan "$INPUT_DIR" --output "$REPORTS_DIR" "$@"
else
    # Pass all arguments to the jar
    exec java $JAVA_OPTS -jar /app/app.jar "$@"
fi

