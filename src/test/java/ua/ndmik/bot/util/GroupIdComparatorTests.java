package ua.ndmik.bot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupIdComparatorTests {

    @Test
    void sort_ordersDottedIdsByNumericParts() {
        List<String> sorted = List.of("1.1", "10.1", "11.1", "12.1", "2.1", "3.1")
                .stream()
                .sorted(GroupIdComparator.INSTANCE)
                .toList();

        assertThat(sorted).containsExactly("1.1", "2.1", "3.1", "10.1", "11.1", "12.1");
    }

    @Test
    void sort_comparesEachPartNumerically() {
        List<String> sorted = List.of("1.10", "1.2", "1.2.1", "1.2.0")
                .stream()
                .sorted(GroupIdComparator.INSTANCE)
                .toList();

        assertThat(sorted).containsExactly("1.2", "1.2.0", "1.2.1", "1.10");
    }
}
