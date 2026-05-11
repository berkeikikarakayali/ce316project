# Integrated Assignment Environment (IAE)

CE316 Team #7 project — a Windows desktop app for batch-evaluating student programming assignments.

## Build & run

```sh
./gradlew build
./gradlew test
./gradlew run
```

Requires JDK 17 (Gradle will auto-download the toolchain).

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
├── App.java          // JavaFX entry point (stub; Kutay owns the real shell)
├── controller/       // Kutay — wires UI actions to services
├── db/               // Salih — DatabaseService, schema, JSON codec
├── dao/              // Salih — JDBC DAOs for the 4 tables
└── model/            // Shared plain data classes
src/main/resources/db/schema.sql
src/test/java/...     // JUnit 5 tests
```

See [db/README-db.md](src/main/java/com/ce316/iae/db/README-db.md) for integration details between the persistence layer and each teammate's module.
