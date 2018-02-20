package co.kenrg.mega.utils;

import com.google.common.collect.LinkedHashMultimap;

public class LinkedHashMultimaps {
    public static <T, U> LinkedHashMultimap<T, U> of() {
        return LinkedHashMultimap.create();
    }

    public static <T, U> LinkedHashMultimap<T, U> of(
        T k1, U v1
    ) {
        LinkedHashMultimap<T, U> map = LinkedHashMultimap.create();
        map.put(k1, v1);
        return map;
    }

    public static <T, U> LinkedHashMultimap<T, U> of(
        T k1, U v1,
        T k2, U v2
    ) {
        LinkedHashMultimap<T, U> map = LinkedHashMultimap.create();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <T, U> LinkedHashMultimap<T, U> of(
        T k1, U v1,
        T k2, U v2,
        T k3, U v3
    ) {
        LinkedHashMultimap<T, U> map = LinkedHashMultimap.create();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
