package ua.ndmik.bot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class YasnoClient {

    private static final String BASE_URL = "https://app.yasno.ua/api/blackout-service/public/shutdowns/addresses/v2";
    private static final String STREETS_PATH = "/streets";
    private static final String HOUSES_PATH = "/houses";
    private static final String GROUP_PATH = "/group";

    private final RestClient restClient;
    private final JsonMapper mapper;

    public YasnoClient() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        this.mapper = new JsonMapper();
    }

    public List<AddressItem> findStreets(int regionId, int dsoId, String query) {
        String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(STREETS_PATH)
                        .queryParam("regionId", regionId)
                        .queryParam("query", query)
                        .queryParam("dsoId", dsoId)
                        .build())
                .retrieve()
                .body(String.class);
        return parseAddressItems(response);
    }

    public List<AddressItem> findHouses(int regionId, int dsoId, long streetId, String query) {
        String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(HOUSES_PATH)
                        .queryParam("regionId", regionId)
                        .queryParam("streetId", streetId)
                        .queryParam("query", query)
                        .queryParam("dsoId", dsoId)
                        .build())
                .retrieve()
                .body(String.class);
        return parseAddressItems(response);
    }

    public String findGroup(int regionId, int dsoId, long streetId, long houseId) {
        String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(GROUP_PATH)
                        .queryParam("regionId", regionId)
                        .queryParam("streetId", streetId)
                        .queryParam("houseId", houseId)
                        .queryParam("dsoId", dsoId)
                        .build())
                .retrieve()
                .body(String.class);
        return extractGroupId(response);
    }

    private List<AddressItem> parseAddressItems(String json) {
        List<AddressItem> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        JsonNode root = mapper.readTree(json);
        JsonNode itemsNode = root.isArray() ? root : root.path("data");
        if (!itemsNode.isArray()) {
            log.debug("Unexpected YASNO address payload: {}", json);
            return result;
        }

        for (JsonNode item : itemsNode) {
            long id = item.path("id").asLong();
            String name = item.path("name").asString();
            String fullName = item.path("fullName").asString();
            String displayName = !fullName.isBlank() ? fullName : name;
            if (id > 0) {
                result.add(new AddressItem(id, displayName));
            }
        }
        return result;
    }

    private String extractGroupId(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }

        JsonNode root = mapper.readTree(json);
        JsonNode candidate = firstPresent(
                root.path("group"),
                root.path("groupId"),
                root.path("data").path("group"),
                root.path("data").path("groupId")
        );

        if (candidate == null || candidate.isMissingNode() || candidate.isNull()) {
            log.debug("Unexpected YASNO group payload: {}", json);
            return "";
        }
        return text(candidate).trim();
    }

    private JsonNode firstPresent(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && !candidate.isMissingNode() && !candidate.isNull() && !text(candidate).isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asString();
    }

    public record AddressItem(long id, String name) {
    }
}
