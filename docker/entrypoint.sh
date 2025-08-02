#!/bin/bash
set -e

# Function to log messages
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Check if OpenAI API key is provided
if [ -z "$OPENAI_API_KEY" ]; then
    log "ERROR: OPENAI_API_KEY environment variable is required"
    exit 1
fi

# Create necessary directories
mkdir -p "$REPORTS_DIR" "$LOGS_DIR"

# Set default values
JAVA_OPTS=${JAVA_OPTS:-"-Xmx512m -Xms256m"}
INPUT_DIR=${INPUT_DIR:-"/app/input"}

log "Starting Code Guard application..."
log "Java Options: $JAVA_OPTS"
log "Reports Directory: $REPORTS_DIR"
log "Input Directory: $INPUT_DIR"

# If no arguments provided, show help
if [ $# -eq 0 ]; then
    set -- "--help"
fi

# Special handling for common Docker use cases
case "$1" in
    "scan")
        # Docker scan mode: scan the input directory
        log "Scanning input directory: $INPUT_DIR"
        exec java $JAVA_OPTS -jar /app/app.jar \
            --scan "$INPUT_DIR" \
            --output "$REPORTS_DIR" \
            "${@:2}"
        ;;
    "analyze")
        # Docker analyze mode: analyze specific files
        log "Analyzing files: ${@:2}"
        exec java $JAVA_OPTS -jar /app/app.jar \
            --output "$REPORTS_DIR" \
            "${@:2}"
        ;;
    "qa-automation")
        # QA automation mode
        log "Running in QA automation mode"
        exec java $JAVA_OPTS -jar /app/app.jar \
            --scan "$INPUT_DIR" \
            --mode qa-automation \
            --threshold "${QUALITY_THRESHOLD:-75}" \
            --output "$REPORTS_DIR" \
            --format json \
            "${@:2}"
        ;;
    "devops-testing")
        # DevOps testing mode
        log "Running in DevOps testing mode"
        exec java $JAVA_OPTS -jar /app/app.jar \
            --scan "$INPUT_DIR" \
            --mode devops-testing \
            --threshold "${QUALITY_THRESHOLD:-80}" \
            --output "$REPORTS_DIR" \
            "${@:2}"
        ;;
    *)
        # Pass all arguments directly to the application
        exec java $JAVA_OPTS -jar /app/app.jar "$@"
        ;;
esac
