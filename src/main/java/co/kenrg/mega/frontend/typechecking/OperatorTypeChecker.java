package co.kenrg.mega.frontend.typechecking;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class OperatorTypeChecker {
    static class OperatorSignature {
        final MegaType lType;
        final MegaType rType;
        final MegaType returnType;

        OperatorSignature(MegaType lType, MegaType rType, MegaType returnType) {
            this.lType = lType;
            this.rType = rType;
            this.returnType = returnType;
        }
    }

    private static Map<String, List<OperatorSignature>> operators = ImmutableMap.<String, List<OperatorSignature>>builder()
        .put(">", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.NUMBER, PrimitiveTypes.NUMBER, PrimitiveTypes.BOOLEAN)
        ))
        .put(">=", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.NUMBER, PrimitiveTypes.NUMBER, PrimitiveTypes.BOOLEAN)
        ))
        .put("<", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.NUMBER, PrimitiveTypes.NUMBER, PrimitiveTypes.BOOLEAN)
        ))
        .put("<=", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.NUMBER, PrimitiveTypes.NUMBER, PrimitiveTypes.BOOLEAN)
        ))
        .put("==", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.ANY, PrimitiveTypes.ANY, PrimitiveTypes.BOOLEAN)
        ))
        .put("!=", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.ANY, PrimitiveTypes.ANY, PrimitiveTypes.BOOLEAN)
        ))
        .put("+", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.STRING, PrimitiveTypes.ANY, PrimitiveTypes.STRING),
            new OperatorSignature(PrimitiveTypes.ANY, PrimitiveTypes.STRING, PrimitiveTypes.STRING)
        ))
        .put("-", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT)
        ))
        .put("*", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.STRING, PrimitiveTypes.INTEGER, PrimitiveTypes.STRING),
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.STRING, PrimitiveTypes.STRING)
        ))
        .put("/", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER, PrimitiveTypes.INTEGER),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT, PrimitiveTypes.FLOAT),
            new OperatorSignature(PrimitiveTypes.FLOAT, PrimitiveTypes.INTEGER, PrimitiveTypes.FLOAT)
        ))
        .put("&&", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.BOOLEAN, PrimitiveTypes.BOOLEAN, PrimitiveTypes.BOOLEAN)
        ))
        .put("||", Lists.newArrayList(
            new OperatorSignature(PrimitiveTypes.BOOLEAN, PrimitiveTypes.BOOLEAN, PrimitiveTypes.BOOLEAN)
        ))
        .build();

    private static List<String> booleanOperators = Lists.newArrayList(
        ">", ">=", "<", "<=", "==", "!=", "&&", "||"
    );

    @Nullable
    public static List<OperatorSignature> signaturesForOperator(String operator) {
        return operators.get(operator);
    }

    public static boolean isBooleanOperator(String operator) {
        return booleanOperators.contains(operator);
    }
}
