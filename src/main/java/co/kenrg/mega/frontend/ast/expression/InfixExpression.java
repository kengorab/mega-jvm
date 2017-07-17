package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class InfixExpression extends Expression {
    public final Token token;   // The operator token, e.g. '+'
    public final String operator;
    public final Expression left;
    public final Expression right;

    public InfixExpression(Token token, String operator, Expression left, Expression right) {
        this.token = token;
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String str = String.format(
            "%s %s %s",
            this.left.repr(debug, indentLevel),
            this.operator,
            this.right.repr(debug, indentLevel)
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
