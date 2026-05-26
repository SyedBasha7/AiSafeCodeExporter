package com.inneo.aisafecodesync.exception;

public class StartupPreflightException extends RuntimeException {

    private final String action;

    public StartupPreflightException(String message, String action) {
        super(message);
        this.action = action;
    }

    public StartupPreflightException(String message, String action, Throwable cause) {
        super(message, cause);
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
