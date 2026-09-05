package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChatAutoStarterTest {

    @Test
    void shouldEnterChatOnInteractiveStartup() {
        ChatLoop chatLoop = mock(ChatLoop.class);
        ChatAutoStarter starter = new ChatAutoStarter(chatLoop, () -> true);
        ApplicationArguments args = new DefaultApplicationArguments();
        starter.run(args);
        verify(chatLoop).chat();
    }

    @Test
    void shouldSkipChatWithoutConsole() {
        ChatLoop chatLoop = mock(ChatLoop.class);
        ChatAutoStarter starter = new ChatAutoStarter(chatLoop, () -> false);
        ApplicationArguments args = new DefaultApplicationArguments();
        starter.run(args);
        verify(chatLoop, never()).chat();
    }

    @Test
    void shouldSkipChatForShellCommandArgs() {
        ChatLoop chatLoop = mock(ChatLoop.class);
        ChatAutoStarter starter = new ChatAutoStarter(chatLoop, () -> true);
        ApplicationArguments args = new DefaultApplicationArguments("help");
        starter.run(args);
        verify(chatLoop, never()).chat();
    }

    @Test
    void shouldEnterChatWithSpringOptionArgs() {
        ChatLoop chatLoop = mock(ChatLoop.class);
        ChatAutoStarter starter = new ChatAutoStarter(chatLoop, () -> true);
        ApplicationArguments args = new DefaultApplicationArguments("--rag.enabled=true");
        starter.run(args);
        verify(chatLoop).chat();
    }
}
