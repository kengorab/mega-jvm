package co.kenrg.mega.frontend.typechecking.types;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;

public class PrimitiveTypes {
    //TODO: Make these not lambdas, once more methods get added to the MegaType interface
    public static final MegaType INTEGER = () -> "Int";
    public static final MegaType FLOAT = () -> "Float";
    public static final MegaType BOOLEAN = () -> "Bool";
    public static final MegaType STRING = () -> "String";
    public static final MegaType UNIT = () -> "Unit";

    public static final Map<String, MegaType> ALL =
        Lists.newArrayList(
            INTEGER,
            FLOAT,
            BOOLEAN,
            STRING,
            UNIT
        ).stream().collect(toMap(MegaType::displayName, identity()));

    public static Optional<MegaType> byDisplayName(String name) {
        return Optional.ofNullable(ALL.get(name));
    }
}
