package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class FloatLiteral extends Expression {
    public final Token token;
    public final float value;

    public FloatLiteral(Token token, float value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return Float.toString(this.value);
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
