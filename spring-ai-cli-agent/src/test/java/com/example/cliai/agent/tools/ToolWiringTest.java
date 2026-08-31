package com.example.cliai.agent.tools;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class ToolWiringTest {

    @Test
    void shouldProduceToolCallbacksForAllTools() {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        FileSystemTools fs = FileSystemTools.builder().allowedDirectory(projectRoot).build();
        SandboxedGlobTool glob = new SandboxedGlobTool(GlobTool.builder().build(), projectRoot);
        SandboxedGrepTool grep = new SandboxedGrepTool(GrepTool.builder().build(), projectRoot);

        ToolCallback[] callbacks = ToolCallbacks.from(fs, glob, grep);
        Set<String> names = Arrays.stream(callbacks)
            .map(tc -> tc.getToolDefinition().name())
            .collect(Collectors.toSet());
        // At minimum: FileSystemTools (read/write/edit) + Glob + Grep
        assertThat(names).contains("Read", "Write", "Edit", "Glob", "Grep");
    }

    @Test
    void shouldProduceValidToolDefinitions() {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        FileSystemTools fs = FileSystemTools.builder().allowedDirectory(projectRoot).build();
        SandboxedGlobTool glob = new SandboxedGlobTool(GlobTool.builder().build(), projectRoot);
        SandboxedGrepTool grep = new SandboxedGrepTool(GrepTool.builder().build(), projectRoot);

        ToolCallback[] callbacks = ToolCallbacks.from(fs, glob, grep);
        for (ToolCallback tc : callbacks) {
            assertThat(tc.getToolDefinition()).isNotNull();
            assertThat(tc.getToolDefinition().name()).isNotBlank();
            assertThat(tc.getToolDefinition().description()).isNotBlank();
        }
    }
}
