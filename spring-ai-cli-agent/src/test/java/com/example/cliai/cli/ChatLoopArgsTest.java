package com.example.cliai.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure parsing/mime helpers for the BLUEPRINT Step 7 (/convert) and Step 8 (/image) commands. */
class ChatLoopArgsTest {

    @Test
    void parseImageArgsShouldSplitPathAndQuestion() {
        ChatLoop.ImageQuery query = ChatLoop.parseImageArgs("/image /tmp/pic.jpg What do you see?");

        assertThat(query).isNotNull();
        assertThat(query.path()).isEqualTo("/tmp/pic.jpg");
        assertThat(query.question()).isEqualTo("What do you see?");
    }

    @Test
    void parseImageArgsShouldRejectMalformedInput() {
        assertThat(ChatLoop.parseImageArgs("/image")).isNull();
        assertThat(ChatLoop.parseImageArgs("/image /tmp/pic.jpg")).isNull();
    }

    @Test
    void parseConvertArgsShouldReturnTriple() {
        String[] args = ChatLoop.parseConvertArgs("/convert 100 km miles");

        assertThat(args).containsExactly("100", "km", "miles");
    }

    @Test
    void parseConvertArgsShouldRejectBadInput() {
        assertThat(ChatLoop.parseConvertArgs("/convert 100 km")).isNull();
        assertThat(ChatLoop.parseConvertArgs("/convert abc km miles")).isNull();
    }

    @Test
    void mimeForShouldMapImageExtensions() {
        assertThat(ChatLoop.mimeFor("pic.png")).isEqualTo(org.springframework.util.MimeTypeUtils.IMAGE_PNG);
        assertThat(ChatLoop.mimeFor("pic.JPG")).isEqualTo(org.springframework.util.MimeTypeUtils.IMAGE_JPEG);
        assertThat(ChatLoop.mimeFor("anim.gif")).isEqualTo(org.springframework.util.MimeTypeUtils.IMAGE_GIF);
        assertThat(ChatLoop.mimeFor("photo.webp")).isEqualTo(org.springframework.util.MimeTypeUtils.parseMimeType("image/webp"));
        assertThat(ChatLoop.mimeFor("notes.txt")).isNull();
    }
}
