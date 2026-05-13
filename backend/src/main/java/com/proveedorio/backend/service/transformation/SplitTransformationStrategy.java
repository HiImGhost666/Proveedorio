package com.proveedorio.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SplitTransformationStrategy implements TransformationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SplitTransformationStrategy.class);
    private final DirectTransformationStrategy directTransformationStrategy;

    public SplitTransformationStrategy(DirectTransformationStrategy directTransformationStrategy) {
        this.directTransformationStrategy = directTransformationStrategy;
    }

    @Override
    public JsonNode transform(JsonNode root, String config) {
        try {
            int bracketStart = config.indexOf('[');
            int bracketEnd = config.indexOf(']');
            if (bracketStart == -1 || bracketEnd == -1) return null;
            String fieldName = config.substring(0, bracketStart);
            int index = Integer.parseInt(config.substring(bracketStart + 1, bracketEnd));
            JsonNode target = directTransformationStrategy.transform(root, fieldName);
            if (target != null && target.isTextual()) {
                String[] parts = target.asText().split(";");
                if (index >= 0 && index < parts.length) {
                    return new TextNode(parts[index].trim());
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing SPLIT rule: {}", config);
        }
        return null;
    }

    @Override
    public String getStrategyName() {
        return "SPLIT";
    }
}

