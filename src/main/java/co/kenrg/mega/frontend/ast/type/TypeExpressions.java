package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.toList;

import co.kenrg.mega.frontend.typechecking.types.FunctionType;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ParametrizedMegaType;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import com.google.common.collect.LinkedHashMultimap;

public class TypeExpressions {
    // TODO: Add tests for this
    public static TypeExpression fromType(MegaType type) {
        if (type instanceof FunctionType) {
            return new FunctionTypeExpression(
                ((FunctionType) type).paramTypes.stream()
                    .map(TypeExpressions::fromType)
                    .collect(toList()),
                TypeExpressions.fromType(((FunctionType) type).returnType),
                null
            );
        } else if (type instanceof ParametrizedMegaType) {
            return new ParametrizedTypeExpression(
                type.displayName(),
                ((ParametrizedMegaType) type).typeArgs().stream()
                    .map(TypeExpressions::fromType)
                    .collect(toList()),
                null
            );
        } else if (type instanceof StructType) {
            LinkedHashMultimap<String, TypeExpression> propTypes = LinkedHashMultimap.create();
            type.getProperties().forEach((propName, propType) -> {
                propTypes.put(propName, TypeExpressions.fromType(propType));
            });
            return new StructTypeExpression(
//                ((StructType) type).getProperties().stream()
//                    .map(p -> Pair.of(p.getLeft(), TypeExpressions.fromType(p.getRight())))
//                    .collect(toList()),
                propTypes,
                null
            );
        } else {
            return new BasicTypeExpression(type.displayName(), null);
        }
    }
}
