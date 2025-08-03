package com.hackathon.codeguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents the analysis result for a single code file
 */
public class FileAnalysisResult {
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("filepath")
    private String filepath;
    
    @JsonProperty("codeQuality")
    private double codeQuality;
    
    @JsonProperty("solid")
    private double solid;
    
    @JsonProperty("designPatterns")
    private double designPatterns;
    
    @JsonProperty("cleanCode")
    private double cleanCode;
    
    @JsonProperty("security")
    private double security;
    
    @JsonProperty("bugDetection")
    private double bugDetection;
    
    @JsonProperty("finalScore")
    private double finalScore;
    
    @JsonProperty("qualityIndicator")
    private QualityIndicator qualityIndicator;
    
    // Reason fields for detailed explanations
    @JsonProperty("codeQualityReason")
    private String codeQualityReason;
    
    @JsonProperty("solidReason")
    private String solidReason;
    
    @JsonProperty("designPatternsReason")
    private String designPatternsReason;
    
    @JsonProperty("securityReason")
    private String securityReason;
    
    @JsonProperty("bugDetectionReason")
    private String bugDetectionReason;
    
    @JsonProperty("issues")
    private List<CodeIssue> issues;
    
    @JsonProperty("suggestions")
    private List<String> suggestions;
    
    @JsonProperty("metrics")
    private Map<String, Object> metrics;

    // Constructors
    public FileAnalysisResult() {}

    public FileAnalysisResult(String filename, String filepath) {
        this.filename = filename;
        this.filepath = filepath;
    }

    // Calculate final score based on weighted criteria
    public void calculateFinalScore() {
        this.finalScore = (codeQuality * 0.25) + (solid * 0.20) + (designPatterns * 0.15) + 
                         (security * 0.20) + (bugDetection * 0.20);
        this.qualityIndicator = determineQualityIndicator(finalScore);
    }

    private QualityIndicator determineQualityIndicator(double score) {
        if (score >= 85) return QualityIndicator.GREEN;
        if (score >= 70) return QualityIndicator.YELLOW;
        return QualityIndicator.RED;
    }

    // Getters and Setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getFilepath() { return filepath; }
    public void setFilepath(String filepath) { this.filepath = filepath; }
    
    public double getCodeQuality() { return codeQuality; }
    public void setCodeQuality(double codeQuality) { this.codeQuality = codeQuality; }
    
    public double getSolid() { return solid; }
    public void setSolid(double solid) { this.solid = solid; }
    
    public double getDesignPatterns() { return designPatterns; }
    public void setDesignPatterns(double designPatterns) { this.designPatterns = designPatterns; }
    
    public double getCleanCode() { return cleanCode; }
    public void setCleanCode(double cleanCode) { this.cleanCode = cleanCode; }
    
    public double getSecurity() { return security; }
    public void setSecurity(double security) { this.security = security; }
    
    public double getBugDetection() { return bugDetection; }
    public void setBugDetection(double bugDetection) { this.bugDetection = bugDetection; }
    
    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
    
    public QualityIndicator getQualityIndicator() { return qualityIndicator; }
    public void setQualityIndicator(QualityIndicator qualityIndicator) { this.qualityIndicator = qualityIndicator; }
    
    public List<CodeIssue> getIssues() { return issues; }
    public void setIssues(List<CodeIssue> issues) { this.issues = issues; }
    
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    
    // Reason getters and setters
    public String getCodeQualityReason() { return codeQualityReason; }
    public void setCodeQualityReason(String codeQualityReason) { this.codeQualityReason = codeQualityReason; }
    
    public String getSolidReason() { return solidReason; }
    public void setSolidReason(String solidReason) { this.solidReason = solidReason; }
    
    public String getDesignPatternsReason() { return designPatternsReason; }
    public void setDesignPatternsReason(String designPatternsReason) { this.designPatternsReason = designPatternsReason; }
    
    public String getSecurityReason() { return securityReason; }
    public void setSecurityReason(String securityReason) { this.securityReason = securityReason; }
    
    public String getBugDetectionReason() { return bugDetectionReason; }
    public void setBugDetectionReason(String bugDetectionReason) { this.bugDetectionReason = bugDetectionReason; }

    /**
     * Quality indicator enum for color coding
     */
    public enum QualityIndicator {
        GREEN("High Quality", "#28a745"),
        YELLOW("Medium Quality", "#ffc107"), 
        RED("Low Quality", "#dc3545");

        private final String label;
        private final String color;

        QualityIndicator(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
    }

    /**
     * Represents a code issue found during analysis
     */
    public static class CodeIssue {
        @JsonProperty("severity")
        private String severity;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("lineNumber")
        private Integer lineNumber;
        
        @JsonProperty("suggestion")
        private String suggestion;

        // Constructors
        public CodeIssue() {}

        public CodeIssue(String severity, String type, String description, Integer lineNumber, String suggestion) {
            this.severity = severity;
            this.type = type;
            this.description = description;
            this.lineNumber = lineNumber;
            this.suggestion = suggestion;
        }

        // Getters and Setters
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getLineNumber() { return lineNumber; }
        public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
