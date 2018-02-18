package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.token.Position;
import org.apache.commons.lang3.tuple.Pair;

public class StructTypeExpression extends TypeExpression {
    public final List<Pair<String, TypeExpression>> propTypes;

    public StructTypeExpression(List<Pair<String, TypeExpression>> propTypes, Position position) {
        super(position);
        this.propTypes = propTypes;
    }

    @Override
    public String signature() {
        return this.propTypes.stream()
            .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue().signature()))
            .collect(joining(", ", "{ ", " }"));
    }

    /**
     * Represents whether this type should be better represented as a declared type, rather than as an inline object type
     *
     * @return true if it contains a nested StructTypeExpression, its display length is >= 30, or it has more than 2 fields; false otherwise
     */
    public boolean isTooUnwieldy() {
        for (Pair<String, TypeExpression> prop : propTypes) {
            if (prop.getRight() instanceof StructTypeExpression) {
                return true;
            }
        }

        return this.signature().length() >= 30 || this.propTypes.size() > 2;
    }
}
