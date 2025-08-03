package com.hackathon.codeguard.service.openai;

import com.hackathon.codeguard.cli.CodeGuardCLI.AnalysisMode;
import com.hackathon.codeguard.model.FileAnalysisResult;
import com.hackathon.codeguard.model.FileAnalysisResult.CodeIssue;
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
    public FileAnalysisResult analyzeCodeFile(Path filePath, String fileContent, AnalysisMode mode, boolean ktEnabled) throws Exception {
        logger.debug("Analyzing file with OpenAI: {}", filePath);

        FileAnalysisResult result = new FileAnalysisResult(
            filePath.getFileName().toString(),
            filePath.toString()
        );

        // Determine programming language
        String language = fileService.determineProgrammingLanguage(filePath);

        // Analyze different aspects
        result.setCodeQuality(analyzeCodeQuality(fileContent, language));
        result.setSolid(analyzeSolidPrinciples(fileContent, language));
        result.setDesignPatterns(analyzeDesignPatterns(fileContent, language));
        result.setCleanCode(analyzeCleanCode(fileContent, language));
        result.setSecurity(analyzeSecurity(fileContent, language));

        // Extract KT data using OpenAI only if KT is enabled
        if (ktEnabled) {
            result.setKtPurpose(analyzeKtPurpose(fileContent, language));
            result.setKtDesign(analyzeKtDesign(fileContent, language));
            result.setKtModules(analyzeKtModules(fileContent, language));
        }

        // Calculate final score
        result.calculateFinalScore();

        // Get issues and suggestions
        result.setIssues(identifyIssues(fileContent, language));
        result.setSuggestions(generateSuggestions(fileContent, language, mode));
        result.setMetrics(extractMetrics(fileContent, language));

        return result;
    }

    private double analyzeCodeQuality(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for overall quality including readability, maintainability, " +
            "and documentation. Return a score from 0-100 where 100 is excellent quality.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Respond with only a number between 0 and 100.",
            language, code
        );
        
        return getScoreFromOpenAI(prompt);
    }

    private double analyzeSolidPrinciples(String code, String language) throws Exception {
        String prompt = String.format(
            "Evaluate how well the following %s code follows SOLID principles " +
            "(Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion). " +
            "Return a score from 0-100.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Respond with only a number between 0 and 100.",
            language, code
        );
        
        return getScoreFromOpenAI(prompt);
    }

    private double analyzeDesignPatterns(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for proper use of design patterns and architectural decisions. " +
            "Consider if appropriate patterns are used and if they're implemented correctly. " +
            "Return a score from 0-100.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Respond with only a number between 0 and 100.",
            language, code
        );
        
        return getScoreFromOpenAI(prompt);
    }

    private double analyzeCleanCode(String code, String language) throws Exception {
        String prompt = String.format(
            "Evaluate the following %s code for clean code practices including meaningful names, " +
            "small functions, clear structure, and minimal complexity. Return a score from 0-100.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Respond with only a number between 0 and 100.",
            language, code
        );
        
        return getScoreFromOpenAI(prompt);
    }

    private double analyzeSecurity(String code, String language) throws Exception {
        String prompt = String.format(
            "Analyze the following %s code for security vulnerabilities and best practices. " +
            "Look for common security issues like injection flaws, insecure data handling, etc. " +
            "Return a score from 0-100 where 100 is very secure.\\n\\n" +
            "Code:\\n%s\\n\\n" +
            "Respond with only a number between 0 and 100.",
            language, code
        );
        
        return getScoreFromOpenAI(prompt);
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

    private double getScoreFromOpenAI(String prompt) throws Exception {
        String response = getResponseFromOpenAI(prompt);
        try {
            // Extract number from response
            String cleanResponse = response.trim().replaceAll("[^0-9.]", "");
            return Double.parseDouble(cleanResponse);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse score from response: {}", response);
            return 50.0; // Default score
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

    // --- Replace extractKt* with analyzeKt* using new prompts ---
    private String analyzeKtPurpose(String code, String language) throws Exception {
        String prompt = "Describe the purpose of your system in 2–3 sentences. What is the core problem it aims to solve? Who are the primary users or stakeholders (e.g., end users, developers, admins, business teams)? What are the system's main objectives (e.g., automation, monitoring, data analysis, user experience enhancement)?\n\nCode:\n" + code;
        return getResponseFromOpenAI(prompt);
    }
    private String analyzeKtDesign(String code, String language) throws Exception {
        String prompt = "Provide a high-level architectural overview of your system in 2–3 sentences. Include:\n- Core components (e.g., frontend, backend, services, databases)\n- Technology stack used (languages, frameworks, tools, infrastructure)\n- How components interact with each other (e.g., API calls, message queues, DB connections)\nIf available, include or reference an architecture diagram.\n\nCode:\n" + code;
        return getResponseFromOpenAI(prompt);
    }
    private String analyzeKtModules(String code, String language) throws Exception {
        String prompt = "List the major functional modules in your system (e.g., User Management, Payment Processing, Analytics Dashboard) in 2–3 sentences. For each:\n- Briefly describe its responsibilities and what it does\n- Highlight any important business logic (e.g., validations, workflows, data rules) that it implements\n\nCode:\n" + code;
        return getResponseFromOpenAI(prompt);
    }
    // Remove extractKt* methods

    /**
     * Summarizes the KT Purpose section using OpenAI
     */
    public String summarizePurpose(String purposeText) {
        String prompt = "Summarize the following project purpose and goals for onboarding documentation. " +
                "Make it concise, clear, and suitable for new joiners.\n\n" + purposeText;
        try {
            return getResponseFromOpenAI(prompt).trim();
        } catch (Exception e) {
            logger.warn("OpenAI summarizePurpose failed: {}", e.getMessage());
            return "No summary available.";
        }
    }

    /**
     * Summarizes the KT Design section using OpenAI
     */
    public String summarizeDesign(String designText) {
        String prompt = "Summarize the following system design, architecture, and tech stack for onboarding documentation. " +
                "Make it concise, clear, and suitable for new joiners.\n\n" + designText;
        try {
            return getResponseFromOpenAI(prompt).trim();
        } catch (Exception e) {
            logger.warn("OpenAI summarizeDesign failed: {}", e.getMessage());
            return "No summary available.";
        }
    }

    /**
     * Summarizes the KT Modules section using OpenAI
     */
    public String summarizeModules(String modulesText) {
        String prompt = "Summarize the following modules and business logic for onboarding documentation. " +
                "Make it concise, clear, and suitable for new joiners.\n\n" + modulesText;
        try {
            return getResponseFromOpenAI(prompt).trim();
        } catch (Exception e) {
            logger.warn("OpenAI summarizeModules failed: {}", e.getMessage());
            return "No summary available.";
        }
    }
}
