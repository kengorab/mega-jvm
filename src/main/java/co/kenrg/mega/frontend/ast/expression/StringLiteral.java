package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class StringLiteral extends Expression {
    public final Token token;
    public final String value;

    public StringLiteral(Token token, String value) {
        this.token = token;
        this.value = value;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return this.value;
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
