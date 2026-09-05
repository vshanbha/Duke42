package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatAutoStarterConditionTest {

    @TestConfiguration
    @Import(ChatAutoStarter.class)
    static class StarterBeans {
        @Bean
        ChatLoop chatLoop() {
            return mock(ChatLoop.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StarterBeans.class));

    @Test
    void shouldLoadStarterByDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(ChatAutoStarter.class));
    }

    @Test
    void shouldLoadStarterWhenExplicitlyEnabled() {
        runner.withPropertyValues("chat.auto-start=true")
            .run(context -> assertThat(context).hasSingleBean(ChatAutoStarter.class));
    }

    @Test
    void shouldNotLoadStarterWhenDisabled() {
        runner.withPropertyValues("chat.auto-start=false")
            .run(context -> assertThat(context).doesNotHaveBean(ChatAutoStarter.class));
    }
}
