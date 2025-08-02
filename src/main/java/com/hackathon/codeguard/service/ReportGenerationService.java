package com.hackathon.codeguard.service;

import com.hackathon.codeguard.model.AnalysisResult;
import com.hackathon.codeguard.model.FileAnalysisResult;
import com.hackathon.codeguard.model.ReportType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Code Guard Technical Report</h1>
                        <p>Comprehensive code analysis and quality metrics</p>
                    </div>
            """);

        // Summary section
        html.append("<div class=\"summary\">");
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
                <div class="metric-label">High Quality</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Critical Issues</div>
            </div>
            """, 
            result.getOverallScore(),
            result.getSummary().getTotalFiles(),
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
                        <th>SOLID</th>
                        <th>Design Patterns</th>
                        <th>Clean Code</th>
                        <th>Security</th>
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
            
            html.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td class="score">%.1f</td>
                    <td class="score">%.1f</td>
                    <td class="score">%.1f</td>
                    <td class="score">%.1f</td>
                    <td class="score">%.1f</td>
                    <td class="score">%.1f</td>
                    <td><span class="%s">%s</span></td>
                </tr>
                """,
                file.getFilename(),
                file.getCodeQuality(),
                file.getSolid(),
                file.getDesignPatterns(),
                file.getCleanCode(),
                file.getSecurity(),
                file.getFinalScore(),
                qualityClass,
                file.getQualityIndicator().getLabel()
            ));
        }

        html.append("</tbody></table>");

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
                The analysis covers code quality, adherence to SOLID principles, design patterns usage, clean code practices, and security considerations.</p>
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
        // Generate each KT section as a separate file, passing AnalysisResult for file-specific details
        Files.writeString(ktDir.resolve("purpose.html"), buildKTPurposeHtml(result));
        Files.writeString(ktDir.resolve("design.html"), buildKTDesignHtml(result));
        Files.writeString(ktDir.resolve("modules.html"), buildKTModulesHtml(result));
        Files.writeString(ktDir.resolve("dataflow.html"), buildKTDataFlowHtml(result));
        Files.writeString(ktDir.resolve("execution.html"), buildKTExecutionHtml(result));
        Files.writeString(ktDir.resolve("tools.html"), buildKTToolsHtml(result));
        // Generate index.html linking to all KT section files
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
                <li style=\"margin-bottom:16px;\"><a href=\"dataflow.html\">Data Flow</a></li>
                <li style=\"margin-bottom:16px;\"><a href=\"execution.html\">Report Execution</a></li>
                <li style=\"margin-bottom:16px;\"><a href=\"tools.html\">Tools & Tech Stack</a></li>
            </ul>
            <div class=\"section\">
                <p>Welcome! Use the navigation above to explore each KT section. All documents are designed for new joiners and onboarding.</p>
            </div>
        </div>
        </body>
        </html>
        """;
    }

    private String buildKTPurposeHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Purpose & Goals</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class='container'>
            <h1>Purpose, Key Goals, and Stakeholders</h1>
            <ul>
        """);
        Map<String, List<String>> purposeToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String purpose = file.getKtPurpose() != null ? file.getKtPurpose().trim() : "No data from OpenAI.";
            purposeToFiles.computeIfAbsent(purpose, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : purposeToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
    }

    private String buildKTDesignHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - System Design</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>High-Level System Design, Tech Stack, and Component Interactions</h1>
            <ul>
        """);
        Map<String, List<String>> designToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String design = file.getKtDesign() != null ? file.getKtDesign().trim() : "No data from OpenAI.";
            designToFiles.computeIfAbsent(design, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : designToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
    }

    private String buildKTModulesHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Modules & Business Logic</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>Functional Overview of Each Module and Key Business Logic</h1>
            <ul>
        """);
        Map<String, List<String>> modulesToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String modules = file.getKtModules() != null ? file.getKtModules().trim() : "No data from OpenAI.";
            modulesToFiles.computeIfAbsent(modules, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : modulesToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
    }

    private String buildKTDataFlowHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Data Flow</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>Data Flow</h1>
            <ul>
        """);
        Map<String, List<String>> dataFlowToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String dataFlow = file.getKtDataFlow() != null ? file.getKtDataFlow().trim() : "No data from OpenAI.";
            dataFlowToFiles.computeIfAbsent(dataFlow, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : dataFlowToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
    }

    private String buildKTExecutionHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Report Execution</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>Step-by-Step Report Execution</h1>
            <ul>
        """);
        Map<String, List<String>> executionToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String execution = file.getKtExecution() != null ? file.getKtExecution().trim() : "No data from OpenAI.";
            executionToFiles.computeIfAbsent(execution, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : executionToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
    }

    private String buildKTToolsHtml(AnalysisResult result) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>KT - Tools & Tech Stack</title>
        """ + getKTCommonCss() + """
        </head>
        <body>
        <div class=\"container\">
            <h1>Tools, Libraries, External Services, and Version Constraints</h1>
            <ul>
        """);
        Map<String, List<String>> toolsToFiles = new LinkedHashMap<>();
        for (FileAnalysisResult file : result.getFileResults()) {
            String tools = file.getKtTools() != null ? file.getKtTools().trim() : "No data from OpenAI.";
            toolsToFiles.computeIfAbsent(tools, k -> new ArrayList<>()).add(file.getFilename());
        }
        for (Map.Entry<String, List<String>> entry : toolsToFiles.entrySet()) {
            html.append("<li>")
                .append(entry.getKey())
                .append("<br><em>Files: ")
                .append(String.join(", ", entry.getValue()))
                .append("</em></li>");
        }
        html.append("""
            </ul>
            <a href='index.html'>Back to KT Index</a>
        </div>
        </body>
        </html>
        """);
        return html.toString();
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
}
