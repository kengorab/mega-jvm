package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class PrefixExpression extends Expression {
    public final Token token;
    public final String operator;
    public final Expression expression;

    public PrefixExpression(Token token, String operator, Expression expression) {
        this.token = token;
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String str = String.format(
            "%s%s",
            this.operator,
            this.expression.repr(debug, indentLevel)
        );

        if (debug) {
            return "(" + str + ")";
        } else {
            return str;
        }
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
