package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.joining;

import java.util.List;

public class ParametrizedTypeExpression extends TypeExpression {
    public final String type;
    public final List<TypeExpression> typeArgs;

    public ParametrizedTypeExpression(String type, List<TypeExpression> typeArgs) {
        this.type = type;
        this.typeArgs = typeArgs;
    }

    @Override
    public String signature() {
        return String.format(
            "%s%s",
            this.type,
            this.typeArgs.stream().map(TypeExpression::signature).collect(joining(", ", "[", "]"))
        );
    }
}
