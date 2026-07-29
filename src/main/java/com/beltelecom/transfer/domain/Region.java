package com.beltelecom.transfer.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Регионы Беларуси (код АСКР / области).
 */
public enum Region {

    BREST(1, "Брест", "Brest"),
    VITEBSK(2, "Витебск", "Vitebsk"),
    GOMEL(3, "Гомель", "Gomel"),
    GRODNO(4, "Гродно", "Grodno"),
    MINSK_REGION(5, "Минская область", "Minsk Region"),
    MOGILEV(6, "Могилев", "Mogilev"),
    MINSK(7, "Минск", "Minsk");

    private static final Map<Integer, Region> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Region::getId, Function.identity()));

    private final int id;
    private final String nameRu;
    private final String nameEn;

    Region(int id, String nameRu, String nameEn) {
        this.id = id;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
    }

    public int getId() {
        return id;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameEn() {
        return nameEn;
    }

    public static Optional<Region> findById(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Region requireById(int id) {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный регион: " + id));
    }
}
