package com.datapath.integration.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.lang.Nullable;

import java.time.ZonedDateTime;

public class JsonUtils {

    @Nullable
    public static String getString(JsonNode node, String path) {
        JsonNode targetNode = node.at(path);
        return targetNode.isMissingNode() || targetNode.isNull() ?
                null : targetNode.asText();
    }

    @Nullable
    public static Double getDouble(JsonNode node, String path) {
        JsonNode targetNode = node.at(path);
        return !isNumber(targetNode) ? null : targetNode.asDouble();
    }

    @Nullable
    public static Integer getInt(JsonNode node, String path) {
        JsonNode targetNode = node.at(path);
        return !isNumber(targetNode) || targetNode.isLong() ? null : targetNode.asInt();
    }

    @Nullable
    public static Long getLong(JsonNode node, String path) {
        JsonNode targetNode = node.at(path);
        return !isNumber(targetNode) ? null : targetNode.asLong();
    }

    @Nullable
    public static ZonedDateTime getDate(JsonNode node, String path) {
        JsonNode targetNode = node.at(path);
        return targetNode.isMissingNode() || targetNode.isNull() ?
                null : DateUtils.parseZonedDateTime(targetNode.asText());
    }

    public static boolean isNumber(JsonNode node) {
        return !(node.isMissingNode() || node.isNull() || (!node.isDouble() && !node.isInt() && !node.isLong()));
    }
}
