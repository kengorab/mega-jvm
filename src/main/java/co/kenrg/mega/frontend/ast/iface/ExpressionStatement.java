package co.kenrg.mega.frontend.ast.iface;

import co.kenrg.mega.frontend.token.Token;

public class ExpressionStatement extends Statement {
    public final Token token;
    public final Expression expression;

    public ExpressionStatement(Token token, Expression expr) {
        this.token = token;
        this.expression = expr;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return this.expression.repr(debug, indentLevel);
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
