package com.hackathon.codeguard;

import com.hackathon.codeguard.cli.CodeGuardCLI;
import picocli.CommandLine;

/**
 * Main application entry point for Code Guard tool.
 * 
 * A minimal code analysis tool that scores code files and generates 
 * technical & non-technical HTML reports using OpenAI APIs.
 */
public class CodeGuardApplication {
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CodeGuardCLI()).execute(args);
        System.exit(exitCode);
    }
}
