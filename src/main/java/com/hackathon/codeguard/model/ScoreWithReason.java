package com.hackathon.codeguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a score with detailed reasoning
 */
public class ScoreWithReason {
    
    @JsonProperty("score")
    private double score;
    
    @JsonProperty("reason")
    private String reason;

    // Constructors
    public ScoreWithReason() {}

    public ScoreWithReason(double score, String reason) {
        this.score = score;
        this.reason = reason;
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

    @Override
    public String toString() {
        return String.format("ScoreWithReason{score=%.1f, reason='%s'}", score, reason);
    }
}
