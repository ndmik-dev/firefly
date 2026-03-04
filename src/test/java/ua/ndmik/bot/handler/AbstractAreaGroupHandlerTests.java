package ua.ndmik.bot.handler;

import org.junit.jupiter.api.Test;
import ua.ndmik.bot.model.DtekArea;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAreaGroupHandlerTests {

    @Test
    void isSelectedForArea_returnsTrueWhenGroupAndAreaMatch() {
        boolean selected = AbstractAreaGroupHandler.isSelectedForArea("1.1", "1.1", DtekArea.KYIV_REGION, DtekArea.KYIV_REGION);

        assertThat(selected).isTrue();
    }

    @Test
    void isSelectedForArea_returnsFalseWhenAreaDiffers() {
        boolean selected = AbstractAreaGroupHandler.isSelectedForArea("1.1", "1.1", DtekArea.KYIV_REGION, DtekArea.KYIV);

        assertThat(selected).isFalse();
    }

    @Test
    void isSelectedForArea_returnsFalseWhenGroupDiffers() {
        boolean selected = AbstractAreaGroupHandler.isSelectedForArea("2.1", "1.1", DtekArea.KYIV, DtekArea.KYIV);

        assertThat(selected).isFalse();
    }
}
