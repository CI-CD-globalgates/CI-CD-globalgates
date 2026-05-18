package com.app.globalgates.common.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RagDocumentStatus {
    ACTIVE("active"), DELETED("deleted");

    private final String value;

    private static final Map<String, RagDocumentStatus> STATUS_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RagDocumentStatus::getValue, Function.identity()));

    @JsonCreator
    RagDocumentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RagDocumentStatus from(String value) {
        return STATUS_MAP.get(value);
    }
}
