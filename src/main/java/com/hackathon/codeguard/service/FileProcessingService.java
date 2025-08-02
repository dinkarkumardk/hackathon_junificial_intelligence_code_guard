package com.hackathon.codeguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for handling file operations and processing
 */
public class FileProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    
    // Supported file extensions for code analysis
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
        ".java", ".js", ".ts", ".py", ".cpp", ".c", ".cs", ".php", ".rb", ".go", ".kt", ".scala"
    );

    /**
     * Reads the content of a file
     */
    public String readFileContent(Path filePath) throws IOException {
        logger.debug("Reading file content: {}", filePath);
        return Files.readString(filePath);
    }

    /**
     * Scans a directory recursively for code files
     */
    public List<Path> scanDirectoryForCodeFiles(Path directory) throws IOException {
        logger.info("Scanning directory for code files: {}", directory);
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(this::isCodeFile)
                .collect(Collectors.toList());
        }
    }

    /**
     * Checks if a file is a supported code file based on extension
     */
    public boolean isCodeFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream()
            .anyMatch(fileName::endsWith);
    }

    /**
     * Gets the file extension
     */
    public String getFileExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    /**
     * Determines the programming language based on file extension
     */
    public String determineProgrammingLanguage(Path file) {
        String extension = getFileExtension(file).toLowerCase();
        
        return switch (extension) {
            case ".java" -> "Java";
            case ".js" -> "JavaScript";
            case ".ts" -> "TypeScript";
            case ".py" -> "Python";
            case ".cpp", ".cc" -> "C++";
            case ".c" -> "C";
            case ".cs" -> "C#";
            case ".php" -> "PHP";
            case ".rb" -> "Ruby";
            case ".go" -> "Go";
            case ".kt" -> "Kotlin";
            case ".scala" -> "Scala";
            default -> "Unknown";
        };
    }

    /**
     * Validates if file exists and is readable
     */
    public boolean validateFile(Path file) {
        return Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file);
    }

    /**
     * Gets file size in bytes
     */
    public long getFileSize(Path file) throws IOException {
        return Files.size(file);
    }

    /**
     * Checks if file is too large for analysis (e.g., > 1MB)
     */
    public boolean isFileTooLarge(Path file) throws IOException {
        final long MAX_FILE_SIZE = 1024 * 1024; // 1MB
        return getFileSize(file) > MAX_FILE_SIZE;
    }
}
