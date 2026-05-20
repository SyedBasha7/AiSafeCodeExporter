package com.inneo.aisafecodesync.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppDataDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path appData = Path.of(System.getProperty("user.home"), ".ai-safe-code-sync");
        try {
            Files.createDirectories(appData.resolve("logs"));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create application data directory: " + appData, ex);
        }
    }
}
