package com.example.cliai.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.GrepTool.OutputMode;

import static org.assertj.core.api.Assertions.assertThat;

class GrepToolTest {

    @Test
    void shouldGrepForPattern(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.java"), "public class Foo {\n    @Tool\n    public String run() {}\n}");
        GrepTool grepTool = GrepTool.builder().workingDirectory(tempDir).build();
        String result = grepTool.grep("@Tool", tempDir.toString(), null, OutputMode.content,
            null, null, null, null, null, null, null, null, null);
        assertThat(result).contains("@Tool");
    }

    @Test
    void shouldReturnEmptyForNoMatch(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.java"), "nothing here");
        GrepTool grepTool = GrepTool.builder().workingDirectory(tempDir).build();
        String result = grepTool.grep("nonexistent_xyz_pattern", tempDir.toString(), null, OutputMode.content,
            null, null, null, null, null, null, null, null, null);
        assertThat(result).contains("No matches");
    }
}
