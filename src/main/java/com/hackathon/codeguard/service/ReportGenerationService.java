package com.hackathon.codeguard.service;

import com.hackathon.codeguard.model.AnalysisResult;
import com.hackathon.codeguard.model.FileAnalysisResult;
import com.hackathon.codeguard.model.ReportType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.codeguard.service.openai.OpenAIAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Map;

/**
 * Service for generating HTML and JSON reports from analysis results
 */
public class ReportGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);
    
    private final ObjectMapper objectMapper;

    public ReportGenerationService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates reports based on analysis results
     */
    public void generateReports(AnalysisResult result, String outputDir, ReportType reportType, String format) throws IOException {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        
        logger.info("Generating {} reports in {} format to: {}", reportType, format, outputDir);
        
        if ("json".equalsIgnoreCase(format)) {
            generateJsonReport(result, outputPath);
        } else {
            // Default to HTML
            if (reportType == ReportType.TECHNICAL || reportType == ReportType.BOTH) {
                generateTechnicalHtmlReport(result, outputPath);
                generateDetailedAnalysisFiles(result, outputPath); // Generate individual file analysis reports
            }
            
            if (reportType == ReportType.NON_TECHNICAL || reportType == ReportType.BOTH) {
                generateNonTechnicalHtmlReport(result, outputPath);
            }
        }
    }

    private void generateJsonReport(AnalysisResult result, Path outputPath) throws IOException {
        Path jsonFile = outputPath.resolve("analysis-report.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), result);
        logger.info("JSON report generated: {}", jsonFile);
    }

    private void generateTechnicalHtmlReport(AnalysisResult result, Path outputPath) throws IOException {
        String htmlContent = buildTechnicalHtmlReport(result);
        Path htmlFile = outputPath.resolve("technical-report.html");
        Files.writeString(htmlFile, htmlContent);
        logger.info("Technical HTML report generated: {}", htmlFile);
    }

    private void generateNonTechnicalHtmlReport(AnalysisResult result, Path outputPath) throws IOException {
        String htmlContent = buildNonTechnicalHtmlReport(result);
        Path htmlFile = outputPath.resolve("executive-report.html");
        Files.writeString(htmlFile, htmlContent);
        logger.info("Non-technical HTML report generated: {}", htmlFile);
    }

    private String buildTechnicalHtmlReport(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Code Guard - Technical Analysis Report</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #007acc; }
                    .header h1 { color: #007acc; margin: 0; font-size: 2.5em; }
                    .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
                    .metric-card { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; border: 1px solid #dee2e6; }
                    .metric-value { font-size: 2em; font-weight: bold; color: #007acc; }
                    .metric-label { color: #666; margin-top: 5px; }
                    .files-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    .files-table th, .files-table td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    .files-table th { background-color: #007acc; color: white; font-weight: bold; }
                    .files-table tr:hover { background-color: #f5f5f5; }
                    .quality-green { background-color: #28a745; color: white; padding: 4px 8px; border-radius: 4px; }
                    .quality-yellow { background-color: #ffc107; color: black; padding: 4px 8px; border-radius: 4px; }
                    .quality-red { background-color: #dc3545; color: white; padding: 4px 8px; border-radius: 4px; }
                    .score { font-weight: bold; }
                    .timestamp { text-align: center; color: #666; margin-top: 20px; font-size: 0.9em; }
                    .issues-section { margin-top: 30px; }
                    .issue-item { background: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 10px 0; }
                    .issue-critical { border-left-color: #dc3545; background: #f8d7da; }
                    .issue-high { border-left-color: #fd7e14; background: #ffeeba; }
                    .tooltip { position: relative; cursor: help; }
                    .tooltip .tooltiptext { visibility: hidden; width: 300px; background-color: #555; color: #fff; text-align: left; border-radius: 6px; padding: 10px; position: absolute; z-index: 1; bottom: 125%; left: 50%; margin-left: -150px; opacity: 0; transition: opacity 0.3s; font-size: 0.9em; line-height: 1.4; }
                    .tooltip:hover .tooltiptext { visibility: visible; opacity: 1; }
                    .reasoning-section { margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-radius: 8px; border: 1px solid #dee2e6; }
                    .reasoning-section h2 { color: #495057; margin-bottom: 15px; border-bottom: 2px solid #dee2e6; padding-bottom: 10px; }
                    .recommendations-section { margin: 30px 0; padding: 20px; background-color: #e8f5e8; border-radius: 8px; border: 1px solid #c3e6c3; }
                    .recommendations-section h2 { color: #2d5016; margin-bottom: 15px; border-bottom: 2px solid #c3e6c3; padding-bottom: 10px; }
                    .metric-recommendations { margin: 20px 0; padding: 15px; background-color: white; border-radius: 6px; border: 1px solid #e0e0e0; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
                    .metric-recommendations h3 { color: #333; margin-bottom: 10px; font-size: 1.1em; }
                    .metric-recommendations ul { margin: 10px 0; padding-left: 20px; }
                    .metric-recommendations li { margin: 5px 0; color: #555; line-height: 1.4; }
                    .reasoning-item { margin: 20px 0; padding: 15px; background-color: white; border-radius: 6px; border: 1px solid #e9ecef; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .reasoning-item h3 { color: #212529; margin-bottom: 15px; font-size: 1.1em; border-bottom: 1px solid #dee2e6; padding-bottom: 8px; }
                    .reasoning-title { font-weight: bold; color: #495057; margin: 10px 0 5px 0; }
                    .reasoning-text { color: #6c757d; line-height: 1.5; padding: 8px 0; border-left: 3px solid #007bff; padding-left: 15px; background-color: #f8f9fa; border-radius: 3px; }
                    .metrics-section { margin: 30px 0; }
                    .metrics-section h2 { color: #495057; margin-bottom: 20px; border-bottom: 2px solid #dee2e6; padding-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Code Guard Technical Report</h1>
                        <p>Comprehensive code analysis and quality metrics</p>
                    </div>
            """);

        // Summary section with file metrics
        html.append("<div class=\"summary\">");

        // Calculate aggregate metrics
        int totalLines = result.getFileResults().stream()
            .mapToInt(file -> (Integer) file.getMetrics().getOrDefault("linesOfCode", 0))
            .sum();

        int totalFunctions = result.getFileResults().stream()
            .mapToInt(file -> (Integer) file.getMetrics().getOrDefault("numberOfMethods", 0))
            .sum();

        double avgComplexity = result.getFileResults().stream()
            .mapToInt(file -> (Integer) file.getMetrics().getOrDefault("cyclomaticComplexity", 0))
            .average()
            .orElse(0.0);

        html.append(String.format("""
            <div class="metric-card">
                <div class="metric-value">%.1f</div>
                <div class="metric-label">Overall Score</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Total Files</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Total Lines</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Total Functions</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%.1f</div>
                <div class="metric-label">Avg Complexity</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">High Quality</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Critical Issues</div>
            </div>
            """, 
            result.getOverallScore(),
            result.getSummary().getTotalFiles(),
            totalLines,
            totalFunctions,
            avgComplexity,
            result.getSummary().getHighQualityFiles(),
            result.getSummary().getCriticalIssues()
        ));
        html.append("</div>");

        // Files table
        html.append("""
            <h2>File Analysis Results</h2>
            <table class="files-table">
                <thead>
                    <tr>
                        <th>Filename</th>
                        <th>Code Quality</th>
                        <th>Single Responsibility</th>
                        <th>Design Patterns</th>
                        <th>Security</th>
                        <th>Bug Detection</th>
                        <th>Final Score</th>
                        <th>Quality</th>
                    </tr>
                </thead>
                <tbody>
            """);

        for (FileAnalysisResult file : result.getFileResults()) {
            String qualityClass = switch (file.getQualityIndicator()) {
                case GREEN -> "quality-green";
                case YELLOW -> "quality-yellow";
                case RED -> "quality-red";
            };
            
            String sanitizedFileName = sanitizeFileName(file.getFilename());
            String detailsLink = String.format("analysis/%s-analysis.html", sanitizedFileName);
            
            html.append(String.format("""
                <tr>
                    <td><a href="%s" style="color: #007acc; text-decoration: none; font-weight: bold;">%s</a></td>
                    <td class="score tooltip">%.1f
                        <span class="tooltiptext">%s</span>
                    </td>
                    <td class="score tooltip">%.1f
                        <span class="tooltiptext">%s</span>
                    </td>
                    <td class="score tooltip">%.1f
                        <span class="tooltiptext">%s</span>
                    </td>
                    <td class="score tooltip">%.1f
                        <span class="tooltiptext">%s</span>
                    </td>
                    <td class="score tooltip">%.1f
                        <span class="tooltiptext">%s</span>
                    </td>
                    <td class="score">%.1f</td>
                    <td><span class="%s">%s</span></td>
                </tr>
                """,
                detailsLink,
                file.getFilename(),
                file.getCodeQuality(),
                escapeHtml(file.getCodeQualityReason() != null ? file.getCodeQualityReason() : "No detailed reasoning available"),
                file.getSolid(),
                escapeHtml(file.getSolidReason() != null ? file.getSolidReason() : "No detailed reasoning available"),
                file.getDesignPatterns(),
                escapeHtml(file.getDesignPatternsReason() != null ? file.getDesignPatternsReason() : "No detailed reasoning available"),
                file.getSecurity(),
                escapeHtml(file.getSecurityReason() != null ? file.getSecurityReason() : "No detailed reasoning available"),
                file.getBugDetection(),
                escapeHtml(file.getBugDetectionReason() != null ? file.getBugDetectionReason() : "No detailed reasoning available"),
                file.getFinalScore(),
                qualityClass,
                file.getQualityIndicator().getLabel()
            ));
        }

        html.append("</tbody></table>");

        // Simple message pointing to detailed analysis
        html.append("""
            <div class="reasoning-section">
                <h2>Detailed Analysis & Recommendations</h2>
                <p>For detailed analysis, reasoning, and specific recommendations for each file, click on the file name in the table above. 
                Each file has its own comprehensive analysis report with actionable insights.</p>
                <p><strong>Note:</strong> Hover over the scores in the table above to see brief explanations.</p>
            </div>
            """);

        html.append("</div>");

        // File Metrics Section
        html.append("""
            <div class="metrics-section">
                <h2>File Metrics Overview</h2>
                <table class="files-table">
                    <thead>
                        <tr>
                            <th>Filename</th>
                            <th>Lines of Code</th>
                            <th>Functions</th>
                            <th>Classes</th>
                            <th>Cyclomatic Complexity</th>
                            <th>Comment Ratio (%)</th>
                            <th>Complexity Level</th>
                        </tr>
                    </thead>
                    <tbody>
            """);

        for (FileAnalysisResult file : result.getFileResults()) {
            Map<String, Object> metrics = file.getMetrics();
            html.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%.1f</td>
                    <td>%s</td>
                </tr>
                """,
                escapeHtml(file.getFilename()),
                metrics.getOrDefault("linesOfCode", "N/A"),
                metrics.getOrDefault("numberOfMethods", "N/A"),
                metrics.getOrDefault("numberOfClasses", "N/A"),
                metrics.getOrDefault("cyclomaticComplexity", "N/A"),
                ((Number) metrics.getOrDefault("commentRatio", 0.0)).doubleValue(),
                metrics.getOrDefault("codeComplexity", "UNKNOWN")
            ));
        }

        html.append("</tbody></table></div>");

        // Recommendations
        if (result.getSummary().getRecommendations() != null && !result.getSummary().getRecommendations().isEmpty()) {
            html.append("<h2>Recommendations</h2><ul>");
            for (String recommendation : result.getSummary().getRecommendations()) {
                html.append("<li>").append(recommendation).append("</li>");
            }
            html.append("</ul>");
        }

        // Footer
        html.append(String.format("""
            <div class="timestamp">
                Report generated on %s by Code Guard v1.0.0
            </div>
            </div>
            </body>
            </html>
            """, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        return html.toString();
    }

    private String buildNonTechnicalHtmlReport(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Code Guard - Executive Summary</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 40px; padding-bottom: 20px; border-bottom: 2px solid #007acc; }
                    .header h1 { color: #007acc; margin: 0; font-size: 2.5em; }
                    .executive-summary { background: #f8f9fa; padding: 25px; border-radius: 8px; margin-bottom: 30px; }
                    .quality-score { text-align: center; margin: 30px 0; }
                    .score-circle { display: inline-block; width: 120px; height: 120px; border-radius: 50%; line-height: 120px; font-size: 2em; font-weight: bold; color: white; }
                    .score-excellent { background: #28a745; }
                    .score-good { background: #17a2b8; }
                    .score-fair { background: #ffc107; color: black; }
                    .score-poor { background: #dc3545; }
                    .key-findings { margin: 30px 0; }
                    .finding-item { background: white; border-left: 4px solid #007acc; padding: 15px; margin: 15px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                    .recommendations { background: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .recommendation-item { margin: 10px 0; padding: 10px; background: white; border-radius: 4px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Executive Summary</h1>
                        <p>Code Quality Assessment Report</p>
                    </div>
            """);

        // Executive Summary
        String qualityLevel = getQualityLevel(result.getOverallScore());
        String scoreClass = getScoreClass(result.getOverallScore());
        
        html.append("""
            <div class="executive-summary">
                <h2>Project Overview</h2>
                <p>This report provides a comprehensive assessment of your codebase quality based on industry standards and best practices. 
                The analysis covers code quality, adherence to Single Responsibility Principle, design patterns usage, security considerations, and bug detection.</p>
            </div>
            """);

        html.append(String.format("""
            <div class="quality-score">
                <div class="score-circle %s">%.0f</div>
                <h3>Overall Quality: %s</h3>
                <p>Based on analysis of %d files</p>
            </div>
            """,
            scoreClass,
            result.getOverallScore(),
            qualityLevel,
            result.getSummary().getTotalFiles()
        ));

        // Key Findings
        html.append("""
            <div class="key-findings">
                <h2>Key Findings</h2>
            """);

        html.append(String.format("""
            <div class="finding-item">
                <h4>Code Quality Distribution</h4>
                <p>• High Quality Files: %d<br>
                • Medium Quality Files: %d<br>
                • Files Needing Improvement: %d</p>
            </div>
            """,
            result.getSummary().getHighQualityFiles(),
            result.getSummary().getMediumQualityFiles(),
            result.getSummary().getLowQualityFiles()
        ));

        if (result.getSummary().getCriticalIssues() > 0) {
            html.append(String.format("""
                <div class="finding-item">
                    <h4>Critical Issues Identified</h4>
                    <p>%d critical issues require immediate attention to ensure code reliability and security.</p>
                </div>
                """,
                result.getSummary().getCriticalIssues()
            ));
        }

        html.append("</div>");

        // Recommendations
        if (result.getSummary().getRecommendations() != null && !result.getSummary().getRecommendations().isEmpty()) {
            html.append("""
                <div class="recommendations">
                    <h2>Strategic Recommendations</h2>
                """);
            
            for (String recommendation : result.getSummary().getRecommendations()) {
                html.append(String.format("""
                    <div class="recommendation-item">
                        <p>%s</p>
                    </div>
                    """, recommendation));
            }
            
            html.append("</div>");
        }

        // Footer
        html.append(String.format("""
            <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #666;">
                <p>Report generated on %s by Code Guard</p>
                <p><em>This report serves as Knowledge Transfer material for development teams</em></p>
            </div>
            </div>
            </body>
            </html>
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        return html.toString();
    }

    /**
     * Generates KT (Knowledge Transfer) documentation for new joiners in HTML format.
     * Each section is generated as a separate file and linked from index.html in the kt folder.
     */
    public void generateKTDocumentation(AnalysisResult result, String outputDir) throws IOException {
        Path ktDir = Paths.get(outputDir, "kt");
        Files.createDirectories(ktDir);
        OpenAIAnalysisService openAIService = new OpenAIAnalysisService();
        // Merge and summarize KT data for each section
        String mergedPurpose = result.getFileResults().stream()
            .map(FileAnalysisResult::getKtPurpose)
            .filter(s -> s != null && !s.isBlank())
            .reduce("", (a, b) -> a + "\n" + b);
        String mergedDesign = result.getFileResults().stream()
            .map(FileAnalysisResult::getKtDesign)
            .filter(s -> s != null && !s.isBlank())
            .reduce("", (a, b) -> a + "\n" + b);
        String mergedModules = result.getFileResults().stream()
            .map(FileAnalysisResult::getKtModules)
            .filter(s -> s != null && !s.isBlank())
            .reduce("", (a, b) -> a + "\n" + b);
        // Summarize using OpenAI
        String summarizedPurpose;
        String summarizedDesign;
        String summarizedModules;
        
        try {
            summarizedPurpose = mergedPurpose.isBlank() ? "No data from OpenAI." : openAIService.summarizePurpose(mergedPurpose);
        } catch (Exception e) {
            logger.warn("Failed to summarize purpose data: {}", e.getMessage());
            summarizedPurpose = "Unable to generate purpose summary due to API error.";
        }
        
        try {
            summarizedDesign = mergedDesign.isBlank() ? "No data from OpenAI." : openAIService.summarizeDesign(mergedDesign);
        } catch (Exception e) {
            logger.warn("Failed to summarize design data: {}", e.getMessage());
            summarizedDesign = "Unable to generate design summary due to API error.";
        }
        
        try {
            summarizedModules = mergedModules.isBlank() ? "No data from OpenAI." : openAIService.summarizeModules(mergedModules);
        } catch (Exception e) {
            logger.warn("Failed to summarize modules data: {}", e.getMessage());
            summarizedModules = "Unable to generate modules summary due to API error.";
        }
        // Generate KT HTML files using summaries
        Files.writeString(ktDir.resolve("purpose.html"), buildKTPurposeHtml(summarizedPurpose));
        Files.writeString(ktDir.resolve("design.html"), buildKTDesignHtml(summarizedDesign));
        Files.writeString(ktDir.resolve("modules.html"), buildKTModulesHtml(summarizedModules));
        Files.writeString(ktDir.resolve("index.html"), buildKTIndexHtml());
    }

    private String getKTCommonCss() {
        return "<style>" +
            "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }" +
            ".container { max-width: 900px; margin: 40px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            "h1, h2 { color: #007acc; }" +
            "ul { margin-left: 20px; }" +
            ".section { margin-bottom: 32px; }" +
            ".nav { margin-bottom: 24px; }" +
            ".nav a { margin-right: 16px; color: #007acc; text-decoration: none; font-weight: bold; }" +
            ".nav a:hover { text-decoration: underline; }" +
            "</style>";
    }

    private String buildKTIndexHtml() {
        return """
        <!DOCTYPE html>
        <html lang=\"en\">
        <head>
            <meta charset=\"UTF-8\">
            <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
            <title>Knowledge Transfer - Code Guard</title>
            """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>Knowledge Transfer (KT) Documentation</h1>
            <ul class=\"nav\" style=\"list-style-type:none; padding:0;\">
                <li style=\"margin-bottom:16px;\"><a href=\"purpose.html\">Purpose & Goals</a></li>
                <li style=\"margin-bottom:16px;\"><a href=\"design.html\">System Design</a></li>
                <li style=\"margin-bottom:16px;\"><a href=\"modules.html\">Modules & Business Logic</a></li>
            </ul>
            <div class=\"section\">
                <p>Welcome! Use the navigation above to explore each KT section. All documents are designed for new joiners and onboarding.</p>
            </div>
        </div>
        </body>
        </html>
        """;
    }

    private String buildKTPurposeHtml(String summary) {
        String html = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Purpose & Goals</title>
        %s
        </head>
        <body>
        <div class='container'>
            <h1>Purpose, Key Goals, and Stakeholders</h1>
            <p>%s</p>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """, getKTCommonCss(), summary);
        return html;
    }

    private String buildKTDesignHtml(String summary) {
        String html = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - System Design</title>
        %s
        </head>
        <body>
        <div class='container'>
            <h1>High-Level System Design, Tech Stack, and Component Interactions</h1>
            <p>%s</p>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """, getKTCommonCss(), summary);
        return html;
    }

    private String buildKTModulesHtml(String summary) {
        String html = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Modules & Business Logic</title>
        %s
        </head>
        <body>
        <div class='container'>
            <h1>Functional Overview of Each Module and Key Business Logic</h1>
            <p>%s</p>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """, getKTCommonCss(), summary);
        return html;
    }


    private String getQualityLevel(double score) {
        if (score >= 90) return "Excellent";
        if (score >= 80) return "Good";
        if (score >= 70) return "Fair";
        return "Needs Improvement";
    }

    private String getScoreClass(double score) {
        if (score >= 90) return "score-excellent";
        if (score >= 80) return "score-good";
        if (score >= 70) return "score-fair";
        return "score-poor";
    }

    /**
     * Escapes HTML special characters to prevent XSS and formatting issues
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Generates detailed analysis files for each code file
     */
    private void generateDetailedAnalysisFiles(AnalysisResult result, Path outputPath) throws IOException {
        // Create a subdirectory for detailed analysis files
        Path analysisDir = outputPath.resolve("analysis");
        Files.createDirectories(analysisDir);
        
        for (FileAnalysisResult file : result.getFileResults()) {
            String fileName = sanitizeFileName(file.getFilename()) + "-analysis.html";
            Path analysisFile = analysisDir.resolve(fileName);
            String htmlContent = buildDetailedFileAnalysisHtml(file);
            Files.writeString(analysisFile, htmlContent);
        }
        
        logger.info("Generated {} detailed analysis files in: {}", result.getFileResults().size(), analysisDir);
    }

    /**
     * Sanitizes filename for use in HTML file names
     */
    private String sanitizeFileName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("\\.(java|js|py|ts|cpp|c|h)$", "");
    }

    /**
     * Builds detailed analysis HTML for a single file
     */
    private String buildDetailedFileAnalysisHtml(FileAnalysisResult file) {
        StringBuilder html = new StringBuilder();
        
        // HTML Header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>").append(escapeHtml(file.getFilename())).append(" - Detailed Analysis</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n");
        html.append(".container { max-width: 1000px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append(".header { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #007acc; }\n");
        html.append(".header h1 { color: #007acc; margin: 0; font-size: 2.2em; }\n");
        html.append(".back-link { display: inline-block; margin-bottom: 20px; color: #007acc; text-decoration: none; font-weight: bold; }\n");
        html.append(".back-link:hover { text-decoration: underline; }\n");
        html.append(".summary-card { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 30px; border-left: 4px solid #007acc; }\n");
        html.append(".score-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin: 20px 0; }\n");
        html.append(".score-item { background: white; padding: 15px; border-radius: 6px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        html.append(".score-value { font-size: 1.8em; font-weight: bold; color: #007acc; }\n");
        html.append(".score-label { color: #666; margin-top: 5px; font-size: 0.9em; }\n");
        html.append(".analysis-section { margin: 30px 0; padding: 20px; background: #fafafa; border-radius: 8px; border: 1px solid #e0e0e0; }\n");
        html.append(".analysis-section h2 { color: #333; margin-bottom: 15px; border-bottom: 2px solid #007acc; padding-bottom: 8px; }\n");
        html.append(".reasoning-text { color: #555; line-height: 1.6; margin-bottom: 15px; padding: 15px; background: white; border-radius: 6px; border-left: 3px solid #007acc; }\n");
        html.append(".recommendations { margin-top: 20px; }\n");
        html.append(".recommendations h3 { color: #007acc; margin-bottom: 10px; }\n");
        html.append(".recommendations ul { margin: 0; padding-left: 25px; }\n");
        html.append(".recommendations li { margin: 8px 0; color: #555; line-height: 1.5; display: flex; justify-content: space-between; align-items: flex-start; }\n");
        html.append(".recommendation-text { flex: 1; margin-right: 10px; }\n");
        html.append(".fix-button { background: linear-gradient(45deg, #007acc, #005999); color: white; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 0.8em; font-weight: bold; transition: all 0.3s ease; white-space: nowrap; }\n");
        html.append(".fix-button:hover { background: linear-gradient(45deg, #005999, #003d73); transform: translateY(-1px); box-shadow: 0 2px 4px rgba(0,0,0,0.2); }\n");
        html.append(".fix-button:active { transform: translateY(0); }\n");
        html.append(".fix-button:disabled { background: #ccc; cursor: not-allowed; transform: none; }\n");
        html.append(".fix-status { margin-left: 5px; font-size: 0.8em; }\n");
        html.append(".fix-success { color: #28a745; }\n");
        html.append(".fix-error { color: #dc3545; }\n");
        html.append(".fix-loading { color: #ffc107; }\n");
        html.append(".metrics-section { margin: 30px 0; }\n");
        html.append(".metrics-table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n");
        html.append(".metrics-table th, .metrics-table td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }\n");
        html.append(".metrics-table th { background-color: #007acc; color: white; }\n");
        html.append(".quality-indicator { padding: 5px 10px; border-radius: 4px; color: white; font-weight: bold; text-align: center; }\n");
        html.append(".quality-green { background-color: #28a745; }\n");
        html.append(".quality-yellow { background-color: #ffc107; color: black; }\n");
        html.append(".quality-red { background-color: #dc3545; }\n");
        html.append(".final-score { font-size: 2.5em; font-weight: bold; color: #007acc; text-align: center; margin: 20px 0; }\n");
        html.append("</style>\n");
        html.append("<script>\n");
        html.append("function fixWithAI(category, recommendation, button) {\n");
        html.append("    const originalText = button.textContent;\n");
        html.append("    button.disabled = true;\n");
        html.append("    button.textContent = 'Fixing...';\n");
        html.append("    button.style.background = '#ffc107';\n");
        html.append("    \n");
        html.append("    // Create status indicator\n");
        html.append("    const statusSpan = document.createElement('span');\n");
        html.append("    statusSpan.className = 'fix-status fix-loading';\n");
        html.append("    statusSpan.textContent = ' ⏳';\n");
        html.append("    button.parentNode.appendChild(statusSpan);\n");
        html.append("    \n");
        html.append("    // Simulate AI processing (replace with actual API call)\n");
        html.append("    setTimeout(() => {\n");
        html.append("        // For demo purposes, randomly succeed or fail\n");
        html.append("        const success = Math.random() > 0.3;\n");
        html.append("        \n");
        html.append("        if (success) {\n");
        html.append("            button.textContent = 'Fixed ✓';\n");
        html.append("            button.style.background = '#28a745';\n");
        html.append("            statusSpan.className = 'fix-status fix-success';\n");
        html.append("            statusSpan.textContent = ' Applied';\n");
        html.append("            \n");
        html.append("            // Show success message\n");
        html.append("            showNotification('Fix applied successfully for: ' + category, 'success');\n");
        html.append("        } else {\n");
        html.append("            button.textContent = 'Retry';\n");
        html.append("            button.style.background = '#dc3545';\n");
        html.append("            button.disabled = false;\n");
        html.append("            statusSpan.className = 'fix-status fix-error';\n");
        html.append("            statusSpan.textContent = ' Failed';\n");
        html.append("            \n");
        html.append("            // Show error message\n");
        html.append("            showNotification('Failed to apply fix. Please try again.', 'error');\n");
        html.append("        }\n");
        html.append("    }, 2000 + Math.random() * 3000); // 2-5 second delay\n");
        html.append("}\n");
        html.append("\n");
        html.append("function showNotification(message, type) {\n");
        html.append("    const notification = document.createElement('div');\n");
        html.append("    notification.style.cssText = `\n");
        html.append("        position: fixed; top: 20px; right: 20px; z-index: 1000;\n");
        html.append("        padding: 12px 20px; border-radius: 6px; color: white;\n");
        html.append("        font-weight: bold; box-shadow: 0 4px 12px rgba(0,0,0,0.3);\n");
        html.append("        transform: translateX(100%); transition: transform 0.3s ease;\n");
        html.append("        background: ${type === 'success' ? '#28a745' : '#dc3545'};\n");
        html.append("    `;\n");
        html.append("    notification.textContent = message;\n");
        html.append("    document.body.appendChild(notification);\n");
        html.append("    \n");
        html.append("    // Slide in\n");
        html.append("    setTimeout(() => notification.style.transform = 'translateX(0)', 100);\n");
        html.append("    \n");
        html.append("    // Slide out and remove\n");
        html.append("    setTimeout(() => {\n");
        html.append("        notification.style.transform = 'translateX(100%)';\n");
        html.append("        setTimeout(() => document.body.removeChild(notification), 300);\n");
        html.append("    }, 4000);\n");
        html.append("}\n");
        html.append("</script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"container\">\n");
        html.append("<a href=\"../technical-report.html\" class=\"back-link\">← Back to Technical Report</a>\n");
        
        // Header
        html.append("<div class=\"header\">\n");
        html.append("<h1>").append(escapeHtml(file.getFilename())).append("</h1>\n");
        html.append("<p>Detailed Code Analysis Results</p>\n");
        html.append("</div>\n");

        // Summary card with overall score
        html.append("<div class=\"summary-card\">\n");
        html.append("<div class=\"final-score\">").append(String.format("%.1f", file.getFinalScore())).append("</div>\n");
        html.append("<div style=\"text-align: center;\">\n");
        
        String qualityClass = switch (file.getQualityIndicator()) {
            case GREEN -> "quality-green";
            case YELLOW -> "quality-yellow";  
            case RED -> "quality-red";
        };
        
        html.append("<span class=\"quality-indicator ").append(qualityClass).append("\">");
        html.append(file.getQualityIndicator().getLabel());
        html.append("</span></div></div>\n");

        // Score breakdown
        html.append("<div class=\"score-grid\">\n");
        html.append("<div class=\"score-item\">\n");
        html.append("<div class=\"score-value\">").append(String.format("%.1f", file.getCodeQuality())).append("</div>\n");
        html.append("<div class=\"score-label\">Code Quality</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"score-item\">\n");
        html.append("<div class=\"score-value\">").append(String.format("%.1f", file.getSolid())).append("</div>\n");
        html.append("<div class=\"score-label\">SOLID Principles</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"score-item\">\n");
        html.append("<div class=\"score-value\">").append(String.format("%.1f", file.getDesignPatterns())).append("</div>\n");
        html.append("<div class=\"score-label\">Design Patterns</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"score-item\">\n");
        html.append("<div class=\"score-value\">").append(String.format("%.1f", file.getSecurity())).append("</div>\n");
        html.append("<div class=\"score-label\">Security</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"score-item\">\n");
        html.append("<div class=\"score-value\">").append(String.format("%.1f", file.getBugDetection())).append("</div>\n");
        html.append("<div class=\"score-label\">Bug Detection</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        // Detailed analysis sections
        addAnalysisSection(html, "Code Quality Analysis", file.getCodeQuality(), file.getCodeQualityReason(), file.getCodeQualityRecommendations());
        addAnalysisSection(html, "SOLID Principles Analysis", file.getSolid(), file.getSolidReason(), file.getSolidRecommendations());
        addAnalysisSection(html, "Design Patterns Analysis", file.getDesignPatterns(), file.getDesignPatternsReason(), file.getDesignPatternsRecommendations());
        addAnalysisSection(html, "Security Analysis", file.getSecurity(), file.getSecurityReason(), file.getSecurityRecommendations());
        addAnalysisSection(html, "Bug Detection Analysis", file.getBugDetection(), file.getBugDetectionReason(), file.getBugDetectionRecommendations());

        // File metrics
        if (file.getMetrics() != null && !file.getMetrics().isEmpty()) {
            html.append("<div class=\"metrics-section\">\n");
            html.append("<h2>File Metrics</h2>\n");
            html.append("<table class=\"metrics-table\">\n");
            html.append("<tr><th>Metric</th><th>Value</th></tr>\n");
            
            file.getMetrics().forEach((key, value) -> {
                String displayKey = key.replaceAll("([A-Z])", " $1").toLowerCase();
                displayKey = displayKey.substring(0, 1).toUpperCase() + displayKey.substring(1);
                html.append("<tr><td>").append(displayKey).append("</td><td>").append(value).append("</td></tr>\n");
            });
            
            html.append("</table></div>\n");
        }

        // Footer
        html.append("<div style=\"text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #666;\">\n");
        html.append("<p>Analysis generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(" by Code Guard</p>\n");
        html.append("<a href=\"../technical-report.html\" class=\"back-link\">← Back to Technical Report</a>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Helper method to add an analysis section to the detailed HTML
     */
    private void addAnalysisSection(StringBuilder html, String title, double score, String reason, List<String> recommendations) {
        html.append("<div class=\"analysis-section\">\n");
        html.append("<h2>").append(title).append(" (").append(String.format("%.1f", score)).append("/100)</h2>\n");
        html.append("<div class=\"reasoning-text\">").append(escapeHtml(reason != null ? reason : "No detailed reasoning available")).append("</div>\n");
        
        if (recommendations != null && !recommendations.isEmpty()) {
            html.append("<div class=\"recommendations\">\n");
            html.append("<h3>Recommendations</h3>\n");
            html.append("<ul>\n");
            for (int i = 0; i < recommendations.size(); i++) {
                String rec = recommendations.get(i);
                String buttonId = String.format("fix-%s-%d", 
                    title.toLowerCase().replaceAll("[^a-z0-9]", "-"), i);
                
                html.append("<li>");
                html.append("<span class=\"recommendation-text\">").append(escapeHtml(rec)).append("</span>");
                html.append(String.format(
                    "<button class=\"fix-button\" id=\"%s\" " +
                    "onclick=\"fixWithAI('%s', '%s', this)\" " +
                    "data-category=\"%s\" data-recommendation=\"%s\">" +
                    "Fix with AI</button>",
                    buttonId,
                    title.replace("'", "\\'"),
                    escapeHtml(rec).replace("'", "\\'"),
                    title,
                    escapeHtml(rec)
                ));
                html.append("</li>\n");
            }
            html.append("</ul></div>\n");
        }
        
        html.append("</div>\n");
    }
}
