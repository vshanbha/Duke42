package com.example.cliai.agent.tools;

import java.nio.file.Path;

import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.GrepTool.OutputMode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Sandbox wrapper around {@link GrepTool} that enforces allowedDirectory restrictions.
 * Without this, an LLM could supply an arbitrary path to grep outside the project.
 */
public final class SandboxedGrepTool {

    private final GrepTool delegate;
    private final Path allowedDirectory;

    public SandboxedGrepTool(GrepTool delegate, Path allowedDirectory) {
        this.delegate = delegate;
        this.allowedDirectory = allowedDirectory.toAbsolutePath().normalize();
    }

    @Tool(name = "Grep", description = "A powerful search tool built with pure Java regex. "
        + "Searches file contents using regular expressions. Supports full regex syntax. "
        + "Filter files by pattern with the include parameter. Returns file paths and line numbers with matching lines.")
    public String grep(
            @ToolParam(description = "The regular expression pattern to search for in file contents") String pattern,
            @ToolParam(description = "File or directory to search in. Defaults to current working directory.", required = false) String path,
            @ToolParam(description = "Glob pattern to filter files (e.g. \"*.js\").", required = false) String include,
            @ToolParam(description = "Output mode: \"content\" shows matching lines, \"files_with_matches\" shows file paths only, \"count\" shows match counts.", required = false) OutputMode outputMode,
            @ToolParam(description = "Number of lines to show before each match.", required = false) Integer beforeContext,
            @ToolParam(description = "Number of lines to show after each match.", required = false) Integer afterContext,
            @ToolParam(description = "Number of lines to show before and after each match.", required = false) Integer contextLines,
            @ToolParam(description = "Show line numbers in output.", required = false) Boolean showLineNumbers,
            @ToolParam(description = "Case insensitive search.", required = false) Boolean caseInsensitive,
            @ToolParam(description = "File type to search (e.g. \"java\", \"py\").", required = false) String type,
            @ToolParam(description = "Limit output to first N lines/entries.", required = false) Integer headLimit,
            @ToolParam(description = "Skip first N lines/entries before applying head_limit.", required = false) Integer offset,
            @ToolParam(description = "Enable multiline mode.", required = false) Boolean multiline) {
        String searchPath = (path != null && !path.isBlank()) ? path : allowedDirectory.toString();
        if (!isWithinAllowedDirectory(searchPath)) {
            return "Error: Access denied. Path is outside the allowed directories: " + searchPath;
        }
        return delegate.grep(pattern, searchPath, include, outputMode,
            beforeContext, afterContext, contextLines, showLineNumbers,
            caseInsensitive, type, headLimit, offset, multiline);
    }

    private boolean isWithinAllowedDirectory(String path) {
        try {
            Path target = Path.of(path);
            // Resolve symlinks if the path exists; fall back to normalize for non-existent paths
            Path resolved = target.toFile().exists() ? target.toRealPath() : target.toAbsolutePath().normalize();
            Path allowed = allowedDirectory.toFile().exists() ? allowedDirectory.toRealPath() : allowedDirectory;
            return resolved.startsWith(allowed);
        } catch (Exception e) {
            return false;
        }
    }
}
