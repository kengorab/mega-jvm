package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class IntegerLiteral extends Expression {
    public final Token token;
    public final int value;

    public IntegerLiteral(Token token, int value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return Integer.toString(this.value);
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
