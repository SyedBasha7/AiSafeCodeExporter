package com.inneo.aisafecodesync.core.scan;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class BinaryFileDetector {

    private final TextFileDetector textFileDetector;

    public BinaryFileDetector(TextFileDetector textFileDetector) {
        this.textFileDetector = textFileDetector;
    }

    public boolean isBinary(Path path) throws IOException {
        if (textFileDetector.isTextFile(path)) {
            return false;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = inputStream.readNBytes(8192);
            for (byte value : buffer) {
                if (value == 0) {
                    return true;
                }
            }
        }
        return true;
    }
}
