package com.inneo.aisafecodesync.config;

import com.inneo.aisafecodesync.exception.StartupPreflightException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class AppDataDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String COMMAND_LINE_ARGS = "commandLineArgs";
    private static final String SYSTEM_PROPERTIES = "systemProperties";
    private static final String H2_FILE_PREFIX = "jdbc:h2:file:";

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
        verifyConfiguredPortIsAvailable(environment);
        verifyH2FileDatabaseIsNotLocked(environment);
    }

    private boolean hasExplicitDebugOverride(ConfigurableEnvironment environment) {
        return containsDebugProperty(environment, COMMAND_LINE_ARGS)
                || containsDebugProperty(environment, SYSTEM_PROPERTIES);
    }

    private boolean containsDebugProperty(ConfigurableEnvironment environment, String propertySourceName) {
        return environment.getPropertySources().contains(propertySourceName)
                && environment.getPropertySources().get(propertySourceName).containsProperty("debug");
    }

    private void verifyConfiguredPortIsAvailable(ConfigurableEnvironment environment) {
        Integer port = environment.getProperty("server.port", Integer.class, 8080);
        if (port == null || port == 0) {
            return;
        }
        String address = environment.getProperty("server.address", "127.0.0.1");
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(address, port));
        } catch (IOException | IllegalArgumentException ex) {
            throw new StartupPreflightException(
                    "AiSafeCodeExporter cannot start because " + address + ":" + port + " is already in use.",
                    "Open http://" + address + ":" + port + " to use the existing instance, or stop the Java process that is already using that port before starting a new instance.",
                    ex
            );
        }
    }

    private void verifyH2FileDatabaseIsNotLocked(ConfigurableEnvironment environment) {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        Path databaseFile = h2DatabaseFile(datasourceUrl);
        if (databaseFile == null || !Files.exists(databaseFile)) {
            return;
        }
        try (FileChannel channel = FileChannel.open(databaseFile, StandardOpenOption.WRITE);
             FileLock ignored = channel.tryLock()) {
            if (ignored == null) {
                throw databaseLocked(databaseFile, null);
            }
        } catch (OverlappingFileLockException ex) {
            throw databaseLocked(databaseFile, ex);
        } catch (IOException ex) {
            throw databaseLocked(databaseFile, ex);
        }
    }

    private StartupPreflightException databaseLocked(Path databaseFile, Throwable cause) {
        return new StartupPreflightException(
                "AiSafeCodeExporter cannot start because the local H2 database is already locked: " + databaseFile.toAbsolutePath().normalize(),
                "Stop the other AiSafeCodeExporter/Java process that is using this database, then start the app again. Do not delete the database file unless you intentionally want to remove local profiles and run history.",
                cause
        );
    }

    private Path h2DatabaseFile(String datasourceUrl) {
        if (datasourceUrl == null || !datasourceUrl.startsWith(H2_FILE_PREFIX)) {
            return null;
        }
        String value = datasourceUrl.substring(H2_FILE_PREFIX.length());
        int optionsStart = value.indexOf(';');
        if (optionsStart >= 0) {
            value = value.substring(0, optionsStart);
        }
        if (value.isBlank()) {
            return null;
        }
        if (value.equals("~") || value.startsWith("~/") || value.startsWith("~\\")) {
            value = System.getProperty("user.home") + value.substring(1);
        }
        Path path = Path.of(value);
        Path databaseFile = path.toString().endsWith(".mv.db") ? path : Path.of(path + ".mv.db");
        return databaseFile.toAbsolutePath().normalize();
    }
}
