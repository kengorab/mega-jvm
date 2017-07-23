package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class IndexExpression extends Expression {
    public final Token token;
    public final Expression target;
    public final Expression index;

    public IndexExpression(Token token, Expression target, Expression index) {
        this.token = token;
        this.target = target;
        this.index = index;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String repr = String.format(
            "%s[%s]",
            this.target.repr(debug, indentLevel),
            this.index.repr(debug, indentLevel)
        );
        return debug ? String.format("(%s)", repr) : repr;
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
