package co.kenrg.mega.frontend.typechecking.errors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;
import co.kenrg.mega.frontend.typechecking.types.StructType;
import org.apache.commons.lang3.tuple.Pair;

public class TypeMismatchError extends TypeCheckerError {
    public final MegaType expected;
    public final MegaType actual;

    public TypeMismatchError(MegaType expected, MegaType actual, Position position) {
        super(position);
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String message() {
        if (expected instanceof StructType && actual instanceof ObjectType) {
            Map<String, MegaType> expectedProps = ((StructType) expected).getProperties().stream()
                .collect(toMap(Pair::getKey, Pair::getValue));
            Map<String, MegaType> actualProps = ((ObjectType) actual).properties.stream()
                .collect(toMap(Pair::getKey, Pair::getValue));

            String missingProps = expectedProps.entrySet().stream()
                .filter(entry -> !(actualProps.containsKey(entry.getKey()) && actualProps.get(entry.getKey()).isEquivalentTo(entry.getValue())))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(joining(", ", "{ ", " }"));

            return String.format("Expected %s, got %s; missing properties %s", this.expected, this.actual, missingProps);
        }

        return String.format("Expected %s, got %s", this.expected, this.actual);
    }
}
