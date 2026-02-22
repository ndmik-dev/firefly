package ua.ndmik.bot.model;

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
