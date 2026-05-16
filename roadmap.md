# CE316 Team 7 — Integrated Assignment Environment (IAE) Roadmap

This roadmap aligns the **course project description** (`CE316 Project Description.pdf`), the **team design document** (`ce316_design_document_team7.pdf`, May 2, 2026), and the **current codebase** in this repository. It is a delivery-oriented plan; detailed UML remains in the PDFs.

---

## 1. Product snapshot

The **Integrated Assignment Environment** is a **standalone Windows desktop application** (JavaFX) that lets a lecturer:

1. Define **language configurations** (compile / interpret steps, paths, arguments).
2. Create an **assignment project** backed by a **single SQLite `.iae` file** (no database server).
3. Point at a folder of **student ZIP archives** (named by student ID); the app **batch-processes** every ZIP—no manual per-file unzip.
4. For each student: extract (via **`java.util.zip`**, not shell `zip`/`unzip`), compile or run per configuration, pass **project-level CLI arguments** to the student program, **compare stdout** to an **expected output file**, and record **PASS / FAIL / ERROR / COMPILE_ERROR / TIMEOUT**.
5. **Persist** submissions and evaluation rows in the project DB; **re-open** projects later (Req 10).
6. Ship with a **Windows installer** bundling runtime pieces (course assumes Java present but **not JavaFX** on the target machine—bundle accordingly), plus **Help → manual**.

---

## 2. Requirements traceability

| Req | Statement (abbrev.) | Primary design § | Owner (README) |
|-----|---------------------|------------------|----------------|
| **R1** | Windows installer; deps bundled | §7.1 UI & Deployment | Kutay |
| **R2** | Help menu opens manual | §7.2 | Kutay |
| **R3** | Create project using new/existing configuration | §2 System & Database | Salih (+ UI) |
| **R4** | CRUD configurations | §3 Configuration Manager | Kuzey |
| **R5** | Import/export configurations | §3.4 | Kuzey |
| **R6** | Batch ZIP processing | §4 File & Directory Manager | Arda |
| **R7** | Compile/interpreter via configuration | §5 Execution Engine | Fatih |
| **R8** | Compare output vs expected | §6 Evaluation & Reporting | Berke |
| **R9** | Display per-student results | §6 + §7.3 | Berke + Kutay |
| **R10** | Open/save projects anytime | §2 | Salih (+ UI) |

**Course constraints called out in the project PDF:** no reliance on servers (SQLite/file OK); **must not** assume shell `zip`/`unzip`; comparison should **not** depend on external diff tools unless you bundle them—the team design implements comparison **in Java**.

---

## 3. Architecture (from design doc)

- **Persistence:** SQLite `.iae` — tables `CONFIGURATION`, `PROJECT` (single row, `id = 1`), `STUDENT_SUBMISSION`, `EVALUATION_RESULT` (+ internal `meta` / versioning in repo schema).
- **Configuration:** `LanguageConfig`, validation, `ConfigurationService`, JSON **import/export** with merge vs replace modes and skipped-entry reporting.
- **Files:** `FileManager.prepareSubmissions(zipDir, sourceFileName)` → `List<Submission>`; resilient per-ZIP failure handling.
- **Execution:** `Executioner` + `Enforcer` (`ProcessBuilder`, timeouts, streamed stdout/stderr, **1 MB output cap** per design risks).
- **Evaluation:** `ComparisonService` with `NormalizationMode` (STRICT, TRIM_WHITESPACE, CASE_INSENSITIVE); `ReportingService` aggregates `StudentReport`, persists results, CSV export, summary stats.
- **UI:** Menu/toolbar shell; wizard for project fields (ZIP folder, expected output path, **run_args**, timeouts, normalization); config editor; import/export dialog; run progress + results table; **`Controller`** orchestrates services and uses `Platform.runLater()` for responsiveness.

---

## 4. Current codebase status (repository audit)

**In place**

