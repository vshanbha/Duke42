package com.example.cliai.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.GlobTool;

import static org.assertj.core.api.Assertions.assertThat;

class GlobToolTest {

    @Test
    void shouldGlobJavaFiles(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("test.java"), "// code");
        Files.writeString(tempDir.resolve("src/Util.java"), "// code");
        Files.writeString(tempDir.resolve("config.txt"), "config");

        GlobTool globTool = GlobTool.builder().workingDirectory(tempDir).build();
        String result = globTool.glob("**/*.java", tempDir.toString());
        assertThat(result).contains("test.java");
        assertThat(result).contains("Util.java");
    }

    @Test
    void shouldReturnEmptyForNoMatch() {
        GlobTool globTool = GlobTool.builder().build();
        String result = globTool.glob("**/*.nonexistent", ".");
        assertThat(result).contains("No files");
    }
}
