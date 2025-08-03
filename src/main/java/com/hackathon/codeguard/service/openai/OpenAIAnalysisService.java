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
    public FileAnalysisResult analyzeCodeFile(Path filePath, String fileContent, AnalysisMode mode) throws Exception {
        logger.debug("Analyzing file with OpenAI: {}", filePath);
        
        FileAnalysisResult result = new FileAnalysisResult(
            filePath.getFileName().toString(),
            filePath.toString()
        );

        // Determine programming language
        String language = fileService.determineProgrammingLanguage(filePath);
        
        // Analyze different aspects
        ScoreWithReason codeQualityResult = analyzeCodeQuality(fileContent, language);
        result.setCodeQuality(codeQualityResult.getScore());
        result.setCodeQualityReason(codeQualityResult.getReason());
        result.setCodeQualityRecommendations(codeQualityResult.getRecommendations());
        
        ScoreWithReason solidResult = analyzeSolidPrinciples(fileContent, language);
        result.setSolid(solidResult.getScore());
        result.setSolidReason(solidResult.getReason());
        result.setSolidRecommendations(solidResult.getRecommendations());
        
        ScoreWithReason designPatternsResult = analyzeDesignPatterns(fileContent, language);
        result.setDesignPatterns(designPatternsResult.getScore());
        result.setDesignPatternsReason(designPatternsResult.getReason());
        result.setDesignPatternsRecommendations(designPatternsResult.getRecommendations());
        
        ScoreWithReason securityResult = analyzeSecurity(fileContent, language);
        result.setSecurity(securityResult.getScore());
        result.setSecurityReason(securityResult.getReason());
        result.setSecurityRecommendations(securityResult.getRecommendations());
        
        // Calculate final score
        result.calculateFinalScore();
        
        // Get issues and suggestions
        result.setIssues(identifyIssues(fileContent, language));
        result.setSuggestions(generateSuggestions(fileContent, language, mode));
        result.setMetrics(extractMetrics(fileContent, language));
        
        return result;
    }

    private ScoreWithReason analyzeCodeQuality(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for overall quality including readability, maintainability, " +
            "and documentation. Provide a score from 0-100 where 100 is excellent quality.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 4-5 concise actionable recommendations to improve code quality)",
            language, code
        );
        
        return getScoreWithReasonFromOpenAI(prompt);
    }

    private ScoreWithReason analyzeSolidPrinciples(String code, String language) throws Exception {
        String prompt = String.format(
            "Evaluate how well the following %s code follows SOLID principles " +
            "(Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion). " +
            "Return a score from 0-100.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON with keys:\\n" +
            "- 'score' (number 0-100)\\n" +
            "- 'reason' (detailed explanation for the score)\\n" +
            "- 'recommendations' (array of 4-5 concise actionable recommendations to improve SOLID compliance)",
            language, code
        );
        
        return getScoreWithReasonFromOpenAI(prompt);
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
            "- 'recommendations' (array of 4-5 concise actionable recommendations to improve design patterns usage)",
            language, code
        );
        
        return getScoreWithReasonFromOpenAI(prompt);
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
            "- 'recommendations' (array of 4-5 concise actionable recommendations to improve security)",
            language, code
        );
        
        return getScoreWithReasonFromOpenAI(prompt);
    }

    private List<CodeIssue> identifyIssues(String code, String language) throws Exception {
        String prompt = String.format(
            "Identify specific issues in the following %s code. For each issue, provide:\\n" +
            "- Severity (CRITICAL, HIGH, MEDIUM, LOW)\\n" +
            "- Type (e.g., Security, Performance, Maintainability)\\n" +
            "- Description\\n" +
            "- Line number (if applicable)\\n" +
            "- Suggestion for fix\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON array with objects containing: severity, type, description, lineNumber, suggestion",
            language, code
        );
        
        String response = getResponseFromOpenAI(prompt);
        return parseIssuesFromResponse(response);
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
            "Return suggestions as a JSON array of strings.",
            language, modeContext, code
        );
        
        String response = getResponseFromOpenAI(prompt);
        return parseSuggestionsFromResponse(response);
    }

    private Map<String, Object> extractMetrics(String code, String language) throws Exception {
        String prompt = String.format(
            "Extract code metrics from the following %s code including:\\n" +
            "- Lines of code\\n" +
            "- Cyclomatic complexity estimate\\n" +
            "- Number of methods/functions\\n" +
            "- Number of classes\\n" +
            "- Comment ratio\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Return as JSON object with these metrics.",
            language, code
        );
        
        String response = getResponseFromOpenAI(prompt);
        return parseMetricsFromResponse(response);
    }

    private ScoreWithReason getScoreWithReasonFromOpenAI(String prompt) throws Exception {
        String response = getResponseFromOpenAI(prompt);
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            double score = jsonNode.get("score").asDouble();
            String reason = jsonNode.get("reason").asText();
            
            // Parse recommendations if present
            List<String> recommendations = new ArrayList<>();
            if (jsonNode.has("recommendations") && jsonNode.get("recommendations").isArray()) {
                for (JsonNode recNode : jsonNode.get("recommendations")) {
                    recommendations.add(recNode.asText());
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
        ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(MODEL)
            .messages(List.of(message))
            .maxTokens(MAX_TOKENS)
            .temperature(TEMPERATURE)
            .build();
        
        var completion = openAiService.createChatCompletion(request);
        
        if (completion.getChoices() != null && !completion.getChoices().isEmpty()) {
            return completion.getChoices().get(0).getMessage().getContent();
        }
        
        throw new RuntimeException("No response received from OpenAI");
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
            
            jsonNode.fields().forEachRemaining(entry -> 
                metrics.put(entry.getKey(), entry.getValue().asText())
            );
            
            return metrics;
        } catch (Exception e) {
            logger.warn("Could not parse metrics from response: {}", response);
            return new HashMap<>();
        }
    }
}
