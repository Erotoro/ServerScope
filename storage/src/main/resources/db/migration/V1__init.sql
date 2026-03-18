CREATE TABLE IF NOT EXISTS metric_samples (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_time INTEGER NOT NULL,
    tps REAL NOT NULL,
    mspt REAL NOT NULL,
    heap_used_bytes INTEGER NOT NULL,
    online_players INTEGER NOT NULL,
    world_count INTEGER NOT NULL,
    loaded_chunks INTEGER NOT NULL,
    total_entities INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_metric_samples_time
    ON metric_samples(sample_time DESC);

CREATE TABLE IF NOT EXISTS alerts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_time INTEGER NOT NULL,
    alert_code TEXT NOT NULL,
    severity TEXT NOT NULL,
    status TEXT NOT NULL,
    dedupe_key TEXT NOT NULL,
    message TEXT NOT NULL,
    dimensions_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alerts_event_time
    ON alerts(event_time DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_code_dedupe
    ON alerts(alert_code, dedupe_key, event_time DESC);

CREATE TABLE IF NOT EXISTS plugin_profile_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_time INTEGER NOT NULL,
    plugin_name TEXT NOT NULL,
    event_name TEXT NOT NULL,
    listener_class TEXT NOT NULL,
    calls_count INTEGER NOT NULL,
    total_time_nanos INTEGER NOT NULL,
    max_time_nanos INTEGER NOT NULL,
    p95_time_nanos INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_plugin_profile_snapshots_time
    ON plugin_profile_snapshots(sample_time DESC);

CREATE INDEX IF NOT EXISTS idx_plugin_profile_snapshots_plugin_event
    ON plugin_profile_snapshots(plugin_name, event_name, sample_time DESC);

CREATE TABLE IF NOT EXISTS chunk_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_time INTEGER NOT NULL,
    world_name TEXT NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    entity_count INTEGER NOT NULL,
    block_entity_count INTEGER NOT NULL,
    hotspot_score INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chunk_snapshots_time
    ON chunk_snapshots(sample_time DESC);

CREATE INDEX IF NOT EXISTS idx_chunk_snapshots_world_chunk
    ON chunk_snapshots(world_name, chunk_x, chunk_z, sample_time DESC);
