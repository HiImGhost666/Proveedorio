package com.laberit.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FindInArrayTransformationStrategy implements TransformationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FindInArrayTransformationStrategy.class);
    private final DirectTransformationStrategy directTransformationStrategy;

    public FindInArrayTransformationStrategy(DirectTransformationStrategy directTransformationStrategy) {
        this.directTransformationStrategy = directTransformationStrategy;
    }

    @Override
    public JsonNode transform(JsonNode root, String config) {
        try {
            int bracketStart = config.indexOf('[');
            int bracketEnd = config.indexOf(']');
            if (bracketStart == -1 || bracketEnd == -1) return null;
            String arrayPath = config.substring(0, bracketStart);
            String condition = config.substring(bracketStart + 1, bracketEnd);
            String[] parts = condition.split("=");
            if (parts.length != 2) return null;
            String searchKey = parts[0].trim();
            String searchValue = parts[1].trim();
            JsonNode arrayNode = directTransformationStrategy.transform(root, arrayPath);
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    if (item.has(searchKey) && searchValue.equalsIgnoreCase(item.get(searchKey).asText())) {
                        if (item.has("val")) return item.get("val");
                        if (item.has("value")) return item.get("value");
                        return item;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing FIND_IN_ARRAY rule: {}", config);
        }
        return null;
    }

    @Override
    public String getStrategyName() {
        return "FIND_IN_ARRAY";
    }
}

