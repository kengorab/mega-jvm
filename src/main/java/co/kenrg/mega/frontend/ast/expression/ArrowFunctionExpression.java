package co.kenrg.mega.frontend.ast.expression;

import static java.util.stream.Collectors.joining;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class ArrowFunctionExpression extends Expression {
    public final Token token;
    public final List<Identifier> parameters;
    public final BlockExpression body;

    public ArrowFunctionExpression(Token token, List<Identifier> parameters, BlockExpression body) {
        this.token = token;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String params;
        if (this.parameters.size() == 1) {
            params = this.parameters.get(0).repr(debug, indentLevel);
        } else {
            String paramList = this.parameters.stream()
                .map(param -> param.repr(debug, indentLevel))
                .collect(joining(", "));
            params = String.format("(%s)", paramList);
        }

        return String.format("%s => %s", params, this.body.repr(debug, indentLevel));
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
