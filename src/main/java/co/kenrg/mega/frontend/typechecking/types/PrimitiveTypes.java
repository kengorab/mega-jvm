package co.kenrg.mega.frontend.typechecking.types;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;

public class PrimitiveTypes {

    public static final MegaType INTEGER = new MegaType() {
        @Override
        public String displayName() {
            return "Int";
        }
    };

    public static final MegaType FLOAT = new MegaType() {
        @Override
        public String displayName() {
            return "Float";
        }
    };

    public static final MegaType BOOLEAN = new MegaType() {
        @Override
        public String displayName() {
            return "Bool";
        }
    };

    public static final MegaType STRING = new MegaType() {
        @Override
        public String displayName() {
            return "String";
        }
    };

    public static final MegaType UNIT = new MegaType() {
        @Override
        public String displayName() {
            return "Unit";
        }
    };

    public static final MegaType NOTHING = new MegaType() {
        @Override
        public String displayName() {
            return "Nothing";
        }
    };

    public static final Map<String, MegaType> ALL =
        Lists.newArrayList(INTEGER, FLOAT, BOOLEAN, STRING, UNIT, NOTHING).stream()
            .collect(toMap(MegaType::displayName, identity()));

    public static Optional<MegaType> byDisplayName(String name) {
        return Optional.ofNullable(ALL.get(name));
    }
}
