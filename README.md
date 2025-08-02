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

## Usage

### Command Line Interface

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
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build Code Guard
        run: mvn clean package -DskipTests
      
      - name: Run Code Quality Analysis
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: |
          java -jar target/code-guard-1.0.0.jar --scan ./src --threshold 75 --format json
          if [ $? -ne 0 ]; then
            echo "Quality gate failed"
            exit 1
          fi
      
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
                sh 'mvn clean package -DskipTests'
                sh '''
                    java -jar target/code-guard-1.0.0.jar \
                        --scan ./src \
                        --mode devops-testing \
                        --threshold 80 \
                        --output ./reports
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

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/hackathon/codeguard/
│   │       ├── CodeGuardApplication.java
│   │       ├── cli/
│   │       │   └── CodeGuardCLI.java
│   │       ├── model/
│   │       │   ├── AnalysisResult.java
│   │       │   ├── FileAnalysisResult.java
│   │       │   └── ReportType.java
│   │       └── service/
│   │           ├── CodeAnalysisService.java
│   │           ├── FileProcessingService.java
│   │           ├── ReportGenerationService.java
│   │           └── openai/
│   │               └── OpenAIAnalysisService.java
│   └── resources/
│       ├── application.properties
│       └── log4j2.properties
└── test/
    └── java/
        └── com/hackathon/codeguard/
            ├── CodeGuardApplicationTest.java
            └── service/
                └── FileProcessingServiceTest.java
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FileProcessingServiceTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Building for Production

```bash
# Create executable JAR
mvn clean package

# Skip tests during build
mvn clean package -DskipTests

# Create distribution package
mvn clean package assembly:single
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