package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class Identifier extends Expression {
    public final Token token;
    public final String value;

    public Identifier(Token token, String value) {
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
