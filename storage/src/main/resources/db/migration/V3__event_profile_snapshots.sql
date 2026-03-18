CREATE TABLE IF NOT EXISTS event_profile_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_time INTEGER NOT NULL,
    snapshot_kind TEXT NOT NULL,
    event_id TEXT NOT NULL,
    event_class_name TEXT NOT NULL,
    count INTEGER NOT NULL,
    total_time_nanos INTEGER NOT NULL,
    max_time_nanos INTEGER NOT NULL,
    average_time_nanos INTEGER NOT NULL,
    max_window_count INTEGER NOT NULL,
    burst_score REAL NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_profile_snapshots_time
    ON event_profile_snapshots(sample_time DESC);

CREATE INDEX IF NOT EXISTS idx_event_profile_snapshots_kind_event
    ON event_profile_snapshots(snapshot_kind, event_id, sample_time DESC);
