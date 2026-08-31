package com.example.cliai.agent.tools;

import java.nio.file.Path;

import org.springaicommunity.agent.tools.GlobTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Sandbox wrapper around {@link GlobTool} that enforces allowedDirectory restrictions.
 * Without this, an LLM could supply an arbitrary path to glob outside the project.
 */
public final class SandboxedGlobTool {

    private final GlobTool delegate;
    private final Path allowedDirectory;

    public SandboxedGlobTool(GlobTool delegate, Path allowedDirectory) {
        this.delegate = delegate;
        this.allowedDirectory = allowedDirectory.toAbsolutePath().normalize();
    }

    @Tool(name = "Glob", description = "Fast file pattern matching tool that works with any codebase size. "
        + "Supports glob patterns like \"**/*.js\" or \"src/**/*.ts\". Returns matching file paths. "
        + "Use this when you need to find files by name patterns.")
    public String glob(
            @ToolParam(description = "The glob pattern to match files against") String pattern,
            @ToolParam(description = "The directory to search in. Defaults to current working directory.", required = false) String path) {
        String searchPath = (path != null && !path.isBlank()) ? path : allowedDirectory.toString();
        if (!isWithinAllowedDirectory(searchPath)) {
            return "Error: Access denied. Path is outside the allowed directories: " + searchPath;
        }
        return delegate.glob(pattern, searchPath);
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
