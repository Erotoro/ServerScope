package com.serverscope.storage.sqlite;

import com.serverscope.api.config.StorageConfig;
import com.serverscope.api.storage.AlertRecord;
import com.serverscope.api.storage.AlertSeverity;
import com.serverscope.api.storage.AlertStatus;
import com.serverscope.api.storage.AnalyzerFindingRecord;
import com.serverscope.api.storage.AnalyzerFindingSeverity;
import com.serverscope.api.storage.ChunkSnapshot;
import com.serverscope.api.storage.EventProfileSnapshot;
import com.serverscope.api.storage.MetricSample;
import com.serverscope.api.storage.PluginProfileSnapshot;
import com.serverscope.api.storage.WorldSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteStorageServiceIntegrationTest {
    @Test
    void writesAndReadsBatchedData() throws Exception {
        Path tempDirectory = Files.createTempDirectory("serverscope-storage-test");
        Path sqliteFile = tempDirectory.resolve("serverscope.db");
        StorageConfig config = new StorageConfig(sqliteFile, true, 128, 64, 100, 30);

        SqliteStorageService service = new SqliteStorageService(Logger.getLogger("storage-test"), config);
        service.start();

        assertTrue(service.enqueueMetricSample(new MetricSample(Instant.now(), 19.95, 12.5, 123456L, 5, 3, 240L, 900L)));
        assertTrue(service.enqueueAlert(new AlertRecord(
                Instant.now(),
                "LOW_TPS",
                AlertSeverity.WARN,
                AlertStatus.ACTIVE,
                "server",
                "TPS below threshold",
                "{\"scope\":\"server\"}"
        )));
        assertTrue(service.enqueuePluginProfileSnapshot(new PluginProfileSnapshot(
                Instant.now(),
                "ExamplePlugin",
                "PlayerMoveEvent",
                "com.example.Listener",
                100L,
                5_000_000L,
                120_000L,
                90_000L
        )));
        assertTrue(service.enqueueWorldSnapshot(new WorldSnapshot(
                Instant.now(),
                "world",
                240L
        )));
        assertTrue(service.enqueueEventProfileSnapshot(new EventProfileSnapshot(
                Instant.now(),
                "BURST",
                "player_move",
                "org.bukkit.event.player.PlayerMoveEvent",
                1000L,
                250_000_000L,
                2_000_000L,
                250_000L,
                320L,
                3.2d
        )));
        assertTrue(service.enqueueChunkSnapshot(new ChunkSnapshot(
                Instant.now(),
                "world",
                10,
                20,
                48,
                6,
                1_000L
        )));
        assertTrue(service.enqueueAnalyzerFinding(new AnalyzerFindingRecord(
                Instant.now(),
                "TOP_PLUGIN",
                AnalyzerFindingSeverity.WARN,
                "ExamplePlugin",
                "Plugin is heavy",
                "{\"plugin\":\"ExamplePlugin\"}"
        )));

        service.flush();
        service.stop();

        SqliteStorageService readService = new SqliteStorageService(Logger.getLogger("storage-test"), config);
        readService.start();
        assertEquals(1, readService.findLatestMetricSamples(10).size());
        assertEquals(1, readService.findMetricSamplesSince(Instant.now().minusSeconds(3600), 10).size());
        assertTrue(readService.findMetricSamplesSince(Instant.now().plusSeconds(3600), 10).isEmpty());
        assertEquals(1, readService.findLatestAlerts(10).size());
        assertEquals(1, readService.findLatestPluginProfileSnapshots(10).size());
        assertEquals(1, readService.findLatestEventProfileSnapshots(10).size());
        assertEquals(1, readService.findLatestWorldSnapshots(10).size());
        assertEquals(1, readService.findLatestChunkSnapshots(10).size());
        assertEquals(1, readService.findLatestAnalyzerFindings(10).size());
        readService.stop();
    }

    @Test
    void findMetricSamplesSinceReturnsAscendingWindowWithLimit() throws Exception {
        Path tempDirectory = Files.createTempDirectory("serverscope-history-test");
        Path sqliteFile = tempDirectory.resolve("serverscope.db");
        StorageConfig config = new StorageConfig(sqliteFile, true, 128, 64, 100, 30);

        SqliteStorageService service = new SqliteStorageService(Logger.getLogger("history-test"), config);
        service.start();

        Instant base = Instant.now().minusSeconds(600);
        for (int i = 0; i < 5; i++) {
            assertTrue(service.enqueueMetricSample(new MetricSample(
                    base.plusSeconds(i * 10L), 20.0 - i, 10.0 + i, 1_000L * i, i, 1, 10L * i, 100L * i)));
        }
        service.flush();
        service.stop();

        SqliteStorageService readService = new SqliteStorageService(Logger.getLogger("history-test"), config);
        readService.start();

        var window = readService.findMetricSamplesSince(base.minusSeconds(1), 10);
        assertEquals(5, window.size());
        for (int i = 1; i < window.size(); i++) {
            assertTrue(!window.get(i).sampleTime().isBefore(window.get(i - 1).sampleTime()),
                    "history must be ascending by sample time");
        }

        // When limited, keep the most recent rows but still return them ascending.
        var limited = readService.findMetricSamplesSince(base.minusSeconds(1), 2);
        assertEquals(2, limited.size());
        assertEquals(base.plusSeconds(30).toEpochMilli(), limited.get(0).sampleTime().toEpochMilli());
        assertEquals(base.plusSeconds(40).toEpochMilli(), limited.get(1).sampleTime().toEpochMilli());

        readService.stop();
    }
}
