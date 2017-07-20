package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class CallExpression extends Expression {
    public final Token token;
    public final Expression target; // The invokee
    public final List<Expression> arguments;

    public CallExpression(Token token, Expression target, List<Expression> arguments) {
        this.token = token;
        this.target = target;
        this.arguments = arguments;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format(
            "%s(%s)",
            this.target.repr(debug, indentLevel),
            this.arguments.stream()
                .map(arg -> arg.repr(debug, indentLevel))
                .collect(joining(", "))
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
