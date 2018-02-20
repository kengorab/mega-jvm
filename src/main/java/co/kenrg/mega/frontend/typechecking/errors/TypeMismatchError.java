package co.kenrg.mega.frontend.typechecking.errors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;

import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.ObjectType;

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
        if (expected instanceof ObjectType && actual instanceof ObjectType) {
            Map<String, MegaType> expectedProps = expected.getProperties().entries().stream()
                .collect(toMap(Entry::getKey, Entry::getValue));
            Map<String, MegaType> actualProps = actual.getProperties().entries().stream()
                .collect(toMap(Entry::getKey, Entry::getValue));

            String missingProps = expectedProps.entrySet().stream()
                .filter(entry -> !(actualProps.containsKey(entry.getKey()) && actualProps.get(entry.getKey()).isEquivalentTo(entry.getValue())))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(joining(", ", "{ ", " }"));

            return String.format("Expected %s, got %s; missing properties %s", this.expected, this.actual, missingProps);
        }

        return String.format("Expected %s, got %s", this.expected, this.actual);
    }
}
