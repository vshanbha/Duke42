package com.example.cliai.agent.tools;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;

class ToolWiringTest {

    @Test
    void shouldProduceToolCallbacksForAllThreeTools() {
        FileSystemTools fs = FileSystemTools.builder().allowedDirectory(Path.of(".")).build();
        GlobTool glob = GlobTool.builder().build();
        GrepTool grep = GrepTool.builder().build();

        ToolCallback[] callbacks = ToolCallbacks.from(fs, glob, grep);
        // FileSystemTools exposes 3 @Tool methods (read/write/edit) + GlobTool (1) + GrepTool (1)
        assertThat(callbacks.length).isEqualTo(5);
    }

    @Test
    void shouldProduceValidToolDefinitions() {
        FileSystemTools fs = FileSystemTools.builder().allowedDirectory(Path.of(".")).build();
        GlobTool glob = GlobTool.builder().build();
        GrepTool grep = GrepTool.builder().build();

        ToolCallback[] callbacks = ToolCallbacks.from(fs, glob, grep);
        for (ToolCallback tc : callbacks) {
            assertThat(tc.getToolDefinition()).isNotNull();
            assertThat(tc.getToolDefinition().name()).isNotBlank();
            assertThat(tc.getToolDefinition().description()).isNotBlank();
        }
    }
}
