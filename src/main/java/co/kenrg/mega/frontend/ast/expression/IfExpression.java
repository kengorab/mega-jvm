package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class IfExpression extends Expression {
    public final Token token;
    public final Expression condition;
    public final Expression thenExpr;
    public final Expression elseExpr;

    public IfExpression(Token token, Expression condition, Expression thenExpr, Expression elseExpr) {
        this.token = token;
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String str = String.format(
            "if %s %s",
            this.condition.repr(debug, indentLevel),
            this.thenExpr.repr(debug, indentLevel)
        );

        if (elseExpr != null) {
            return String.format(
                "%s else %s",
                str,
                this.elseExpr.repr(debug, indentLevel)
            );
        } else {
            return str;
        }
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
