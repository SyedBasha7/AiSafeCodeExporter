# AiSafeCodeExporter

AiSafeCodeExporter is a local-only Spring Boot web application for creating an AI-safe, sanitised copy of a source-code project. It helps remove or replace customer, project, tenant, environment, credential, server, and organisation details before a copied target folder is shared with AI tools.

The sync engine is independent of Spring MVC, so it can be reused by another interface later, such as a CLI.

## Privacy Model

- The application binds to `127.0.0.1:8080` by default.
- Project data is never uploaded by the running application.
- There is no telemetry, analytics, AI API integration, or external network call in application code.
- Profiles and run history are stored in a local H2 database under `~/.ai-safe-code-sync/`.
- Generated target files are not stored in the database.
- Private browser reports may contain local paths and rule ids.
- AI-safe JSON/CSV exports replace absolute roots with `${SOURCE_ROOT}` and `${TARGET_ROOT}` and redact replacement search values and sensitive terms.

Do not paste real customer, tenant, server, environment, credential, organisation, or private project values into AI prompts. Use the exported AI-safe report and the sanitised target copy only.

## Requirements

- Java 21
- Maven 3.9+

## Run

```powershell
mvn spring-boot:run
```

Open:

```text
http://127.0.0.1:8080
```

The default binding is configured in `src/main/resources/application.yml`:

```yaml
server:
  address: 127.0.0.1
  port: 8080
```

## Build

```powershell
mvn clean package
```

The executable jar is written to:

```text
target\AiSafeCodeExporter-0.1.0-SNAPSHOT.jar
```

Run the packaged app with:

```powershell
java -jar target\AiSafeCodeExporter-0.1.0-SNAPSHOT.jar
```

Production startup uses the idempotent `schema.sql` initializer for the local H2 database. Hibernate DDL mutation is disabled in normal runs, so repeated starts should not log table-or-constraint-already-exists warnings.

## Test

```powershell
mvn test
```

## Browser Workflow

1. Create a profile.
2. Enter source and target folders.
3. Review include and exclude patterns.
4. Add replacement rules.
5. Add sensitive term rules.
6. Save the profile as a draft.
7. Validate the configuration.
8. Run dry-run.
9. Review the private browser report.
10. Confirm actual execution only after a successful dry-run.
11. Export an AI-safe report as JSON or CSV if needed.

For `AI_SAFE_EXPORT` profiles, actual execution is blocked until a recent successful dry-run exists for the exact same config hash. Editing the profile invalidates the previous dry-run.

## Rule Format In The UI

Replacement rules use one line per rule:

```text
id|search|replacement|caseSensitive|regex|enabled|applyTargets
```

Fake values:

```text
project-name|DemoCustomerPortal|demo-app|true|false|true|DIRECTORY_NAME,FILE_NAME,FILE_CONTENT
```

Sensitive term rules use one line per rule:

```text
id|caseSensitive|enabled|comma-separated-values
```

Fake values:

```text
demo-sensitive|true|true|DemoCustomerPortal,DemoTenant
```

## Default Safety Behaviour

- Secret-like files are excluded by default.
- Binary file contents are never transformed.
- Target files are created or updated, but never deleted in v1.
- Planning detects unreadable files, invalid target paths, mapping conflicts, and remaining sensitive leaks before execution.
- If AI-safe leaks remain after transformation, dry-run is marked failed and execution is blocked.

## YAML Import And Export

Profiles can be exported and imported as YAML from the browser UI. Exported profile YAML can contain real replacement search values and sensitive terms, so keep those files private.

`sample-profile.yml` contains fake values only.

## Limitations

- No deletion of target files in v1.
- Content decoding uses UTF-8 for text files.
- Replacement rule editing is intentionally server-rendered and text-based.
- AI-safe export redaction is based on configured roots, replacement search values, and sensitive term values.

## Future Enhancements

- CLI interface backed by the same core engine.
- Optional richer rule editor controls.
- More text encoding detection options.
- More granular report filtering for very large projects.
