package ua.ndmik.bot.util;

import java.util.Optional;

public final class AddressQueryParser {

    private AddressQueryParser() {
    }

    public static Optional<AddressQuery> parse(String rawAddress) {
        if (rawAddress == null) {
            return Optional.empty();
        }

        String normalized = rawAddress.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        int commaIndex = normalized.lastIndexOf(',');
        if (commaIndex > 0 && commaIndex < normalized.length() - 1) {
            return buildAddressQuery(
                    normalized.substring(0, commaIndex),
                    normalized.substring(commaIndex + 1)
            );
        }

        int lastSpaceIndex = normalized.lastIndexOf(' ');
        if (lastSpaceIndex <= 0 || lastSpaceIndex >= normalized.length() - 1) {
            return Optional.empty();
        }

        return buildAddressQuery(
                normalized.substring(0, lastSpaceIndex),
                normalized.substring(lastSpaceIndex + 1)
        );
    }

    private static Optional<AddressQuery> buildAddressQuery(String streetPart, String housePart) {
        String street = streetPart.trim();
        String house = housePart.trim();
        if (street.isBlank() || house.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new AddressQuery(street, house));
    }

    public record AddressQuery(String streetQuery, String houseQuery) {
    }
}
