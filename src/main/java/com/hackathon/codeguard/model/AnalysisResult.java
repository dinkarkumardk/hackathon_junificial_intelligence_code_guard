package com.hackathon.codeguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete analysis result for all code files
 */
public class AnalysisResult {
    
    @JsonProperty("overallScore")
    private double overallScore;
    
    @JsonProperty("fileResults")
    private List<FileAnalysisResult> fileResults;
    
    @JsonProperty("summary")
    private AnalysisSummary summary;
    
    @JsonProperty("timestamp")
    private String timestamp;

    // Constructors
    public AnalysisResult() {}

    public AnalysisResult(double overallScore, List<FileAnalysisResult> fileResults, 
                         AnalysisSummary summary, String timestamp) {
        this.overallScore = overallScore;
        this.fileResults = fileResults;
        this.summary = summary;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public List<FileAnalysisResult> getFileResults() {
        return fileResults;
    }

    public void setFileResults(List<FileAnalysisResult> fileResults) {
        this.fileResults = fileResults;
    }

    public AnalysisSummary getSummary() {
        return summary;
    }

    public void setSummary(AnalysisSummary summary) {
        this.summary = summary;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Inner class representing analysis summary
     */
    public static class AnalysisSummary {
        @JsonProperty("totalFiles")
        private int totalFiles;
        
        @JsonProperty("averageScore")
        private double averageScore;
        
        @JsonProperty("highQualityFiles")
        private int highQualityFiles;
        
        @JsonProperty("mediumQualityFiles") 
        private int mediumQualityFiles;
        
        @JsonProperty("lowQualityFiles")
        private int lowQualityFiles;
        
        @JsonProperty("criticalIssues")
        private int criticalIssues;
        
        @JsonProperty("recommendations")
        private List<String> recommendations;

        // Constructors
        public AnalysisSummary() {}

        // Getters and Setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        
        public int getHighQualityFiles() { return highQualityFiles; }
        public void setHighQualityFiles(int highQualityFiles) { this.highQualityFiles = highQualityFiles; }
        
        public int getMediumQualityFiles() { return mediumQualityFiles; }
        public void setMediumQualityFiles(int mediumQualityFiles) { this.mediumQualityFiles = mediumQualityFiles; }
        
        public int getLowQualityFiles() { return lowQualityFiles; }
        public void setLowQualityFiles(int lowQualityFiles) { this.lowQualityFiles = lowQualityFiles; }
        
        public int getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}
