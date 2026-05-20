# Developer Notes

## Architecture

The core sync implementation is under `com.inneo.aisafecodesync.core` and has no dependency on Spring MVC controllers or Thymeleaf templates. The web layer maps local H2 profile data into immutable core `SyncConfig` instances before validation, dry-run planning, and execution.

## Two-Phase Sync

`SyncPlanner` performs validation, traversal, filtering, path transformation, virtual text content transformation, conflict detection, and leak detection before any file write occurs.

`SyncExecutor` accepts only an executable `SyncPlan`. It creates directories and creates or updates files from planned bytes. It does not delete target files.

## Report Handling

Private browser reports are local and may show absolute source/target paths. Downloaded JSON/CSV reports are passed through `ReportSanitizer`, which replaces absolute roots and redacts configured search and sensitive values.

## Local Data

The H2 database and logs are created below:

```text
~/.ai-safe-code-sync/
```

The application creates this directory before datasource initialization.
