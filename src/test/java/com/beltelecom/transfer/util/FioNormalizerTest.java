package com.beltelecom.transfer.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FioNormalizerTest {

    @Test
    void shouldTreatYoAndYeAsEqual() {
        assertThat(FioNormalizer.normalize("Королёва Алёна")).isEqualTo(FioNormalizer.normalize("Королева Алена"));
        assertThat(FioNormalizer.normalize("Ёлкин")).isEqualTo(FioNormalizer.normalize("елкин"));
    }

    @Test
    void shouldCollapseSpacesAndIgnoreCase() {
        assertThat(FioNormalizer.normalize("  Иванов   ИВАН  "))
                .isEqualTo(FioNormalizer.normalize("иванов иван"));
    }
}
