package com.hackathon.codeguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a score with detailed reasoning and recommendations
 */
public class ScoreWithReason {
    
    @JsonProperty("score")
    private double score;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("recommendations")
    private List<String> recommendations;

    // Constructors
    public ScoreWithReason() {
        this.recommendations = new ArrayList<>();
    }

    public ScoreWithReason(double score, String reason) {
        this.score = score;
        this.reason = reason;
        this.recommendations = new ArrayList<>();
    }

    public ScoreWithReason(double score, String reason, List<String> recommendations) {
        this.score = score;
        this.reason = reason;
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    // Getters and Setters
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("ScoreWithReason{score=%.1f, reason='%s', recommendations=%s}", 
                           score, reason, recommendations);
    }
}