- Gradle/Java 17/JavaFX wiring (`build.gradle`, `App.java` stub window).
- **Schema + JDBC:** `DatabaseService`, `SchemaInitializer`, DAOs (`ConfigurationDAO`, `ProjectDAO`, `StudentSubmissionDAO`, `EvaluationResultDAO`), JSON codec utilities.
- **Shared models:** `Project`, `LanguageConfig` (fields + accessors; **design-time behavior** such as `validate()`, `buildCompileCommand()`, `buildRunCommand()` still to land per Kuzey/Fatih integration).
- **`Enforcer`:** coherent process runner with timeout and output limits (matches design §5.2 / §7.6 risk mitigation).

**Missing or incomplete**

- **`controller/` package** described in README is **not** present yet; UI is still the stub `App`.
- **Services:** `ConfigurationService`, `ImportExportService`, `FileManager`, `ComparisonService`, `ReportingService` — **not** in tree as of this roadmap.
- **`Executioner`:** logic exists but **does not compile**: wrong imports (`model.*`, `service.*`), references `engine.Enforcer` instead of `com.ce316.iae.engine.Enforcer`, and depends on undefined service APIs. **`./gradlew build` fails** until imports/APIs are unified.

---

## 5. Delivery phases

Phases are ordered for **risk reduction**: restore a green build early, then integrate vertically toward “Run” end-to-end.

### Phase 0 — Integration hygiene (blocked work)

- Fix **`Executioner`** package/import references to `com.ce316.iae.model.*` and `com.ce316.iae.engine.*`.
- Introduce **minimal service interfaces** (or stubs) so `compileJava` succeeds and tests run—either temporary façade classes under `com.ce316.iae.service` or narrow `Executioner` until real implementations land.
- Ensure **`gradlew`** is executable in clones (`chmod +x gradlew`) so CI/teammates don’t hit permission issues.

**Exit criterion:** `./gradlew build` (and existing DB/DAO tests) pass on a clean clone.

### Phase 1 — Configuration manager + persistence (R4, R5, part of R3)

- Implement **`ConfigurationService`** against `ConfigurationDAO`: load/save to DB, in-memory map keyed by language name, validation hooks (`VALID` / `INVALID_PATH` / `INVALID_ARGS`).
- Extend **`LanguageConfig`** with **`validate()`**, **`buildCompileCommand(...)`**, **`buildRunCommand(workingDir, projectRunArgs)`** per §3 / §5.1 (interpreted languages: empty compile step).
- Implement **`ImportExportService`** JSON export/import with merge/replace and **`SkippedEntry`** / **`ImportResult`** reporting.

**Exit criterion:** CRUD configurations survive close/reopen of `.iae`; import/export round-trip documented in manual.

### Phase 2 — Project entity & wizard data (R3, R10)

- **`ProjectDAO`** + UI/controller flow: assignment name, selected `configuration_id`, expected output path, **tokenized `run_args`**, `compile_timeout_sec` / `run_timeout_sec`, `normalization_mode`.
- Wire **New/Open/Save** to `DatabaseService` (`README-db.md` lifecycle).

**Exit criterion:** Lecturer can author a project record in SQLite and reopen it with identical settings.

### Phase 3 — File & directory manager (R6)

- Implement **`FileManager`** using **`java.util.zip`**: scan directory, derive student ID from ZIP basename, extract to per-student folder, recursively resolve main source file name from configuration/extension.
- On Run: **`StudentSubmissionDAO.deleteAll()` + `insertAll()`** after `prepareSubmissions()` (per integration notes).

**Exit criterion:** Batch folder of ZIPs yields persisted `STUDENT_SUBMISSION` rows with populated paths.

### Phase 4 — Execution engine wiring (R7)

- Finalize **`Executioner`** loop to match §5.4: compile (if any) → run with merged args → hand stdout to comparison unless compile/timeout/error short-circuit.
- Align status taxonomy with **`ComparisonStatus`** / DB checks.

**Exit criterion:** Given mock submissions on disk, engine produces deterministic statuses without stopping the batch.

### Phase 5 — Evaluation & reporting (R8, R9)

