package com.laberit.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laberit.backend.entity.Supplier;
import com.laberit.backend.entity.ProviderMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UniversalMapperService {

    private static final Logger logger = LoggerFactory.getLogger(UniversalMapperService.class);
    private static final Set<String> COMMERCIAL_FIELDS = Set.of("price", "stock", "val_unit", "q_avail", "pk", "id", "retail_price", "ean", "moq");
    private final ObjectMapper objectMapper;
    private final Map<String, TransformationStrategy> strategies;

    public UniversalMapperService(ObjectMapper objectMapper, List<TransformationStrategy> strategyList) {
        this.objectMapper = objectMapper;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TransformationStrategy::getStrategyName, Function.identity()));
    }

    /**
     * Normaliza la respuesta JSON cruda de un proveedor externo aplicando las reglas de mapeo configuradas.
     *
     * @param rawJson  JSON devuelto por el proveedor.
     * @param supplier proveedor para el que se procesan las reglas de mapeo.
     * @return lista de estructuras normalizadas listas para persistencia/uso interno.
     */
    public List<NormalizedData> normalizeResponse(String rawJson, Supplier supplier) {
        List<NormalizedData> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            Map<String, ProviderMapping> rules = supplier.getMappingConfigurations().stream()
                    .collect(Collectors.toMap(ProviderMapping::getInternalField, m -> m));

            JsonNode itemsNode = detectRootArray(root, rules.get("root_list"));

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    try {
                        NormalizedData normalized = mapSingleItem(item, rules);
                        if (normalized != null && normalized.mpn != null) {
                            results.add(normalized);
                        }
                    } catch (Exception e) {
                        logger.error("Error mapeando item individual del proveedor {}: {}", supplier.getName(), e.getMessage());
                    }
                }
            } else {
                logger.warn("No se detectó un array de items para el proveedor {}. Se intentará mapear el objeto raíz.", supplier.getName());
                NormalizedData normalized = mapSingleItem(root, rules);
                if (normalized != null && normalized.mpn != null) {
                    results.add(normalized);
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando JSON del proveedor {}: {}", supplier.getName(), e.getMessage());
        }
        return results;
    }

    private JsonNode detectRootArray(JsonNode root, ProviderMapping rootRule) {
        if (rootRule != null) {
            JsonNode itemsNode = applyTransformation(root, rootRule);
            if (itemsNode != null && itemsNode.isArray()) {
                return itemsNode;
            }
        }
        if (root.isArray()) return root;
        if (root.has("items") && root.get("items").isArray()) return root.get("items");
        if (root.has("products") && root.get("products").isArray()) return root.get("products");
        if (root.has("data") && root.get("data").isArray()) return root.get("data");
        return null;
    }

    private NormalizedData mapSingleItem(JsonNode node, Map<String, ProviderMapping> rules) {
        String mpn = extractString(node, rules.get("mpn"));
        String externalId = extractString(node, rules.get("id"));
        if (mpn == null) mpn = externalId;

        String brand = extractString(node, rules.get("brand"));
        String model = extractString(node, rules.get("model"));
        String category = extractString(node, rules.get("category"));

        Map<String, String> specs = new LinkedHashMap<>();
        extractConfiguredSpecs(node, rules, specs);
        extractAutoSpecs(node, rules, specs);
        String techSpecsJson = serializeSpecs(specs);

        BigDecimal price = extractDecimal(node, rules.get("price"));
        BigDecimal retailPrice = extractDecimal(node, rules.get("retail_price"));
        Integer stock = extractInteger(node, rules.get("stock"));
        String ean = extractString(node, rules.get("ean"));
        Integer moq = extractInteger(node, rules.get("moq"));
        String condition = extractString(node, rules.get("condition"));

        return new NormalizedData(mpn, brand, model, category, techSpecsJson, externalId, price, retailPrice, stock, ean, moq, condition);
    }

    private void extractConfiguredSpecs(JsonNode node, Map<String, ProviderMapping> rules, Map<String, String> specs) {
        rules.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("spec_"))
                .forEach(entry -> {
                    String specValue = extractString(node, entry.getValue());
                    if (specValue != null && !specValue.isBlank()) {
                        String rawKey = entry.getKey().substring("spec_".length());
                        String normalizedKey = normalizeSpecKey(rawKey);
                        specs.put(normalizedKey, specValue);
                    }
                });
    }

    private void extractAutoSpecs(JsonNode node, Map<String, ProviderMapping> rules, Map<String, String> specs) {
        Set<String> mappedExternalFields = new HashSet<>();
        for (ProviderMapping rule : rules.values()) {
            String path = rule.getExternalField();
            if (path != null) {
                mappedExternalFields.add(path.split("\\.")[0]);
                int bracketIndex = path.indexOf('[');
                if (bracketIndex > 0) {
                    mappedExternalFields.add(path.substring(0, bracketIndex));
                }
            }
        }

        node.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode childNode = entry.getValue();
            if (childNode.isValueNode() && !COMMERCIAL_FIELDS.contains(fieldName) && !mappedExternalFields.contains(fieldName)) {
                String normalizedKey = normalizeSpecKey(fieldName);
                if (!specs.containsKey(normalizedKey)) {
                    specs.put(normalizedKey, childNode.asText());
                }
            }
        });
    }

    private String normalizeSpecKey(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        input = input.toLowerCase();
        Pattern p = Pattern.compile("[_-]([a-zA-Z0-9])");
        Matcher m = p.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String serializeSpecs(Map<String, String> specs) {
        if (specs == null || specs.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(specs);
        } catch (JsonProcessingException e) {
            logger.error("Error serializando especificaciones a JSON", e);
            return "{}";
        }
    }

    private String extractString(JsonNode node, ProviderMapping rule) {
        if (rule == null) return null;
        JsonNode target = applyTransformation(node, rule);
        if (target == null || target.isNull()) return null;
        if (target.isContainerNode()) return target.toString();
        return target.asText();
    }

    private BigDecimal extractDecimal(JsonNode node, ProviderMapping rule) {
        if (rule == null) return null;
        JsonNode target = applyTransformation(node, rule);
        if (target != null && !target.isNull()) {
            try {
                return new BigDecimal(target.asText().replace(",", "."));
            } catch (Exception e) { return null; }
        }
        return null;
    }

    private Integer extractInteger(JsonNode node, ProviderMapping rule) {
        if (rule == null) return null;
        JsonNode target = applyTransformation(node, rule);
        if (target != null && !target.isNull()) {
            try {
                return target.asInt();
            } catch (Exception e) { return null; }
        }
        return null;
    }

    private JsonNode applyTransformation(JsonNode root, ProviderMapping rule) {
        String type = rule.getTransformationType() != null ? rule.getTransformationType().toUpperCase() : "DIRECT";
        TransformationStrategy strategy = strategies.getOrDefault(type, strategies.get("DIRECT"));
        return strategy.transform(root, rule.getExternalField());
    }

    public record NormalizedData(
        String mpn,
        String brand,
        String model,
        String category,
        String techSpecs,
        String externalId,
        BigDecimal price,
        BigDecimal retailPrice,
        Integer stock,
        String ean,
        Integer moq,
        String condition
    ) {}
}