package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class BooleanLiteral extends Expression {
    public final Token token;
    public final boolean value;

    public BooleanLiteral(Token token, boolean value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return Boolean.toString(this.value);
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
