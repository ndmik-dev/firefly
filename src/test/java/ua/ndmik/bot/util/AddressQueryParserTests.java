package ua.ndmik.bot.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AddressQueryParserTests {

    @Test
    void parse_splitsAddressByComma() {
        Optional<AddressQueryParser.AddressQuery> parsed = AddressQueryParser.parse("вул. Хрещатик, 22");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().streetQuery()).isEqualTo("вул. Хрещатик");
        assertThat(parsed.get().houseQuery()).isEqualTo("22");
    }

    @Test
    void parse_splitsAddressByLastSpaceWhenCommaMissing() {
        Optional<AddressQueryParser.AddressQuery> parsed = AddressQueryParser.parse("вул. Богдана Хмельницького 11Б");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().streetQuery()).isEqualTo("вул. Богдана Хмельницького");
        assertThat(parsed.get().houseQuery()).isEqualTo("11Б");
    }

    @Test
    void parse_returnsEmptyWhenAddressCannotBeSplit() {
        Optional<AddressQueryParser.AddressQuery> parsed = AddressQueryParser.parse("Хрещатик");

        assertThat(parsed).isEmpty();
    }
}
