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
}
