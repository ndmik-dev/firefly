package ua.ndmik.bot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import ua.ndmik.bot.model.yasno.AddressItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@Slf4j
public class YasnoClient {

    private static final String STREETS_PATH = "/streets";
    private static final String HOUSES_PATH = "/houses";
    private static final String GROUP_PATH = "/group";
    public static final String DEFAULT_SUBGROUP = ".1";

    private final RestClient restClient;
    private final JsonMapper mapper;

    public YasnoClient(@Qualifier("yasnoRestClient") RestClient restClient) {
        this.restClient = restClient;
        this.mapper = new JsonMapper();
    }

    public List<AddressItem> findStreets(int regionId, int dsoId, String query) {
        String response = executeRequest(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(STREETS_PATH)
                                .queryParam("regionId", regionId)
                                .queryParam("query", query)
                                .queryParam("dsoId", dsoId)
                                .build())
                        .retrieve()
                        .body(String.class),
                "streets",
                "regionId=%d,dsoId=%d,query=%s".formatted(regionId, dsoId, query)
        );
        return parseAddressItems(response);
    }

    public List<AddressItem> findHouses(int regionId, int dsoId, long streetId, String query) {
        String response = executeRequest(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(HOUSES_PATH)
                                .queryParam("regionId", regionId)
                                .queryParam("streetId", streetId)
                                .queryParam("query", query)
                                .queryParam("dsoId", dsoId)
                                .build())
                        .retrieve()
                        .body(String.class),
                "houses",
                "regionId=%d,dsoId=%d,streetId=%d,query=%s".formatted(regionId, dsoId, streetId, query)
        );
        return parseAddressItems(response);
    }

    public String findGroup(int regionId, int dsoId, long streetId, long houseId) {
        String response = executeRequest(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(GROUP_PATH)
                                .queryParam("regionId", regionId)
                                .queryParam("streetId", streetId)
                                .queryParam("houseId", houseId)
                                .queryParam("dsoId", dsoId)
                                .build())
                        .retrieve()
                        .body(String.class),
                "group",
                "regionId=%d,dsoId=%d,streetId=%d,houseId=%d".formatted(regionId, dsoId, streetId, houseId)
        );
        return extractGroupId(response);
    }

    private List<AddressItem> parseAddressItems(String json) {
        List<AddressItem> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            JsonNode root = readJsonRoot(json);
            if (root == null) {
                log.warn("Unexpected non-JSON YASNO address payload: {}", snippet(json));
                return result;
            }

            JsonNode itemsNode = firstArray(
                    root,
                    root.path("data"),
                    root.path("result"),
                    root.path("items"),
                    root.path("data").path("items")
            );
            if (!itemsNode.isArray()) {
                log.warn("Unexpected YASNO address payload: {}", json);
                return result;
            }

            for (JsonNode item : itemsNode) {
                long id = firstLong(item.path("id"), item.path("streetId"), item.path("houseId"));
                String displayName = firstText(
                        item.path("fullName"),
                        item.path("name"),
                        item.path("value"),
                        item.path("label")
                );
                if (id > 0 && !displayName.isBlank()) {
                    result.add(new AddressItem(id, displayName));
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to parse YASNO address payload");
            log.warn("YASNO address payload parse error: {}", json, ex);
        }
        return result;
    }

    private String extractGroupId(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }

        try {
            JsonNode root = readJsonRoot(json);
            if (root == null) {
                log.warn("Unexpected non-JSON YASNO group payload: {}", snippet(json));
                return "";
            }
            String compositeFromRoot = groupWithSubgroup(root);
            if (!compositeFromRoot.isBlank()) {
                return compositeFromRoot;
            }

            String compositeFromData = groupWithSubgroup(root.path("data"));
            if (!compositeFromData.isBlank()) {
                return compositeFromData;
            }

            JsonNode candidate = firstPresent(
                    root.path("groupId"),
                    root.path("group"),
                    root.path("data").path("groupId"),
                    root.path("data").path("group")
            );

            if (candidate == null || candidate.isMissingNode() || candidate.isNull()) {
                log.warn("Unexpected YASNO group payload: {}", json);
                return "";
            }
            return text(candidate).trim();
        } catch (RuntimeException ex) {
            log.warn("Failed to parse YASNO group payload");
            log.warn("YASNO group payload parse error: {}", json, ex);
            return "";
        }
    }

    private String executeRequest(Supplier<String> request, String operation, String params) {
        try {
            return Objects.toString(request.get(), "");
        } catch (RuntimeException ex) {
            log.warn("YASNO {} request failed ({})", operation, params);
            log.warn("YASNO {} request error", operation, ex);
            return "";
        }
    }

    private JsonNode firstPresent(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            boolean isDataPresent = candidate != null
                    && !candidate.isMissingNode()
                    && !candidate.isNull()
                    && !text(candidate).isBlank();
            if (isDataPresent) {
                return candidate;
            }
        }
        return null;
    }

    private String groupWithSubgroup(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        JsonNode groupNode = firstPresent(node.path("groupId"), node.path("group"));
        if (groupNode == null || groupNode.isMissingNode() || groupNode.isNull()) {
            return "";
        }
        String group = text(groupNode).trim();
        if (group.isBlank()) {
            return "";
        }

        JsonNode subgroupNode = firstPresent(node.path("subgroup"), node.path("subGroup"));
        if (subgroupNode == null || subgroupNode.isMissingNode() || subgroupNode.isNull()) {
            return group.contains(".") ? group : group + DEFAULT_SUBGROUP;
        }

        String subgroup = text(subgroupNode).trim();
        if (group.contains(".")) {
            return group;
        }
        if (subgroup.isBlank()) {
            return group + DEFAULT_SUBGROUP;
        }

        return group + "." + subgroup;
    }

    private JsonNode firstArray(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }
        return null;
    }

    private long firstLong(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate == null || candidate.isMissingNode() || candidate.isNull()) {
                continue;
            }
            long value = candidate.asLong(0L);
            if (value > 0L) {
                return value;
            }
        }
        return 0L;
    }

    private String firstText(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            String value = text(candidate);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private JsonNode readJsonRoot(String payload) {
        String normalized = normalizeJson(payload);
        if (normalized.isBlank()) {
            return null;
        }
        return mapper.readTree(normalized);
    }

    private String normalizeJson(String payload) {
        if (payload == null) {
            return "";
        }
        String trimmed = payload.stripLeading();
        if (trimmed.isBlank()) {
            return "";
        }

        char first = trimmed.charAt(0);
        if (first == '{' || first == '[') {
            return trimmed;
        }
        if (first == '<') {
            return "";
        }

        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int startIndex = selectStartIndex(objectStart, arrayStart);
        if (startIndex < 0) {
            return "";
        }

        String prefix = trimmed.substring(0, startIndex).trim();
        if (prefix.startsWith(")]}',") || prefix.startsWith("for(;;);")) {
            return trimmed.substring(startIndex);
        }
        return "";
    }

    private int selectStartIndex(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }

    private String snippet(String payload) {
        if (payload == null) {
            return "";
        }
        String normalized = payload.replaceAll("\\s+", " ").trim();
        int maxLength = 240;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asString();
    }
}
