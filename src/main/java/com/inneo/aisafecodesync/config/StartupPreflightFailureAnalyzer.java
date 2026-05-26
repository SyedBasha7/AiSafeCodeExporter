package com.inneo.aisafecodesync.config;

import com.inneo.aisafecodesync.exception.StartupPreflightException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class StartupPreflightFailureAnalyzer extends AbstractFailureAnalyzer<StartupPreflightException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, StartupPreflightException cause) {
        return new FailureAnalysis(cause.getMessage(), cause.getAction(), cause);
    }
}
