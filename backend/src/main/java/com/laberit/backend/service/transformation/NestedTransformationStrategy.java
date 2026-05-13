package com.laberit.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class NestedTransformationStrategy implements TransformationStrategy {

    private final DirectTransformationStrategy directTransformationStrategy;

    public NestedTransformationStrategy(DirectTransformationStrategy directTransformationStrategy) {
        this.directTransformationStrategy = directTransformationStrategy;
    }

    @Override
    public JsonNode transform(JsonNode root, String path) {
        return directTransformationStrategy.transform(root, path);
    }

    @Override
    public String getStrategyName() {
        return "NESTED";
    }
}

