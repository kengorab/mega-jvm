package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.joining;

import java.util.Map.Entry;

import co.kenrg.mega.frontend.token.Position;
import com.google.common.collect.LinkedHashMultimap;

public class StructTypeExpression extends TypeExpression {
    public final LinkedHashMultimap<String, TypeExpression> propTypes;

    public StructTypeExpression(LinkedHashMultimap<String, TypeExpression> propTypes, Position position) {
        super(position);
        this.propTypes = propTypes;
    }

    @Override
    public String signature() {
        return this.propTypes.entries().stream()
            .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue().signature()))
            .collect(joining(", ", "{ ", " }"));
    }

    /**
     * Represents whether this type should be better represented as a declared type, rather than as an inline object type
     *
     * @return true if it contains a nested StructTypeExpression, its display length is >= 30, or it has more than 2 fields; false otherwise
     */
    public boolean isTooUnwieldy() {
        for (Entry<String, TypeExpression> prop : propTypes.entries()) {
            if (prop.getValue() instanceof StructTypeExpression) {
                return true;
            }
        }

        return this.signature().length() >= 30 || this.propTypes.size() > 2;
    }
}
