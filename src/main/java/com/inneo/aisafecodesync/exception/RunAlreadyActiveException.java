package com.inneo.aisafecodesync.exception;

public class RunAlreadyActiveException extends AiSafeCodeSyncException {

    public RunAlreadyActiveException() {
        super("Another sync run is already active. Wait for it to finish before starting a new run.");
    }
}
