package com.hackathon.codeguard.service.openai;

import com.hackathon.codeguard.cli.CodeGuardCLI.AnalysisMode;
import com.hackathon.codeguard.model.FileAnalysisResult;
import com.hackathon.codeguard.model.FileAnalysisResult.CodeIssue;
import com.hackathon.codeguard.model.ScoreWithReason;
import com.hackathon.codeguard.service.FileProcessingService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing code using OpenAI APIs
 */
public class OpenAIAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIAnalysisService.class);
    
    private final OpenAiService openAiService;
    private final FileProcessingService fileService;
    private final ObjectMapper objectMapper;
    
    // OpenAI Configuration
    private static final String MODEL = "gpt-4";
    private static final int MAX_TOKENS = 2000;
    private static final double TEMPERATURE = 0.1; // Low temperature for consistent results
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    public OpenAIAnalysisService() {
        // Initialize OpenAI service with API key from environment variable
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.fileService = new FileProcessingService();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes a single code file using OpenAI API
     */
    public FileAnalysisResult analyzeCodeFile(Path filePath, String fileContent, AnalysisMode mode, boolean ktEnabled) throws Exception {
        logger.debug("Analyzing file with OpenAI: {}", filePath);
        
        try {
            FileAnalysisResult result = new FileAnalysisResult(
                filePath.getFileName().toString(),
                filePath.toString()
            );

            // Determine programming language
            String language = fileService.determineProgrammingLanguage(filePath);
            
            // Extract basic file metrics first
            Map<String, Object> fileMetrics = extractMetrics(fileContent, language);
            result.setMetrics(fileMetrics);
            
            // Log file information
            logger.info("Analyzing file: {} ({} lines, {} language)", 
                filePath.getFileName(), 
                fileMetrics.getOrDefault("linesOfCode", "unknown"),
                language);
            
            // Analyze different aspects with individual exception handling
            ScoreWithReason codeQualityResult = analyzeCodeQuality(fileContent, language);
            result.setCodeQuality(codeQualityResult.getScore());
            result.setCodeQualityReason(codeQualityResult.getReason());
            result.setCodeQualityRecommendations(codeQualityResult.getRecommendations());
            
            ScoreWithReason srpResult = analyzeSingleResponsibilityPrinciple(fileContent, language);
            result.setSolid(srpResult.getScore());
            result.setSolidReason(srpResult.getReason());
            result.setSolidRecommendations(srpResult.getRecommendations());
            
            ScoreWithReason designPatternsResult = analyzeDesignPatterns(fileContent, language);
            result.setDesignPatterns(designPatternsResult.getScore());
            result.setDesignPatternsReason(designPatternsResult.getReason());
            result.setDesignPatternsRecommendations(designPatternsResult.getRecommendations());
            
            ScoreWithReason securityResult = analyzeSecurity(fileContent, language);
            result.setSecurity(securityResult.getScore());
            result.setSecurityReason(securityResult.getReason());
            result.setSecurityRecommendations(securityResult.getRecommendations());
            
            ScoreWithReason bugDetectionResult = analyzeBugDetection(fileContent, language);
            result.setBugDetection(bugDetectionResult.getScore());
            result.setBugDetectionReason(bugDetectionResult.getReason());
            result.setBugDetectionRecommendations(bugDetectionResult.getRecommendations());
            
            // Calculate final score
            result.calculateFinalScore();
            
            // Get issues and suggestions (these have their own exception handling)
            result.setIssues(identifyIssues(fileContent, language));
            result.setSuggestions(generateSuggestions(fileContent, language, mode));
            
            // Generate KT (Knowledge Transfer) data if enabled
            if (ktEnabled) {
                try {
                    result.setKtPurpose(generateKTPurpose(fileContent, language, filePath));
                    result.setKtDesign(generateKTDesign(fileContent, language, filePath));
                    result.setKtModules(generateKTModules(fileContent, language, filePath));
                    logger.debug("KT data generated for {}", filePath.getFileName());
                } catch (Exception e) {
                    logger.warn("Failed to generate KT data for {}: {}", filePath.getFileName(), e.getMessage());
                    result.setKtPurpose("Unable to generate KT purpose due to API error");
                    result.setKtDesign("Unable to generate KT design due to API error");
                    result.setKtModules("Unable to generate KT modules due to API error");
                }
            }
            
            // Log analysis completion with metrics
            logger.info("Analysis completed for {}: Final Score = {}, Lines = {}, Functions = {}, CC = {}", 
                filePath.getFileName(),
                result.getFinalScore(),
                fileMetrics.getOrDefault("linesOfCode", "N/A"),
                fileMetrics.getOrDefault("numberOfMethods", "N/A"),
                fileMetrics.getOrDefault("cyclomaticComplexity", "N/A"));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Critical error analyzing file {}: {}", filePath.getFileName(), e.getMessage());
            
            // Create a fallback result with basic information
            FileAnalysisResult fallbackResult = new FileAnalysisResult(
                filePath.getFileName().toString(),
                filePath.toString()
            );
            
            // Set default scores and error messages
            fallbackResult.setCodeQuality(0.0);
            fallbackResult.setCodeQualityReason("Analysis failed due to API error: " + e.getMessage());
            fallbackResult.setSolid(0.0);
            fallbackResult.setSolidReason("Analysis failed due to API error: " + e.getMessage());
            fallbackResult.setDesignPatterns(0.0);
            fallbackResult.setDesignPatternsReason("Analysis failed due to API error: " + e.getMessage());
            fallbackResult.setSecurity(0.0);
            fallbackResult.setSecurityReason("Analysis failed due to API error: " + e.getMessage());
            fallbackResult.setBugDetection(0.0);
            fallbackResult.setBugDetectionReason("Analysis failed due to API error: " + e.getMessage());
            
            // Set basic metrics
            Map<String, Object> basicMetrics = new HashMap<>();
            basicMetrics.put("linesOfCode", fileContent.split("\n").length);
            basicMetrics.put("cyclomaticComplexity", 1);
            basicMetrics.put("numberOfMethods", 0);
            basicMetrics.put("numberOfClasses", 0);
            basicMetrics.put("commentRatio", 0.0);
            basicMetrics.put("codeComplexity", "UNKNOWN");
            fallbackResult.setMetrics(basicMetrics);
            
            fallbackResult.calculateFinalScore();
            fallbackResult.setIssues(new ArrayList<>());
            fallbackResult.setSuggestions(new ArrayList<>());
            
            return fallbackResult;
        }
    }

    private ScoreWithReason analyzeCodeQuality(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for overall quality including readability, maintainability, " +
            "and documentation. Provide a score from 0-100 where 100 is excellent quality.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 2-4 specific actionable improvements)\\n",
            language, code
        );
        
        try {
            return getScoreWithReasonFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to analyze code quality for {} code: {}", language, e.getMessage());
            return new ScoreWithReason(50.0, "Unable to analyze code quality due to API error: " + e.getMessage());
        }
    }

    private ScoreWithReason analyzeSingleResponsibilityPrinciple(String code, String language) throws Exception {
        String prompt = String.format(
            "Evaluate how well the following %s code follows the Single Responsibility Principle (SRP). " +
            "The SRP states that a class should have only one reason to change, meaning it should have only one job or responsibility. " +
            "Analyze if classes/functions are focused on a single responsibility or if they are doing too many things. " +
            "Return a score from 0-100 where 100 means excellent adherence to SRP.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 2-4 specific actionable improvements)\\n",
            language, code
        );
        
        try {
            return getScoreWithReasonFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to analyze SRP for {} code: {}", language, e.getMessage());
            return new ScoreWithReason(50.0, "Unable to analyze Single Responsibility Principle due to API error: " + e.getMessage());
        }
    }

    private ScoreWithReason analyzeDesignPatterns(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for proper use of design patterns and architectural decisions. " +
            "Consider if appropriate patterns are used and if they're implemented correctly. " +
            "Return a score from 0-100.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 2-4 specific actionable improvements)\\n",
            language, code
        );
        
        try {
            return getScoreWithReasonFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to analyze design patterns for {} code: {}", language, e.getMessage());
            return new ScoreWithReason(50.0, "Unable to analyze design patterns due to API error: " + e.getMessage());
        }
    }

    private ScoreWithReason analyzeSecurity(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for security vulnerabilities and best practices. " +
            "Look for common security issues like injection flaws, insecure data handling, etc. " +
            "Return a score from 0-100 where 100 is very secure.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 2-4 specific actionable improvements)\\n",
            language, code
        );
        
        try {
            return getScoreWithReasonFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to analyze security for {} code: {}", language, e.getMessage());
            return new ScoreWithReason(50.0, "Unable to analyze security due to API error: " + e.getMessage());
        }
    }

    private ScoreWithReason analyzeBugDetection(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for potential bugs, logical errors, and runtime issues. " +
            "Look for common programming mistakes such as: null pointer exceptions, array bounds errors, " +
            "infinite loops, incorrect logic conditions, resource leaks, race conditions, and other potential bugs. " +
            "Return a score from 0-100 where 100 means bug-free code and 0 means many potential bugs.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 2-4 specific actionable improvements)\\n",
            language, code
        );
        
        try {
            return getScoreWithReasonFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to analyze bugs for {} code: {}", language, e.getMessage());
            return new ScoreWithReason(50.0, "Unable to analyze bugs due to API error: " + e.getMessage());
        }
    }

    private List<CodeIssue> identifyIssues(String code, String language) throws Exception {
        String prompt = String.format(
            "Identify specific issues in the following %s code. For each issue, provide:\\n" +
            "- Severity (CRITICAL, HIGH, MEDIUM, LOW)\\n" +
            "- Type (e.g., Security, Performance, Maintainability)\\n" +
            "- Description (concise, 1-2 sentences)\\n" +
            "- Line number (if applicable)\\n" +
            "- Suggestion for fix (concise, 1-2 sentences)\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON array with objects containing: severity, type, description, lineNumber, suggestion",
            language, code
        );
        
        try {
            String response = getResponseFromOpenAI(prompt);
            return parseIssuesFromResponse(response);
        } catch (Exception e) {
            logger.warn("Failed to identify issues for {} code: {}", language, e.getMessage());
            return new ArrayList<>(); // Return empty list if identification fails
        }
    }

    private List<String> generateSuggestions(String code, String language, AnalysisMode mode) throws Exception {
        String modeContext = switch (mode) {
            case QA_AUTOMATION -> "Focus on testability and quality assurance aspects.";
            case DEVOPS_TESTING -> "Focus on deployment readiness and operational concerns.";
            case DEVELOPER_REVIEW -> "Focus on code review and improvement suggestions.";
            default -> "Provide general improvement suggestions.";
        };
        
        String prompt = String.format(
            "Provide specific suggestions to improve the following %s code. %s\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return suggestions as a JSON array of strings. Keep each suggestion concise (maximum 1-2 sentences).",
            language, modeContext, code
        );
        
        try {
            String response = getResponseFromOpenAI(prompt);
            return parseSuggestionsFromResponse(response);
        } catch (Exception e) {
            logger.warn("Failed to generate suggestions for {} code: {}", language, e.getMessage());
            return new ArrayList<>(); // Return empty list if generation fails
        }
    }

    private Map<String, Object> extractMetrics(String code, String language) throws Exception {
        String prompt = String.format(
            "Extract detailed code metrics from the following %s code. Analyze and provide:\\n" +
            "- linesOfCode: Total number of lines (integer)\\n" +
            "- cyclomaticComplexity: Estimate cyclomatic complexity (integer)\\n" +
            "- numberOfMethods: Count of methods/functions (integer)\\n" +
            "- numberOfClasses: Count of classes/interfaces (integer)\\n" +
            "- commentRatio: Percentage of commented lines (float 0-100)\\n" +
            "- codeComplexity: Overall complexity level (LOW/MEDIUM/HIGH)\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON object with exact key names above. Ensure numeric values are numbers, not strings.",
            language, code
        );
        
        try {
            String response = getResponseFromOpenAI(prompt);
            return parseMetricsFromResponse(response);
        } catch (Exception e) {
            logger.warn("Failed to extract metrics for {} code: {}", language, e.getMessage());
            // Return basic fallback metrics
            Map<String, Object> fallbackMetrics = new HashMap<>();
            fallbackMetrics.put("linesOfCode", code.split("\n").length);
            fallbackMetrics.put("cyclomaticComplexity", 1);
            fallbackMetrics.put("numberOfMethods", 0);
            fallbackMetrics.put("numberOfClasses", 0);
            fallbackMetrics.put("commentRatio", 0.0);
            fallbackMetrics.put("codeComplexity", "UNKNOWN");
            return fallbackMetrics;
        }
    }

    private ScoreWithReason getScoreWithReasonFromOpenAI(String prompt) throws Exception {
        String response = getResponseFromOpenAI(prompt);
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            double score = jsonNode.get("score").asDouble();
            String reason = jsonNode.get("reason").asText();
            
            // Parse recommendations if available
            List<String> recommendations = new ArrayList<>();
            JsonNode recommendationsNode = jsonNode.get("recommendations");
            if (recommendationsNode != null && recommendationsNode.isArray()) {
                for (JsonNode rec : recommendationsNode) {
                    recommendations.add(rec.asText());
                }
            }
            
            return new ScoreWithReason(score, reason, recommendations);
        } catch (Exception e) {
            logger.warn("Could not parse score and reason from response: {}", response);
            // Try to extract just a number as fallback
            try {
                String cleanResponse = response.trim().replaceAll("[^0-9.]", "");
                double score = Double.parseDouble(cleanResponse);
                return new ScoreWithReason(score, "Unable to parse detailed reasoning from response");
            } catch (NumberFormatException ne) {
                logger.warn("Could not parse any score from response: {}", response);
                return new ScoreWithReason(50.0, "Unable to analyze - using default score");
            }
        }
    }

    private String getResponseFromOpenAI(String prompt) throws Exception {
        return executeWithRetry(() -> {
            ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(MODEL)
                .messages(List.of(message))
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .build();
            
            var completion = openAiService.createChatCompletion(request);
            
            if (completion.getChoices() != null && !completion.getChoices().isEmpty()) {
                String response = completion.getChoices().get(0).getMessage().getContent();
                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("Received empty response from OpenAI");
                }
                return response;
            }
            
            throw new RuntimeException("No response choices received from OpenAI");
        });
    }
    
    private String executeWithRetry(java.util.function.Supplier<String> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.get();
                
            } catch (com.theokanning.openai.OpenAiHttpException e) {
                lastException = e;
                logger.warn("OpenAI HTTP error on attempt {}/{}: {} - {}", attempt, MAX_RETRIES, e.statusCode, e.getMessage());
                
                if (e.statusCode == 429) { // Rate limit
                    if (attempt < MAX_RETRIES) {
                        logger.info("Rate limit hit, retrying in {} ms...", RETRY_DELAY_MS * attempt);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new Exception("Interrupted during retry delay", ie);
                        }
                        continue;
                    }
                    throw new Exception("Rate limit exceeded after " + MAX_RETRIES + " attempts. Please try again later.", e);
                } else if (e.statusCode == 401) {
                    throw new Exception("Invalid API key. Please check your OPENAI_API_KEY.", e);
                } else if (e.statusCode == 403) {
                    throw new Exception("API key lacks required permissions.", e);
                } else if (e.statusCode >= 500) {
                    if (attempt < MAX_RETRIES) {
                        logger.info("Server error, retrying in {} ms...", RETRY_DELAY_MS * attempt);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new Exception("Interrupted during retry delay", ie);
                        }
                        continue;
                    }
                    throw new Exception("OpenAI server error after " + MAX_RETRIES + " attempts. Please try again later.", e);
                } else {
                    throw new Exception("OpenAI API error: " + e.getMessage(), e);
                }
                
            } catch (RuntimeException e) {
                lastException = e;
                logger.warn("Runtime error on attempt {}/{}: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Interrupted during retry delay", ie);
                    }
                    continue;
                }
                throw new Exception("API call failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error on attempt {}/{}: {}", attempt, MAX_RETRIES, e.getMessage());
                break; // Don't retry for unexpected errors
            }
        }
        
        throw new Exception("Failed after " + MAX_RETRIES + " attempts: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    private List<CodeIssue> parseIssuesFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            List<CodeIssue> issues = new ArrayList<>();
            
            if (jsonNode.isArray()) {
                for (JsonNode issueNode : jsonNode) {
                    CodeIssue issue = new CodeIssue(
                        issueNode.get("severity").asText(),
                        issueNode.get("type").asText(),
                        issueNode.get("description").asText(),
                        issueNode.has("lineNumber") ? issueNode.get("lineNumber").asInt() : null,
                        issueNode.get("suggestion").asText()
                    );
                    issues.add(issue);
                }
            }
            
            return issues;
        } catch (Exception e) {
            logger.warn("Could not parse issues from response: {}", response);
            return new ArrayList<>();
        }
    }

    private List<String> parseSuggestionsFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            List<String> suggestions = new ArrayList<>();
            
            if (jsonNode.isArray()) {
                for (JsonNode suggestion : jsonNode) {
                    suggestions.add(suggestion.asText());
                }
            }
            
            return suggestions;
        } catch (Exception e) {
            logger.warn("Could not parse suggestions from response: {}", response);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseMetricsFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            Map<String, Object> metrics = new HashMap<>();
            
            // Parse specific metrics with proper types
            metrics.put("linesOfCode", jsonNode.has("linesOfCode") ? jsonNode.get("linesOfCode").asInt() : 0);
            metrics.put("cyclomaticComplexity", jsonNode.has("cyclomaticComplexity") ? jsonNode.get("cyclomaticComplexity").asInt() : 0);
            metrics.put("numberOfMethods", jsonNode.has("numberOfMethods") ? jsonNode.get("numberOfMethods").asInt() : 0);
            metrics.put("numberOfClasses", jsonNode.has("numberOfClasses") ? jsonNode.get("numberOfClasses").asInt() : 0);
            metrics.put("commentRatio", jsonNode.has("commentRatio") ? jsonNode.get("commentRatio").asDouble() : 0.0);
            metrics.put("codeComplexity", jsonNode.has("codeComplexity") ? jsonNode.get("codeComplexity").asText() : "UNKNOWN");
            
            // Add any additional fields that might be present
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!metrics.containsKey(key)) {
                    JsonNode value = entry.getValue();
                    if (value.isInt()) {
                        metrics.put(key, value.asInt());
                    } else if (value.isDouble()) {
                        metrics.put(key, value.asDouble());
                    } else {
                        metrics.put(key, value.asText());
                    }
                }
            });
            
            return metrics;
        } catch (Exception e) {
            logger.warn("Could not parse metrics from response: {}", response);
            // Return default metrics if parsing fails
            Map<String, Object> defaultMetrics = new HashMap<>();
            defaultMetrics.put("linesOfCode", 0);
            defaultMetrics.put("cyclomaticComplexity", 0);
            defaultMetrics.put("numberOfMethods", 0);
            defaultMetrics.put("numberOfClasses", 0);
            defaultMetrics.put("commentRatio", 0.0);
            defaultMetrics.put("codeComplexity", "UNKNOWN");
            return defaultMetrics;
        }
    }

    /**
     * Generates KT purpose information for a code file
     */
    private String generateKTPurpose(String code, String language, Path filePath) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code file (%s) and describe its main purpose and functionality. " +
            "This will be used for Knowledge Transfer documentation for new team members. " +
            "Focus on what this file does, its role in the system, and key responsibilities:\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Provide a clear, concise description in 2-3 sentences.",
            language, filePath.getFileName(), code
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate KT purpose for {} code: {}", language, e.getMessage());
            return "Unable to analyze file purpose due to API error: " + e.getMessage();
        }
    }

    /**
     * Generates KT design information for a code file
     */
    private String generateKTDesign(String code, String language, Path filePath) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code file (%s) and describe its design approach, patterns used, " +
            "and architectural decisions. This will be used for Knowledge Transfer documentation. " +
            "Focus on design patterns, class structure, key algorithms, and implementation choices:\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Provide a clear description of the design approach in 2-3 sentences.",
            language, filePath.getFileName(), code
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate KT design for {} code: {}", language, e.getMessage());
            return "Unable to analyze file design due to API error: " + e.getMessage();
        }
    }

    /**
     * Generates KT modules information for a code file
     */
    private String generateKTModules(String code, String language, Path filePath) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code file (%s) and describe its relationships with other modules, " +
            "dependencies, and how it fits into the larger system. This will be used for Knowledge Transfer. " +
            "Focus on imports, dependencies, interfaces, and integration points:\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Provide a clear description of module relationships in 2-3 sentences.",
            language, filePath.getFileName(), code
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate KT modules for {} code: {}", language, e.getMessage());
            return "Unable to analyze file modules due to API error: " + e.getMessage();
        }
    }

    /**
     * Summarizes purpose information for KT documentation
     */
    public String summarizePurpose(String purposeData) throws Exception {
        String prompt = String.format(
            "Summarize the following purpose information for Knowledge Transfer documentation. " +
            "Keep it concise but comprehensive, suitable for new team members:\\n\\n%s\\n\\n" +
            "Return a well-structured summary in 2-3 paragraphs.",
            purposeData
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to summarize purpose data: {}", e.getMessage());
            return "Unable to generate purpose summary due to API error: " + e.getMessage();
        }
    }

    /**
     * Summarizes design information for KT documentation
     */
    public String summarizeDesign(String designData) throws Exception {
        String prompt = String.format(
            "Summarize the following design information for Knowledge Transfer documentation. " +
            "Focus on architectural decisions, design patterns, and key implementation choices. " +
            "Keep it suitable for new team members:\\n\\n%s\\n\\n" +
            "Return a well-structured summary in 2-3 paragraphs.",
            designData
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to summarize design data: {}", e.getMessage());
            return "Unable to generate design summary due to API error: " + e.getMessage();
        }
    }

    /**
     * Summarizes modules information for KT documentation
     */
    public String summarizeModules(String modulesData) throws Exception {
        String prompt = String.format(
            "Summarize the following modules information for Knowledge Transfer documentation. " +
            "Focus on module responsibilities, dependencies, and relationships. " +
            "Keep it suitable for new team members:\\n\\n%s\\n\\n" +
            "Return a well-structured summary in 2-3 paragraphs.",
            modulesData
        );
        
        try {
            return getResponseFromOpenAI(prompt);
        } catch (Exception e) {
            logger.warn("Failed to summarize modules data: {}", e.getMessage());
            return "Unable to generate modules summary due to API error: " + e.getMessage();
        }
    }
}
