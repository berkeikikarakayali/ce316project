-- IAE project database schema (design doc Section 2.1)
-- Each .iae file is one SQLite database containing exactly one project.

CREATE TABLE IF NOT EXISTS meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS CONFIGURATION (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    language_name   TEXT NOT NULL UNIQUE,
    file_extension  TEXT NOT NULL,
    compiler_path   TEXT,
    compile_args    TEXT NOT NULL DEFAULT '[]',
    run_args        TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS PROJECT (
    id                   INTEGER PRIMARY KEY CHECK (id = 1),
    name                 TEXT,
    configuration_id     INTEGER,
    expected_output_path TEXT,
    run_args             TEXT,
    compile_timeout_sec  INTEGER NOT NULL DEFAULT 60,
    run_timeout_sec      INTEGER NOT NULL DEFAULT 30,
    normalization_mode   TEXT NOT NULL DEFAULT 'STRICT'
        CHECK (normalization_mode IN ('STRICT', 'TRIM_WHITESPACE', 'CASE_INSENSITIVE')),
    FOREIGN KEY (configuration_id) REFERENCES CONFIGURATION(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS STUDENT_SUBMISSION (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id             TEXT NOT NULL,
    zip_file_path          TEXT NOT NULL,
    extracted_folder_path  TEXT,
    main_source_file       TEXT
);

CREATE TABLE IF NOT EXISTS EVALUATION_RESULT (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    student_submission_id INTEGER NOT NULL,
    status                TEXT NOT NULL
        CHECK (status IN ('PASS', 'FAIL', 'ERROR', 'COMPILE_ERROR', 'TIMEOUT')),
    actual_output         TEXT,
    expected_output       TEXT,
    diff_lines            TEXT NOT NULL DEFAULT '[]',
    error_message         TEXT,
    normalization_mode    TEXT,
    timestamp             TEXT NOT NULL,
    FOREIGN KEY (student_submission_id) REFERENCES STUDENT_SUBMISSION(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_submission_student ON STUDENT_SUBMISSION(student_id);
CREATE INDEX IF NOT EXISTS idx_result_submission  ON EVALUATION_RESULT(student_submission_id);
