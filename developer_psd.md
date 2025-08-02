# Simple Product Specification Document (PSD)

## Overview
A minimal code analysis tool that scores code files and generates technical & non-technical HTML reports. All code processing, scoring, and report generation use OpenAI APIs. No other dependencies.

## Use Cases

### General Use Cases
- Score code files for quality
- Generate technical HTML report (code metrics, issues, suggestions)
- Generate non-technical HTML report (summary, recommendations)

### Developer Use Cases
- **Code Review Preparation**: Score code before submitting pull requests to ensure quality standards
- **Refactoring Guidance**: Identify areas of code that need improvement with specific suggestions
- **Learning & Improvement**: Use detailed technical reports to understand code quality best practices
- **Documentation Quality Check**: Verify that code is properly documented and maintainable
- **Security Vulnerability Detection**: Early identification of potential security issues in code


## Components

### 1. Scoring System
- Uses OpenAI API to analyze code files
- Returns a quality score (0-100)
- Criteria: readability, maintainability, security, documentation

### 2. Technical Report Generator
- Uses OpenAI API to extract metrics, issues, and suggestions
- Generates HTML report (JaCoCo-like format)
- Sections: Metrics, Issues, Suggestions

### 3. Non-Technical Report Generator
- Uses OpenAI API to summarize findings for non-technical stakeholders
- Generates HTML report (simple, readable)
- Sections: Executive Summary, Recommendations

## Technology
- Language: Java 17
- Dependency: OpenAI API
- Output: HTML files

## Scalability
- Stateless API calls
- Can be run locally or in cloud

## Responsibility
- Code file scoring
- HTML report generation (technical & non-technical)
- No local code parsing or analysis

---

## HTML Report Generation Requirement

- The tool must generate a report in HTML format that lists the score for each code file.
- For each file, the report should display:
  - Filename
  - Individual scores for: Code Quality, SOLID, Design Patterns, Clean Code, Security
  - Final Score, calculated as:
    Final Score = (Code Quality × 0.30) + (SOLID × 0.25) + (Design Patterns × 0.15) + (Clean Code × 0.20) + (Security × 0.10)
  - Color-coded quality indicator (e.g., Green/Yellow/Red)
- The report should be in a table or grid format for easy comparison.
- Reports must be clear and structured so they can be used as KT (Knowledge Transfer) material for onboarding new team members.
- Only OpenAI APIs are used for code file analysis, scoring, and report generation.

---
