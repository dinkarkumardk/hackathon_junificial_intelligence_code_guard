# Code Guard

A minimal code analysis tool that scores code files and generates technical & non-technical HTML reports using OpenAI APIs.

## Features

- **Code Quality Analysis**: Analyzes code for readability, maintainability, security, and documentation
- **Multi-Criteria Scoring**: Evaluates Code Quality, SOLID principles, Design Patterns, Clean Code, and Security
- **Weighted Final Score**: Calculates final score with customizable weights
- **HTML Report Generation**: Creates both technical and non-technical reports
- **JSON Export**: Supports JSON format for integration with other tools
- **Multiple Analysis Modes**: Standard, QA Automation, DevOps Testing, and Developer Review modes
- **CI/CD Integration**: Easy integration with build pipelines and quality gates

## Requirements

- Java 17 or higher
- OpenAI API key
- Maven 3.6 or higher

## Setup

### Local Development

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd hackathon_junificial_intelligence_code_guard
   ```

2. **Set OpenAI API Key**:
   ```bash
   export OPENAI_API_KEY="your-openai-api-key"
   ```

3. **Build the project**:
   ```bash
   mvn clean compile
   ```

4. **Run tests**:
   ```bash
   mvn test
   ```

5. **Create executable JAR**:
   ```bash
   mvn clean package
   ```

### Docker Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd hackathon_junificial_intelligence_code_guard
   ```

2. **Configure environment**:
   ```bash
   cp .env.example .env
   # Edit .env and set your OPENAI_API_KEY
   ```

3. **Build Docker image**:
   ```bash
   ./docker-run.sh build
   ```

4. **Run analysis**:
   ```bash
   ./docker-run.sh run
   ```

## Usage

### Command Line Interface (Local)

```bash
# Analyze specific files
java -jar target/code-guard-1.0.0.jar file1.java file2.js

# Scan directory recursively
java -jar target/code-guard-1.0.0.jar --scan ./src

# Generate JSON report
java -jar target/code-guard-1.0.0.jar --scan ./src --format json

# Set quality threshold
java -jar target/code-guard-1.0.0.jar --scan ./src --threshold 80

# QA Automation mode
java -jar target/code-guard-1.0.0.jar --scan ./src --mode qa-automation --threshold 75

# DevOps Testing mode
java -jar target/code-guard-1.0.0.jar --scan ./src --mode devops-testing --threshold 85

# Generate only technical report
java -jar target/code-guard-1.0.0.jar --scan ./src --report-type technical

# Custom output directory
java -jar target/code-guard-1.0.0.jar --scan ./src --output ./custom-reports
```

### Docker Usage

```bash
# Basic analysis
./docker-run.sh run

# QA automation with custom threshold
./docker-run.sh qa 75

# DevOps testing with custom threshold
./docker-run.sh devops 85

# Using docker-compose profiles
./docker-run.sh compose qa      # QA profile
./docker-run.sh compose devops  # DevOps profile
./docker-run.sh compose dev     # Development profile

# Direct Docker commands
docker run --rm \
  -e OPENAI_API_KEY="your-api-key" \
  -v "$(pwd)/src:/app/input:ro" \
  -v "$(pwd)/reports:/app/reports" \
  code-guard:latest scan --threshold 80

# Docker Compose for different environments
docker-compose --profile qa up        # QA environment
docker-compose --profile devops up    # DevOps environment
docker-compose --profile pipeline up  # CI/CD pipeline
```

### Command Line Options

- `FILES`: Code files or directories to analyze
- `-o, --output`: Output directory for reports (default: ./reports)
- `-t, --threshold`: Minimum quality score threshold (default: 70)
- `-m, --mode`: Analysis mode (standard, qa-automation, devops-testing, developer-review)
- `-r, --report-type`: Report type (technical, non-technical, both)
- `-f, --format`: Output format (html, json)
- `--scan`: Scan directory recursively for code files
- `-h, --help`: Show help message
- `-V, --version`: Show version information

