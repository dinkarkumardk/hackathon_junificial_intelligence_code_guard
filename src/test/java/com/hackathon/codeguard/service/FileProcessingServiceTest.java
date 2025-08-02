package com.hackathon.codeguard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileProcessingService
 */
class FileProcessingServiceTest {

    private FileProcessingService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileProcessingService();
    }

    @Test
    void testIsCodeFile() {
        Path javaFile = tempDir.resolve("Test.java");
        Path txtFile = tempDir.resolve("readme.txt");
        
        assertTrue(fileService.isCodeFile(javaFile));
        assertFalse(fileService.isCodeFile(txtFile));
    }

    @Test
    void testDetermineProgrammingLanguage() {
        Path javaFile = tempDir.resolve("Test.java");
        Path jsFile = tempDir.resolve("script.js");
        Path pyFile = tempDir.resolve("app.py");
        
        assertEquals("Java", fileService.determineProgrammingLanguage(javaFile));
        assertEquals("JavaScript", fileService.determineProgrammingLanguage(jsFile));
        assertEquals("Python", fileService.determineProgrammingLanguage(pyFile));
    }

    @Test
    void testReadFileContent() throws Exception {
        String content = "public class Test { }";
        Path testFile = tempDir.resolve("Test.java");
        Files.writeString(testFile, content);
        
        String readContent = fileService.readFileContent(testFile);
        assertEquals(content, readContent);
    }

    @Test
    void testScanDirectoryForCodeFiles() throws Exception {
        // Create test files
        Files.writeString(tempDir.resolve("Test.java"), "class Test {}");
        Files.writeString(tempDir.resolve("script.js"), "console.log('test')");
        Files.writeString(tempDir.resolve("README.md"), "# README");
        
        List<Path> codeFiles = fileService.scanDirectoryForCodeFiles(tempDir);
        
        assertEquals(2, codeFiles.size());
        assertTrue(codeFiles.stream().anyMatch(p -> p.getFileName().toString().equals("Test.java")));
        assertTrue(codeFiles.stream().anyMatch(p -> p.getFileName().toString().equals("script.js")));
    }
}
