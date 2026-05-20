package com.inneo.aisafecodesync.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppDataDirectoryEnvironmentPostProcessorTest {

    private final AppDataDirectoryEnvironmentPostProcessor postProcessor = new AppDataDirectoryEnvironmentPostProcessor();

    @Test
    void ambientDebugEnvironmentVariableDoesNotEnableSpringBootDebugMode() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "systemEnvironment",
                Map.of("debug", "release")
        ));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("debug")).isEqualTo("false");
    }

    @Test
    void explicitCommandLineDebugStillWins() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "commandLineArgs",
                Map.of("debug", "true")
        ));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("debug")).isEqualTo("true");
    }
}
