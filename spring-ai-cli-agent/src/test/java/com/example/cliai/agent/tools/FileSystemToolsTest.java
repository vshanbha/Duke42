package com.example.cliai.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.FileSystemTools;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemToolsTest {

    private FileSystemTools fsTools;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        fsTools = FileSystemTools.builder().allowedDirectory(tempDir).build();
    }

    @Test
    void shouldReadProjectFile() throws IOException {
        Path file = tempDir.resolve("dummy.txt");
        Files.writeString(file, "<project></project>");
        String content = fsTools.read(file.toString(), 1, 5);
        assertThat(content).contains("<project>");
    }

    @Test
    void shouldReadWithLineRange() throws IOException {
        Path file = tempDir.resolve("lines.txt");
        Files.writeString(file, "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6");
        String content = fsTools.read(file.toString(), 3, 3);
        assertThat(content).contains("Line 3");
    }

    @Test
    void shouldWriteAndReadBack() throws IOException {
        Path file = tempDir.resolve("new_file.txt");
        fsTools.write(file.toString(), "Hello, world!");
        String content = fsTools.read(file.toString(), 1, 100);
        assertThat(content).contains("Hello, world!");
    }

    @Test
    void shouldEditFileContent() throws IOException {
        Path file = tempDir.resolve("config.xml");
        Files.writeString(file, "<config><setting>old_value</setting></config>");
        fsTools.edit(file.toString(), "old_value", "new_value", false);
        String content = fsTools.read(file.toString(), 1, 100);
        assertThat(content).contains("new_value");
    }

    @Test
    void shouldRejectOutsideAllowedDirectory() {
        assertThat(fsTools.read("/etc/passwd", 1, 1)).contains("Access denied");
    }
}