- **`ComparisonService.compare(actual, expectedPath, mode)`** with line-level diff list on FAIL; ERROR when expected file missing.
- **`ReportingService`**: accumulate **`StudentReport`**, **`EvaluationResultDAO.deleteAll()` + `insertAll()`** after run; **`exportCSV`** + summary counts for UI.

**Exit criterion:** Results visible after reopening project; CSV export matches schema described in design §6.

### Phase 6 — JavaFX UI & controller (R2, R9, orchestration)

- Build screens per §7.3: main shell, **project wizard**, **configuration editor**, **import/export dialog**, **run** view (progress + log), **results** table with expandable detail + **Export CSV**.
- Implement **`Controller`** owning service references and threading rules (`Platform.runLater`).
- **`Help`** menu loads bundled HTML/PDF manual.

**Exit criterion:** Full demo scenario from course PDF runnable without IDE.

### Phase 7 — Packaging (R1)

- **`jlink` / `jpackage`** image including JavaFX modules per §7.1.
- **Inno Setup** `.exe`: install under Program Files, **desktop shortcut**, Start Menu entry; uninstall removes app files **only**, not lecturer `.iae` projects.

**Exit criterion:** Fresh Windows VM installs from installer and passes smoke checklist (open project, run batch, view results).

---

## 6. Vertical slices (milestones that prove integration)

These cut across modules and reduce late surprises:

1. **“Empty project lifecycle”** — create `.iae`, save, close, reopen (DB only).
2. **“One fake student”** — one ZIP, one language config, expected output on disk → one `EVALUATION_RESULT` row.
3. **“Broken ZIP resilience”** — mix valid/corrupt/missing-source ZIPs; batch completes with per-row ERROR/FAIL semantics.
4. **“Import configs on second machine”** — paths fail validation → skipped entries + manual fix story matches §3.4.

---

## 7. Testing strategy

- Keep expanding **`DaoRoundTripTest`**, **`DatabaseServiceTest`**, **`JsonArrayCodecTest`** for persistence regressions.
- Add unit tests for **`ComparisonService`** normalization matrix and diff behavior.
- Add **`FileManager`** tests with temporary ZIP trees (valid + corrupt).
- Add **`Executioner`** integration tests behind **`@Disabled`** if toolchain-dependent—or mock `Enforcer` with test doubles.

---

## 8. Risks & mitigations (from design §7.6 + engineering reality)

| Risk | Mitigation |
|------|------------|
| Unsigned installer triggers SmartScreen | Document for graders; optional future code signing |
| Paths with spaces / non-ASCII | Use `java.nio.file.Path`; avoid string concatenation for paths |
| Huge student stdout | Enforce **1 MB capture cap** (already in `Enforcer` spirit from design) |
| Compiler missing on lecturer PC | Validation warnings + clear ERROR/TIMEOUT reporting (partially started in `Executioner` messaging) |
| Parallel merge conflicts on shared models | **Do not rename model fields casually** — coordinate schema-first PRs (`README-db.md`) |

---

## 9. Documentation deliverables

- **User manual** (Req 2): installation, creating configs, creating projects, ZIP naming conventions, interpreting statuses, troubleshooting missing compilers.
- **Developer notes:** `README-db.md` remains the persistence contract; update root **`README.md`** when `controller/` and packaging scripts appear.

---

## 10. Suggested timeline (team-adjustable)

| Week focus | Outcome |
|------------|---------|
| Week 1 | Phase 0 green build; Phase 1 configuration CRUD + DB persistence |
| Week 2 | Phase 2–3 project wizard + ZIP pipeline persisting submissions |
| Week 3 | Phase 4–5 execution + comparison + persisted reports |
| Week 4 | Phase 6 full UI polish + Help |
| Week 5 | Phase 7 installer hardening, rehearsal install on clean Windows |

Adjust dates against your actual milestone deadlines from the course syllabus.

---

## 11. References in repo

- Course scenario & numbered requirements: `CE316 Project Description.pdf`
- Detailed architecture & UI plan: `ce316_design_document_team7.pdf`
- DB integration contract: `src/main/java/com/ce316/iae/db/README-db.md`
- Schema: `src/main/resources/db/schema.sql`
