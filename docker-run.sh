#!/bin/bash

# Code Guard Docker Build and Deployment Script
# This script provides easy commands for building and running Code Guard with Docker

set -e

# Configuration
IMAGE_NAME="code-guard"
IMAGE_TAG="latest"
CONTAINER_NAME="code-guard-app"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_warn "Docker Compose is not installed. Some features may not work."
    fi
}

# Check if .env file exists
check_env() {
    if [ ! -f .env ]; then
        log_warn ".env file not found. Creating from .env.example"
        cp .env.example .env
        log_warn "Please edit .env file and set your OPENAI_API_KEY"
        return 1
    fi
    
    # Source the .env file
    set -a
    source .env
    set +a
    
    if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "your-openai-api-key-here" ]; then
        log_error "OPENAI_API_KEY is not set in .env file"
        return 1
    fi
    
    return 0
}

# Build Docker image
build() {
    log_info "Building Docker image: $IMAGE_NAME:$IMAGE_TAG"
    docker build -t "$IMAGE_NAME:$IMAGE_TAG" .
    log_success "Docker image built successfully"
}

# Run analysis on current directory
run_analysis() {
    local threshold=${1:-75}
    local mode=${2:-"standard"}
    
    log_info "Running Code Guard analysis (threshold: $threshold, mode: $mode)"
    
    # Create directories if they don't exist
    mkdir -p reports logs
    
    docker run --rm \
        -e OPENAI_API_KEY="$OPENAI_API_KEY" \
        -e QUALITY_THRESHOLD="$threshold" \
        -v "$(pwd)/src:/app/input:ro" \
        -v "$(pwd)/reports:/app/reports" \
        -v "$(pwd)/logs:/app/logs" \
        "$IMAGE_NAME:$IMAGE_TAG" \
        scan --threshold "$threshold" --mode "$mode"
    
    log_success "Analysis complete. Check reports/ directory for results."
}

# Run QA automation
run_qa() {
    local threshold=${1:-70}
    
    log_info "Running QA automation (threshold: $threshold)"
    
    mkdir -p reports logs
    
    docker run --rm \
        -e OPENAI_API_KEY="$OPENAI_API_KEY" \
        -e QUALITY_THRESHOLD="$threshold" \
        -v "$(pwd)/src:/app/input:ro" \
        -v "$(pwd)/reports:/app/reports" \
        -v "$(pwd)/logs:/app/logs" \
        "$IMAGE_NAME:$IMAGE_TAG" \
        qa-automation
    
    if [ $? -eq 0 ]; then
        log_success "QA automation passed"
    else
        log_error "QA automation failed - quality threshold not met"
        exit 1
    fi
}

# Run DevOps testing
run_devops() {
    local threshold=${1:-80}
    
    log_info "Running DevOps testing (threshold: $threshold)"
    
    mkdir -p reports logs
    
    docker run --rm \
        -e OPENAI_API_KEY="$OPENAI_API_KEY" \
        -e QUALITY_THRESHOLD="$threshold" \
        -v "$(pwd)/src:/app/input:ro" \
        -v "$(pwd)/reports:/app/reports" \
        -v "$(pwd)/logs:/app/logs" \
        "$IMAGE_NAME:$IMAGE_TAG" \
        devops-testing
    
    if [ $? -eq 0 ]; then
        log_success "DevOps testing passed"
    else
        log_error "DevOps testing failed - quality threshold not met"
        exit 1
    fi
}

# Run with docker-compose
compose_up() {
    local profile=${1:-"analysis"}
    
    log_info "Starting Code Guard with docker-compose (profile: $profile)"
    
    docker-compose --profile "$profile" up --build
}

# Clean up Docker resources
clean() {
    log_info "Cleaning up Docker resources"
    
    # Stop and remove containers
    docker-compose down 2>/dev/null || true
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
    
    # Remove image if requested
    if [ "$1" = "--all" ]; then
        docker rmi "$IMAGE_NAME:$IMAGE_TAG" 2>/dev/null || true
        log_success "Removed Docker image and containers"
    else
        log_success "Stopped and removed containers"
    fi
}

# Show logs
logs() {
    local follow=${1:-""}
    
    if [ "$follow" = "-f" ] || [ "$follow" = "--follow" ]; then
        docker logs -f "$CONTAINER_NAME" 2>/dev/null || log_error "Container not running"
    else
        docker logs "$CONTAINER_NAME" 2>/dev/null || log_error "Container not found"
    fi
}

# Show usage
usage() {
    echo "Code Guard Docker Management Script"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  build                     Build Docker image"
    echo "  run [threshold] [mode]    Run analysis (default: threshold=75, mode=standard)"
    echo "  qa [threshold]            Run QA automation (default: threshold=70)"
    echo "  devops [threshold]        Run DevOps testing (default: threshold=80)"
    echo "  compose [profile]         Run with docker-compose (profiles: analysis, qa, devops, pipeline, dev)"
    echo "  clean [--all]             Clean up containers (--all to remove image too)"
    echo "  logs [-f]                 Show container logs (-f to follow)"
    echo "  help                      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 run 80 qa-automation"
    echo "  $0 qa 75"
    echo "  $0 devops 85"
    echo "  $0 compose qa"
    echo "  $0 clean --all"
    echo ""
    echo "Environment:"
    echo "  Requires .env file with OPENAI_API_KEY set"
    echo "  See .env.example for template"
}

# Main script logic
main() {
    check_docker
    
    case "${1:-help}" in
        "build")
            build
            ;;
        "run")
            if ! check_env; then exit 1; fi
            run_analysis "$2" "$3"
            ;;
        "qa")
            if ! check_env; then exit 1; fi
            run_qa "$2"
            ;;
        "devops")
            if ! check_env; then exit 1; fi
            run_devops "$2"
            ;;
        "compose")
            if ! check_env; then exit 1; fi
            compose_up "$2"
            ;;
        "clean")
            clean "$2"
            ;;
        "logs")
            logs "$2"
            ;;
        "help"|"--help"|"-h")
            usage
            ;;
        *)
            log_error "Unknown command: $1"
            usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
