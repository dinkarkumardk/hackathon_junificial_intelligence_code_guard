package com.hackathon.codeguard.service;

import com.hackathon.codeguard.cli.CodeGuardCLI.AnalysisMode;
import com.hackathon.codeguard.model.AnalysisResult;
import com.hackathon.codeguard.model.FileAnalysisResult;
import com.hackathon.codeguard.service.openai.OpenAIAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Main service for coordinating code analysis using OpenAI APIs
 */
public class CodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisService.class);
    
    private final OpenAIAnalysisService openAIService;
    private final FileProcessingService fileService;

    public CodeAnalysisService() {
        this.openAIService = new OpenAIAnalysisService();
        this.fileService = new FileProcessingService();
    }

    /**
     * Analyzes multiple code files and returns comprehensive results
     */
    public AnalysisResult analyzeFiles(List<Path> filePaths, AnalysisMode mode, boolean ktEnabled) throws Exception {
        logger.info("Starting analysis of {} files in {} mode", filePaths.size(), mode);
        
        List<FileAnalysisResult> fileResults = new ArrayList<>();
        double totalScore = 0.0;

        for (Path filePath : filePaths) {
            try {
                logger.debug("Analyzing file: {}", filePath);
                
                // Read file content
                String fileContent = fileService.readFileContent(filePath);
                
                // Analyze with OpenAI
                FileAnalysisResult result = openAIService.analyzeCodeFile(filePath, fileContent, mode, ktEnabled);
                fileResults.add(result);
                totalScore += result.getFinalScore();
                
            } catch (Exception e) {
                logger.warn("Error analyzing file {}: {}", filePath, e.getMessage());
            }
        }

        // Calculate overall metrics
        double overallScore = fileResults.isEmpty() ? 0.0 : totalScore / fileResults.size();
        
        // Create summary
        AnalysisResult.AnalysisSummary summary = createSummary(fileResults, overallScore);
        
        // Create final result
        AnalysisResult result = new AnalysisResult(
            overallScore,
            fileResults,
            summary,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        logger.info("Analysis complete. Overall score: {}", overallScore);
        return result;
    }

    private AnalysisResult.AnalysisSummary createSummary(List<FileAnalysisResult> fileResults, double overallScore) {
        AnalysisResult.AnalysisSummary summary = new AnalysisResult.AnalysisSummary();
        
        summary.setTotalFiles(fileResults.size());
        summary.setAverageScore(overallScore);
        
        // Count quality levels
        int high = 0, medium = 0, low = 0, critical = 0;
        
        for (FileAnalysisResult result : fileResults) {
            double score = result.getFinalScore();
            if (score >= 85) high++;
            else if (score >= 70) medium++;
            else low++;
            
            // Count critical issues
            if (result.getIssues() != null) {
                critical += (int) result.getIssues().stream()
                    .filter(issue -> "CRITICAL".equalsIgnoreCase(issue.getSeverity()))
                    .count();
            }
        }
        
        summary.setHighQualityFiles(high);
        summary.setMediumQualityFiles(medium);
        summary.setLowQualityFiles(low);
        summary.setCriticalIssues(critical);
        
        // Generate recommendations
        List<String> recommendations = generateRecommendations(fileResults, overallScore);
        summary.setRecommendations(recommendations);
        
        return summary;
    }

    private List<String> generateRecommendations(List<FileAnalysisResult> fileResults, double overallScore) {
        List<String> recommendations = new ArrayList<>();
        
        if (overallScore < 70) {
            recommendations.add("Overall code quality is below acceptable threshold. Consider comprehensive refactoring.");
        }
        
        long lowQualityFiles = fileResults.stream()
            .filter(r -> r.getFinalScore() < 70)
            .count();
            
        if (lowQualityFiles > 0) {
            recommendations.add("Focus on improving " + lowQualityFiles + " files with low quality scores.");
        }
        
        // Add more specific recommendations based on common issues
        recommendations.add("Review security practices and implement recommended improvements.");
        recommendations.add("Consider implementing design patterns where appropriate.");
        recommendations.add("Ensure all code follows SOLID principles.");
        
        return recommendations;
    }
}
