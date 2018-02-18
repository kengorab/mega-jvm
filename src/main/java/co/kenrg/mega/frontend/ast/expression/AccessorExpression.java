package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class AccessorExpression extends Expression {
    public final Token token;
    public final Expression target;
    public final Identifier property;

    public AccessorExpression(Token token, Expression target, Identifier property) {
        this.token = token;
        this.target = target;
        this.property = property;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format(
            "%s.%s",
            this.target.repr(debug, indentLevel),
            this.property.repr(debug, indentLevel)
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
