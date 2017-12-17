package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.joining;

import java.util.Map;

import co.kenrg.mega.frontend.token.Position;

public class StructTypeExpression extends TypeExpression {
    public final Map<String, TypeExpression> propTypes;

    public StructTypeExpression(Map<String, TypeExpression> propTypes, Position position) {
        super(position);
        this.propTypes = propTypes;
    }

    @Override
    public String signature() {
        return this.propTypes.entrySet().stream()
            .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue().signature()))
            .collect(joining(", ", "{ ", " }"));
    }
}
