package com.inneo.aisafecodesync.core.plan;

public enum OperationType {
    CREATE_DIRECTORY,
    CREATE_FILE,
    UPDATE_FILE,
    SKIP_UNCHANGED,
    SKIP_EXCLUDED,
    TRANSFORM_PATH,
    TRANSFORM_CONTENT,
    CONFLICT,
    LEAK_DETECTED,
    ERROR
}
