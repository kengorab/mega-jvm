package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class ParenthesizedExpression extends Expression {
    public final Token token;
    public final Expression expr;

    public ParenthesizedExpression(Token token, Expression expr) {
        this.token = token;
        this.expr = expr;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format("(%s)", this.expr.repr(debug, indentLevel));
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