## CI/CD Integration

### GitHub Actions

```yaml
name: Code Quality Gate
on: [push, pull_request]

jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Code Quality Analysis with Docker
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: |
          echo "OPENAI_API_KEY=$OPENAI_API_KEY" > .env
          ./docker-run.sh build
          ./docker-run.sh qa 75
      
      - name: Upload Reports
        uses: actions/upload-artifact@v3
        with:
          name: quality-reports
          path: reports/
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    environment {
        OPENAI_API_KEY = credentials('openai-api-key')
    }
    stages {
        stage('Code Quality Analysis') {
            steps {
                sh '''
                    echo "OPENAI_API_KEY=${OPENAI_API_KEY}" > .env
                    ./docker-run.sh build
                    ./docker-run.sh devops 80
                '''
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'reports',
                        reportFiles: 'technical-report.html',
                        reportName: 'Code Quality Report'
                    ])
                }
            }
        }
    }
}
```

### GitLab CI

```yaml
stages:
  - quality-check

quality-analysis:
  stage: quality-check
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - echo "OPENAI_API_KEY=$OPENAI_API_KEY" > .env
  script:
    - ./docker-run.sh build
    - ./docker-run.sh qa 75
  artifacts:
    reports:
      junit: reports/*.xml
    paths:
      - reports/
    expire_in: 1 week
  only:
    - main
    - merge_requests
```

### Kubernetes Deployment

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: code-guard-analysis
spec:
  template:
    spec:
      containers:
      - name: code-guard
        image: code-guard:latest
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-secret
              key: api-key
        - name: QUALITY_THRESHOLD
          value: "80"
        volumeMounts:
        - name: source-code
          mountPath: /app/input
        - name: reports-volume
          mountPath: /app/reports
        command: ["qa-automation"]
      volumes:
      - name: source-code
        configMap:
          name: source-code-config
      - name: reports-volume
        persistentVolumeClaim:
          claimName: reports-pvc
      restartPolicy: Never
```

## Scoring Algorithm

The final score is calculated using weighted criteria:

```
Final Score = (Code Quality × 0.30) + (SOLID × 0.25) + (Design Patterns × 0.15) + (Clean Code × 0.20) + (Security × 0.10)
```

### Quality Indicators

- **Green (85-100)**: High quality code
- **Yellow (70-84)**: Medium quality, some improvements needed
- **Red (0-69)**: Low quality, significant improvements required

## Report Types

### Technical Report
- Detailed metrics and scores for each file
- Specific issues and suggestions
- Code complexity analysis
- Security vulnerability assessment
- Suitable for developers and technical teams

### Executive Report
- High-level summary and overview
- Strategic recommendations
- Quality distribution analysis
- Business impact assessment
- Suitable for managers and stakeholders

## Supported Languages

- Java (.java)
- JavaScript (.js)
- TypeScript (.ts)
- Python (.py)
- C++ (.cpp, .cc)
- C (.c)
- C# (.cs)
- PHP (.php)
- Ruby (.rb)
- Go (.go)
- Kotlin (.kt)
- Scala (.scala)

## Configuration

Configuration can be customized in `src/main/resources/application.properties`:

```properties
# Quality Thresholds
quality.threshold.high=85
quality.threshold.medium=70
quality.threshold.low=50

# Analysis Configuration
analysis.max.file.size=1048576
analysis.supported.extensions=.java,.js,.ts,.py,.cpp,.c,.cs,.php,.rb,.go,.kt,.scala

# OpenAI Configuration
openai.model=gpt-4
openai.max.tokens=2000
openai.temperature=0.1
```

## Docker Deployment

### Quick Start with Docker

1. **Setup environment**:
   ```bash
   cp .env.example .env
   # Edit .env and set OPENAI_API_KEY
   ```

2. **Build and run**:
   ```bash
   ./docker-run.sh build
   ./docker-run.sh run
   ```

### Docker Commands

```bash
# Build image
./docker-run.sh build

