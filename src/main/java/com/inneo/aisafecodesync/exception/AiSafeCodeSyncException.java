package com.inneo.aisafecodesync.exception;

public class AiSafeCodeSyncException extends RuntimeException {

    public AiSafeCodeSyncException(String message) {
        super(message);
    }

    public AiSafeCodeSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
