package co.kenrg.mega.frontend.ast.expression;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class AssignmentExpression extends Expression {
    public final Token token;
    public final Identifier name;
    public final Expression right;

    public AssignmentExpression(Token token, Identifier name, Expression right) {
        this.token = token;
        this.name = name;
        this.right = right;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return String.format("%s = %s", this.name.value, this.right.repr(debug, indentLevel));
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
