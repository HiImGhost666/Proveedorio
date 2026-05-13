package com.laberit.backend.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface TransformationStrategy {
    JsonNode transform(JsonNode root, String path);
    String getStrategyName();
}

