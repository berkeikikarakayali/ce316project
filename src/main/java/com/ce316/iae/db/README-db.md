# Database Layer — integration notes

Owner: **Salih Maleker (System & Database Architect)**
Design ref: Section 2 of the Milestone 1 design document
Requirements covered: **Req 3** (create project with a configuration), **Req 10** (open/save projects)

## TL;DR for teammates

You do **not** open a `Connection` yourself from random helper classes — construct DAOs against the **currently open** `.iae` via `DatabaseService.connection()`, typically from `App` or a controller façade.

- **Kuzey (Configuration Manager)** — use `ConfigurationService.reloadFromDb()`, `upsert()`, `delete()`, and `ImportExportService` backed by `ConfigurationDAO`.
- **Arda (File & Directory Manager)** — after `FileManager.prepareSubmissions()` returns its `List<Submission>`, call `StudentSubmissionDAO.deleteAll()` + `insertAll(list)`.
- **Fatih (Execution Engine)** — does not touch the database directly. You receive
  `Submission` objects (id already set) and hand `StudentReport` objects to Berke.
- **Berke (Evaluation & Reporting)** — wire `ReportingService.saveToProject()` to
  `EvaluationResultDAO.deleteAll()` + `insertAll(reports)`. Each report's
  `studentSubmissionId` must already be set from the matching `Submission`.

## Project lifecycle (Controller side)

```java
DatabaseService db = new DatabaseService();   // hold this on Controller

// onNewProject
db.createNewProject(Path.of("C:/.../hw1.iae"));

// onOpenProject
db.openProject(Path.of("C:/.../hw1.iae"));

// onSaveProject
db.saveProject();   // safety commit; autocommit is on by default

// onExit / onCloseProject
db.closeProject();
```

After `createNewProject` or `openProject`, build DAOs lazily:

```java
ConfigurationDAO   cfgDao = new ConfigurationDAO(db.connection());
ProjectDAO         prjDao = new ProjectDAO(db.connection());
StudentSubmissionDAO subDao = new StudentSubmissionDAO(db.connection());
EvaluationResultDAO  resDao = new EvaluationResultDAO(db.connection());
```

## Schema

See [schema.sql](../../../../resources/db/schema.sql). Tables align with design doc §2.1 plus internal `meta.schema_version` (**currently version 2**, adding `PROJECT.zip_folder_path` and `PROJECT.main_source_filename`). Foreign keys are enabled (`PRAGMA foreign_keys = ON`) on every open. `EVALUATION_RESULT.student_submission_id` cascades on delete.

## Model class ownership

| Class | Persistence shape | Behavior |
|---|---|---|
| `Project` | Salih | — |
| `LanguageConfig` | Salih | Kuzey (validate, buildCompileCommand, buildRunCommand) |
| `Submission` | Salih | Arda (extraction logic) |
| `StudentReport` | Salih | Berke (comparison logic, toCSVRow) |

Add behavior methods to these classes on your own feature branch. **Do not rename
the fields or change their types** — that's what caused the merge pain Meeting
Report 3 warned about. If a field genuinely needs to change, open a PR against the
schema first.
