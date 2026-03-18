package com.serverscope.api.metric;

public enum MetricType {
    SERVER_TPS(MetricValueType.DOUBLE_GAUGE),
    SERVER_MSPT(MetricValueType.DOUBLE_GAUGE),
    PLAYERS_ONLINE(MetricValueType.LONG_GAUGE),
    LOADED_CHUNKS(MetricValueType.LONG_GAUGE),
    ENTITY_COUNT(MetricValueType.LONG_GAUGE),
    BLOCK_ENTITY_COUNT(MetricValueType.LONG_GAUGE),
    JVM_HEAP_USED_BYTES(MetricValueType.LONG_GAUGE),
    JVM_HEAP_COMMITTED_BYTES(MetricValueType.LONG_GAUGE),
    JVM_HEAP_MAX_BYTES(MetricValueType.LONG_GAUGE),
    JVM_NON_HEAP_USED_BYTES(MetricValueType.LONG_GAUGE),
    JVM_GC_COLLECTION_COUNT(MetricValueType.LONG_COUNTER),
    JVM_GC_COLLECTION_TIME_MILLIS(MetricValueType.LONG_COUNTER);

    private final MetricValueType valueType;

    MetricType(MetricValueType valueType) {
        this.valueType = valueType;
    }

    public MetricValueType valueType() {
        return valueType;
    }
}
