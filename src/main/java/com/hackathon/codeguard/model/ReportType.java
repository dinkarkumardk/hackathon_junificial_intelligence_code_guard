package com.hackathon.codeguard.model;

/**
 * Represents the type of report to generate
 */
public enum ReportType {
    TECHNICAL("Technical Report with detailed metrics, issues, and suggestions"),
    NON_TECHNICAL("Non-technical report with executive summary and recommendations"),
    BOTH("Both technical and non-technical reports");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
