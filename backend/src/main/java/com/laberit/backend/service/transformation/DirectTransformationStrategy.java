package com.laberit.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class DirectTransformationStrategy implements TransformationStrategy {

    @Override
    public JsonNode transform(JsonNode root, String path) {
        return navigate(root, path);
    }

    @Override
    public String getStrategyName() {
        return "DIRECT";
    }

    private JsonNode navigate(JsonNode node, String path) {
        if (path == null || path.isEmpty()) return node;
        String[] keys = path.split("\\.");
        JsonNode current = node;
        for (String key : keys) {
            if (key.isEmpty()) continue;
            current = current.path(key);
            if (current.isMissingNode()) return null;
        }
        return current;
    }
}

