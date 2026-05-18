package com.app.globalgates.common.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RagProcessStatus {
    PENDING("pending"), PROCESSING("processing"), COMPLETED("completed"), FAILED("failed");

    private final String value;

    private static final Map<String, RagProcessStatus> STATUS_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RagProcessStatus::getValue, Function.identity()));

    @JsonCreator
    RagProcessStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RagProcessStatus from(String value) {
        return STATUS_MAP.get(value);
    }
}
