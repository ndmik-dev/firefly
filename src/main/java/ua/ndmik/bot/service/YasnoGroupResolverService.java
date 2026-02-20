package ua.ndmik.bot.service;

import org.springframework.stereotype.Service;
import ua.ndmik.bot.client.YasnoClient;
import ua.ndmik.bot.client.YasnoClient.AddressItem;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static ua.ndmik.bot.util.Constants.KYIV_DSO_ID;
import static ua.ndmik.bot.util.Constants.KYIV_REGION_ID;

@Service
public class YasnoGroupResolverService {

    private final YasnoClient yasnoClient;

    public YasnoGroupResolverService(YasnoClient yasnoClient) {
        this.yasnoClient = yasnoClient;
    }

    public Optional<ResolvedYasnoGroup> resolve(String streetQuery, String houseQuery) {
        List<AddressItem> streets = yasnoClient.findStreets(KYIV_REGION_ID, KYIV_DSO_ID, streetQuery);
        Optional<AddressItem> street = pickBest(streets, streetQuery);
        if (street.isEmpty()) {
            return Optional.empty();
        }

        List<AddressItem> houses = yasnoClient.findHouses(KYIV_REGION_ID, KYIV_DSO_ID, street.get().id(), houseQuery);
        Optional<AddressItem> house = pickBest(houses, houseQuery);
        if (house.isEmpty()) {
            return Optional.empty();
        }

        String groupId = yasnoClient.findGroup(KYIV_REGION_ID, KYIV_DSO_ID, street.get().id(), house.get().id());
        if (groupId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedYasnoGroup(
                KYIV_REGION_ID,
                KYIV_DSO_ID,
                street.get().id(),
                street.get().name(),
                house.get().id(),
                house.get().name(),
                groupId
        ));
    }

    private Optional<AddressItem> pickBest(List<AddressItem> items, String query) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return Optional.of(items.getFirst());
        }

        return items.stream()
                .filter(item -> normalize(item.name()).contains(normalizedQuery))
                .findFirst()
                .or(() -> items.stream()
                        .filter(item -> normalize(item.name()).equals(normalizedQuery))
                        .findFirst())
                .or(() -> Optional.of(items.getFirst()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("вул.", "")
                .replace("улица", "")
                .replace("street", "")
                .trim();
    }

    public record ResolvedYasnoGroup(
            int regionId,
            int dsoId,
            long streetId,
            String streetName,
            long houseId,
            String houseName,
            String groupId
    ) {
    }
}
