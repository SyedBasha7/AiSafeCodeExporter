package com.inneo.aisafecodesync.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AppDataDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String COMMAND_LINE_ARGS = "commandLineArgs";
    private static final String SYSTEM_PROPERTIES = "systemProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!hasExplicitDebugOverride(environment)) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "aiSafeCodeExporterDebugDefaults",
                    Map.of("debug", "false")
            ));
        }
        Path appData = Path.of(System.getProperty("user.home"), ".ai-safe-code-sync");
        try {
            Files.createDirectories(appData.resolve("logs"));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create application data directory: " + appData, ex);
        }
    }

    private boolean hasExplicitDebugOverride(ConfigurableEnvironment environment) {
        return containsDebugProperty(environment, COMMAND_LINE_ARGS)
                || containsDebugProperty(environment, SYSTEM_PROPERTIES);
    }

    private boolean containsDebugProperty(ConfigurableEnvironment environment, String propertySourceName) {
        return environment.getPropertySources().contains(propertySourceName)
                && environment.getPropertySources().get(propertySourceName).containsProperty("debug");
    }
}
