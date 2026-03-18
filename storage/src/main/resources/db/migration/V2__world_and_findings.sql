CREATE TABLE IF NOT EXISTS world_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_time INTEGER NOT NULL,
    world_name TEXT NOT NULL,
    loaded_chunks INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_world_snapshots_time
    ON world_snapshots(sample_time DESC);

CREATE INDEX IF NOT EXISTS idx_world_snapshots_world_time
    ON world_snapshots(world_name, sample_time DESC);

CREATE TABLE IF NOT EXISTS analyzer_findings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_time INTEGER NOT NULL,
    finding_code TEXT NOT NULL,
    severity TEXT NOT NULL,
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    details_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analyzer_findings_time
    ON analyzer_findings(event_time DESC);

CREATE INDEX IF NOT EXISTS idx_analyzer_findings_code_subject
    ON analyzer_findings(finding_code, subject, event_time DESC);
