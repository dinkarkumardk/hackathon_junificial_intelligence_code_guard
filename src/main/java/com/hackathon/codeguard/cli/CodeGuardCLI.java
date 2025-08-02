package com.hackathon.codeguard.cli;

import com.hackathon.codeguard.service.CodeAnalysisService;
import com.hackathon.codeguard.service.ReportGenerationService;
import com.hackathon.codeguard.model.AnalysisResult;
import com.hackathon.codeguard.model.ReportType;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command Line Interface for Code Guard tool
 */
@Command(
    name = "code-guard",
    description = "Code analysis tool that scores code files and generates HTML reports",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class CodeGuardCLI implements Callable<Integer> {

    @Parameters(
        paramLabel = "FILES",
        description = "Code files or directories to analyze"
    )
    private List<File> files;

    @Option(
        names = {"-o", "--output"},
        description = "Output directory for generated reports (default: ./reports)"
    )
    private String outputDir = "./reports";

    @Option(
        names = {"-t", "--threshold"},
        description = "Minimum quality score threshold (default: 70)"
    )
    private int threshold = 70;

    @Option(
        names = {"-m", "--mode"},
        description = "Analysis mode: ${COMPLETION-CANDIDATES} (default: standard)"
    )
    private AnalysisMode mode = AnalysisMode.STANDARD;

    @Option(
        names = {"-r", "--report-type"},
        description = "Report type: ${COMPLETION-CANDIDATES} (default: both)"
    )
    private ReportType reportType = ReportType.BOTH;

    @Option(
        names = {"-f", "--format"},
        description = "Output format: html, json (default: html)"
    )
    private String format = "html";

    @Option(
        names = {"--scan"},
        description = "Scan directory recursively for code files"
    )
    private String scanDirectory;

    public enum AnalysisMode {
        STANDARD,
        QA_AUTOMATION,
        DEVOPS_TESTING,
        DEVELOPER_REVIEW
    }

    private final CodeAnalysisService analysisService;
    private final ReportGenerationService reportService;

    public CodeGuardCLI() {
        this.analysisService = new CodeAnalysisService();
        this.reportService = new ReportGenerationService();
    }

    @Override
    public Integer call() throws Exception {
        try {
            System.out.println("Starting Code Guard analysis...");
            
            // Determine files to analyze
            List<Path> filesToAnalyze = determineFilesToAnalyze();
            
            if (filesToAnalyze.isEmpty()) {
                System.err.println("No code files found to analyze");
                return 1;
            }

            System.out.println("Analyzing " + filesToAnalyze.size() + " files...");
            
            // Perform analysis
            AnalysisResult result = analysisService.analyzeFiles(filesToAnalyze, mode);
            
            // Check threshold
            if (result.getOverallScore() < threshold) {
                System.err.println("Quality gate failed. Score: " + result.getOverallScore() + 
                                 " < threshold: " + threshold);
                
                if (mode == AnalysisMode.QA_AUTOMATION || mode == AnalysisMode.DEVOPS_TESTING) {
                    return 1; // Fail for automation modes
                }
            }

            // Generate reports
            reportService.generateReports(result, outputDir, reportType, format);
            
            System.out.println("Analysis complete. Reports generated in: " + outputDir);
            System.out.println("Overall Score: " + result.getOverallScore());
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private List<Path> determineFilesToAnalyze() {
        // Implementation will scan files/directories and filter code files
        List<Path> result = new java.util.ArrayList<>();
        java.util.function.Predicate<Path> codeFileFilter = path -> {
            String name = path.getFileName().toString().toLowerCase();
            return name.endsWith(".java") || name.endsWith(".ts");
        };

        if (scanDirectory != null && !scanDirectory.isEmpty()) {
            Path scanPath = new File(scanDirectory).toPath();
            if (java.nio.file.Files.isDirectory(scanPath)) {
            try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(scanPath)) {
                stream.filter(java.nio.file.Files::isRegularFile)
                  .filter(codeFileFilter)
                  .forEach(result::add);
            } catch (Exception e) {
                System.err.println("Error scanning directory: " + e.getMessage());
            }
            }
        } else if (files != null) {
            for (File file : files) {
            if (file.isDirectory()) {
                try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(file.toPath())) {
                stream.filter(java.nio.file.Files::isRegularFile)
                      .filter(codeFileFilter)
                      .forEach(result::add);
                } catch (Exception e) {
                System.err.println("Error scanning directory: " + e.getMessage());
                }
            } else if (file.isFile() && codeFileFilter.test(file.toPath())) {
                result.add(file.toPath());
            }
            }
        }
        return result;
    }
}