# Run analysis with default settings
./docker-run.sh run

# Run QA automation
./docker-run.sh qa 75

# Run DevOps testing
./docker-run.sh devops 85

# Use docker-compose profiles
./docker-run.sh compose qa      # QA environment
./docker-run.sh compose devops  # DevOps environment
./docker-run.sh compose dev     # Development environment

# Clean up
./docker-run.sh clean           # Remove containers
./docker-run.sh clean --all     # Remove containers and image
```

### Docker Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key (required) | - |
| `QUALITY_THRESHOLD` | Quality threshold for analysis | 75 |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` |
| `INPUT_DIR` | Input directory path | `/app/input` |
| `REPORTS_DIR` | Reports output directory | `/app/reports` |
| `LOGS_DIR` | Logs directory | `/app/logs` |

### Docker Volumes

- `/app/input` - Mount your source code here (read-only)
- `/app/reports` - Analysis reports output
- `/app/logs` - Application logs

### Production Deployment

```bash
# Build production image
docker build -t code-guard:prod --target production .

# Run in production mode
docker run -d \
  --name code-guard-prod \
  -e OPENAI_API_KEY="your-api-key" \
  -e JAVA_OPTS="-Xmx2g -Xms1g" \
  -v /path/to/source:/app/input:ro \
  -v /path/to/reports:/app/reports \
  -v /path/to/logs:/app/logs \
  --restart unless-stopped \
  code-guard:prod devops-testing
```

## Development

### Local Development

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FileProcessingServiceTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Docker Development

```bash
# Build and run in development mode
./docker-run.sh compose dev

# Build image for testing
./docker-run.sh build

# Clean up Docker resources
./docker-run.sh clean

# View container logs
./docker-run.sh logs

# Follow logs in real-time
./docker-run.sh logs -f
```

### Building for Production

```bash
# Local JAR build
mvn clean package

# Docker production build
./docker-run.sh build

# Create distribution package
mvn clean package assembly:single

# Build multi-architecture Docker image
docker buildx build --platform linux/amd64,linux/arm64 -t code-guard:latest --push .
```

### Project Structure

```
├── Dockerfile                          # Multi-stage Docker build
├── docker-compose.yml                  # Docker Compose configuration
├── docker-run.sh                       # Docker management script
├── .dockerignore                       # Docker ignore file
├── .env.example                        # Environment variables template
├── pom.xml                             # Maven configuration
├── README.md                           # Documentation
├── docker/
│   └── entrypoint.sh                   # Docker entrypoint script
└── src/
    ├── main/
    │   ├── java/com/hackathon/codeguard/
    │   │   ├── CodeGuardApplication.java
    │   │   ├── cli/
    │   │   │   └── CodeGuardCLI.java
    │   │   ├── model/
    │   │   │   ├── AnalysisResult.java
    │   │   │   ├── FileAnalysisResult.java
    │   │   │   └── ReportType.java
    │   │   └── service/
    │   │       ├── CodeAnalysisService.java
    │   │       ├── FileProcessingService.java
    │   │       ├── ReportGenerationService.java
    │   │       └── openai/
    │   │           └── OpenAIAnalysisService.java
    │   └── resources/
    │       ├── application.properties
    │       └── log4j2.properties
    └── test/
        └── java/com/hackathon/codeguard/
            ├── CodeGuardApplicationTest.java
            └── service/
                └── FileProcessingServiceTest.java
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run tests and ensure they pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the example configurations

## Changelog

### v1.0.0
- Initial release
- Basic code analysis functionality
- HTML and JSON report generation
- CI/CD integration support
- Multi-language support
- Quality threshold configuration
- **Docker support with multi-stage builds**
- **Docker Compose configurations for different environments**
- **Docker management script for easy deployment**
- **Container-based CI/CD pipeline integration**
- **Production-ready Docker deployment options**