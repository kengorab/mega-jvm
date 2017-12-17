package co.kenrg.mega.frontend.ast.type;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.token.Position;

public class FunctionTypeExpression extends TypeExpression {
    public final List<TypeExpression> paramTypes;
    public final TypeExpression returnType;

    public FunctionTypeExpression(List<TypeExpression> paramTypes, TypeExpression returnType, Position position) {
        super(position);
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    @Override
    public String signature() {
        return String.format(
            "%s => %s",
            this.paramTypes.stream().map(TypeExpression::signature).collect(joining(", ", "(", ")")),
            this.returnType.signature()
        );
    }
}
