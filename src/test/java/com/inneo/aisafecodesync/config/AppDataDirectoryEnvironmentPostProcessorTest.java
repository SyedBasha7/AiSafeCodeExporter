package com.inneo.aisafecodesync.config;

import com.inneo.aisafecodesync.exception.StartupPreflightException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppDataDirectoryEnvironmentPostProcessorTest {

    private final AppDataDirectoryEnvironmentPostProcessor postProcessor = new AppDataDirectoryEnvironmentPostProcessor();

    @Test
    void ambientDebugEnvironmentVariableDoesNotEnableSpringBootDebugMode() {
        StandardEnvironment environment = environmentWithServerPortZero();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "systemEnvironment",
                Map.of("debug", "release")
        ));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("debug")).isEqualTo("false");
    }

    @Test
    void explicitCommandLineDebugStillWins() {
        StandardEnvironment environment = environmentWithServerPortZero();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "commandLineArgs",
                Map.of("debug", "true")
        ));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("debug")).isEqualTo("true");
    }

    @Test
    void occupiedConfiguredPortFailsBeforeSpringStartsTomcat() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "testPort",
                    Map.of("server.address", "127.0.0.1", "server.port", Integer.toString(port))
            ));

            assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()))
                    .isInstanceOf(StartupPreflightException.class)
                    .hasMessageContaining("127.0.0.1:" + port)
                    .hasMessageContaining("already in use");
        }
    }

    @Test
    void lockedH2FileDatabaseFailsWithClearMessage() throws Exception {
        Path directory = Files.createTempDirectory("aisafe-h2-lock");
        Path databaseFile = directory.resolve("aisafecodesync.mv.db");
        Files.writeString(databaseFile, "");
        try (FileChannel channel = FileChannel.open(databaseFile, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            StandardEnvironment environment = environmentWithServerPortZero();
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "testDatasource",
                    Map.of("spring.datasource.url", "jdbc:h2:file:" + directory.resolve("aisafecodesync") + ";AUTO_SERVER=FALSE")
            ));

            assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()))
                    .isInstanceOf(StartupPreflightException.class)
                    .hasMessageContaining("local H2 database is already locked")
                    .hasMessageContaining("aisafecodesync.mv.db");
        }
    }

    private StandardEnvironment environmentWithServerPortZero() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "testServer",
                Map.of("server.port", "0")
        ));
        return environment;
    }
}
