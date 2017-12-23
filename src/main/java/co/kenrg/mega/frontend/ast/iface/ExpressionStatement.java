package co.kenrg.mega.frontend.ast.iface;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.token.Token;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class ExpressionStatement extends Statement {
    public final Token token;
    public final Expression expression;

    public ExpressionStatement(Token token, Expression expr) {
        this.token = token;
        this.expression = expr;
    }

    @Nullable
    @Override
    public MegaType getType() {
        return this.expression.getType();
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
