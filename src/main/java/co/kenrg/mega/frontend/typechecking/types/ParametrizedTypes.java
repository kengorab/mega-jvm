package co.kenrg.mega.frontend.typechecking.types;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

public class ParametrizedTypes {
    public static final Function<List<MegaType>, MegaType> arrayOf = (typeArgs) -> new ArrayType(typeArgs.get(0));

    private static final Map<String, Function<List<MegaType>, MegaType>> ALL = ImmutableMap.of(
        "Array", arrayOf
    );

    public static Optional<Function<List<MegaType>, MegaType>> byDisplayName(String name) {
        return Optional.ofNullable(ALL.get(name));
    }
}
