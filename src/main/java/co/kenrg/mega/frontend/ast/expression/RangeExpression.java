package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class RangeExpression extends Expression {
    public final Token token;
    public final Expression leftBound;
    public final Expression rightBound;

    public RangeExpression(Token token, Expression leftBound, Expression rightBound) {
        this.token = token;
        this.leftBound = leftBound;
        this.rightBound = rightBound;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format("%s..%s", this.leftBound.repr(debug, indentLevel), this.rightBound.repr(debug, indentLevel));
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
