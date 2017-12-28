package co.kenrg.mega.frontend.typechecking.types;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import com.google.common.collect.Lists;

public class PrimitiveTypes {

    public static final MegaType INTEGER = new MegaType() {
        @Override
        public String displayName() {
            return "Int";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(INTEGER);
        }

        @Override
        public String className() {
            return "java/lang/Integer";
        }
    };

    public static final MegaType FLOAT = new MegaType() {
        @Override
        public String displayName() {
            return "Float";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(FLOAT);
        }

        @Override
        public String className() {
            return "java/lang/Float";
        }
    };

    public static final MegaType BOOLEAN = new MegaType() {
        @Override
        public String displayName() {
            return "Bool";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(BOOLEAN);
        }

        @Override
        public String className() {
            return "java/lang/Boolean";
        }
    };

    public static final MegaType STRING = new MegaType() {
        @Override
        public String displayName() {
            return "String";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(STRING);
        }

        @Override
        public String className() {
            return "java/lang/String";
        }
    };

    public static final MegaType UNIT = new MegaType() {
        @Override
        public String displayName() {
            return "Unit";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(UNIT);
        }
    };

    public static final MegaType NOTHING = new MegaType() {
        @Override
        public String displayName() {
            return "Nothing";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return other.equals(NOTHING);
        }
    };

    public static final MegaType ANY = new MegaType() {
        @Override
        public String displayName() {
            return "Any";
        }

        @Override
        public boolean isEquivalentTo(MegaType other) {
            return true;
        }

        @Override
        public String className() {
            return "java/lang/Object";
        }
    };

    public static final MegaType NUMBER = new UnionType("Number", INTEGER, FLOAT);

    public static final Map<String, MegaType> ALL =
        Lists.newArrayList(INTEGER, FLOAT, BOOLEAN, STRING, UNIT, NOTHING, ANY).stream()
            .collect(toMap(MegaType::displayName, identity()));
}
