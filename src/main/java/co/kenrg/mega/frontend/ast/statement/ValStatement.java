package co.kenrg.mega.frontend.ast.statement;

import co.kenrg.mega.frontend.ast.expression.Identifier;
import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.iface.Statement;
import co.kenrg.mega.frontend.token.Token;

public class ValStatement extends Statement {
    public final Token token;
    public final Identifier name;
    public final Expression value;

    public ValStatement(Token token, Identifier name, Expression value) {
        this.token = token;
        this.name = name;
        this.value = value;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        String valueStr = (this.value == null)
                ? "nil"
                : this.value.repr(debug, indentLevel);

        return String.format(
                "%s %s = %s",
                this.token.literal,
                this.name.repr(debug, indentLevel),
                valueStr
        );
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
