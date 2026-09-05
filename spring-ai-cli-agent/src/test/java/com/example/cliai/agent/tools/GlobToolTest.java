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

    @Test
    void sandboxedGlobShouldRejectOutsideAllowedDirectory(@TempDir Path tempDir) {
        GlobTool globTool = GlobTool.builder().build();
        SandboxedGlobTool sandboxed = new SandboxedGlobTool(globTool, tempDir);
        String result = sandboxed.glob("**/*.java", "/etc");
        assertThat(result).contains("Access denied");
    }

    @Test
    void sandboxedGlobShouldRejectTraversal(@TempDir Path tempDir) {
        GlobTool globTool = GlobTool.builder().build();
        SandboxedGlobTool sandboxed = new SandboxedGlobTool(globTool, tempDir);
        String result = sandboxed.glob("**/*.java", tempDir.resolve("../etc").toString());
        assertThat(result).contains("Access denied");
    }

    @Test
    void sandboxedGlobShouldDefaultToAllowedDirectory(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("test.java"), "// code");
        GlobTool globTool = GlobTool.builder().build();
        SandboxedGlobTool sandboxed = new SandboxedGlobTool(globTool, tempDir);
        String result = sandboxed.glob("**/*.java", null);
        assertThat(result).contains("test.java");
    }

    @Test
    void sandboxedGlobShouldRejectSymlinkEscape(@TempDir Path tempDir) throws IOException {
        Path outsideFile = Files.createTempFile("secret", ".txt");
        Files.writeString(outsideFile, "secret data");
        Path symlink = tempDir.resolve("link");
        Files.createSymbolicLink(symlink, outsideFile);

        GlobTool globTool = GlobTool.builder().build();
        SandboxedGlobTool sandboxed = new SandboxedGlobTool(globTool, tempDir);
        String result = sandboxed.glob("**/*.txt", symlink.toString());
        assertThat(result).contains("Access denied");
    }
}
