# Integrated Assignment Environment (IAE)

CE316 Team #7 project — a Windows desktop app for batch-evaluating student programming assignments.

## Build & run

```sh
./gradlew build
./gradlew test
./gradlew run
```

Requires JDK 17 (Gradle will auto-download the toolchain).

## Submission checklist (CE316)

- [ ] `./gradlew clean test build` passes on your machine (JDK 17).
- [ ] **Req 1:** Build Windows installer (`jpackage` + Inno Setup) and smoke-test on a clean Windows VM/lab PC.
- [ ] **Req 2:** Confirm **Help → User manual** opens `manual/manual.html`.
- [ ] **Req 3–10:** Walk through design-doc scenario (configs, ZIP batch, compile/run, compare, save/reopen project, CSV export).

## Packaging (Windows / Req 1)

Ship JavaFX with **`jpackage`** (JDK 17+), then wrap the resulting folder using **[Inno Setup](https://jrsoftware.org/isinfo.php)** so graders get a desktop shortcut—matching design §7.1.

## Module ownership

| Module | Owner | Design doc § |
|---|---|---|
| System & Database | Salih | §2 |
| Configuration Manager | Kuzey | §3 |
| File & Directory Manager | Arda | §4 |
| Execution Engine | Fatih | §5 |
| Evaluation & Reporting | Berke | §6 |
| UI & Deployment | Kutay | §7 |

## Layout

```
src/main/java/com/ce316/iae/
├── App.java       // JavaFX shell — menus, project/run wiring (UI & Deployment)
├── db/            // DatabaseService, schema init, JSON codec
├── dao/           // JDBC DAOs (configuration / project / submissions / evaluation results)
├── engine/        // Executioner + Enforcer (ProcessBuilder + timeouts)
├── model/         // Plain Java beans + enums + validation helpers
├── service/       // Configuration, import/export, ZIP ingestion, comparison, reporting
├── util/          // RunArgsCodec utilities
└── (tests under src/test/java)
src/main/resources/db/schema.sql
src/main/resources/manual/manual.html   // Help → manual (Req 2)
```

See [db/README-db.md](src/main/java/com/ce316/iae/db/README-db.md) for integration details between the persistence layer and each teammate's module.
