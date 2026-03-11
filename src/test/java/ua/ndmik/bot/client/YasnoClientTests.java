package ua.ndmik.bot.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import ua.ndmik.bot.model.yasno.AddressItem;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class YasnoClientTests {

    @Test
    void findStreets_parsesValueFieldFromCurrentApiSchema() {
        YasnoClient client = clientWithResponse("[{\"id\":1064,\"value\":\"вул. Хрещатик\"}]");

        List<AddressItem> streets = client.findStreets(25, 902, "хре");

        assertThat(streets).containsExactly(new AddressItem(1064L, "вул. Хрещатик"));
    }

    @Test
    void findStreets_parsesXssiPrefixedPayload() {
        YasnoClient client = clientWithResponse(")]}',\n[{\"id\":1064,\"value\":\"вул. Хрещатик\"}]");

        List<AddressItem> streets = client.findStreets(25, 902, "хре");

        assertThat(streets).containsExactly(new AddressItem(1064L, "вул. Хрещатик"));
    }

    @Test
    void findStreets_ignoresNonJsonPayload() {
        YasnoClient client = clientWithResponse("<html>blocked</html>");

        List<AddressItem> streets = client.findStreets(25, 902, "хре");

        assertThat(streets).isEmpty();
    }

    @Test
    void findGroup_extractsNumericGroup() {
        YasnoClient client = clientWithResponse("{\"group\":20,\"subgroup\":1}");

        String group = client.findGroup(25, 902, 1064, 17211);

        assertThat(group).isEqualTo("20");
    }

    @SuppressWarnings("unchecked")
    private YasnoClient clientWithResponse(String responseBody) {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        given(restClient.get()
                .uri(any(Function.class))
                .retrieve()
                .body(String.class))
                .willReturn(responseBody);
        return new YasnoClient(restClient);
    }
}
