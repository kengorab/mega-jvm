package co.kenrg.mega.frontend.ast.expression;

import java.util.Map;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.token.Token;

public class StringInterpolationExpression extends Expression {
    public final Token token;
    public final String value;
    public final Map<String, Expression> interpolatedExpressions;

    public StringInterpolationExpression(Token token, String value, Map<String, Expression> interpolatedExpressions) {
        this.token = token;
        this.value = value;
        this.interpolatedExpressions = interpolatedExpressions;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        return this.value;
    }

    @Override
    public Token getToken() {
        return null;
    }
}
