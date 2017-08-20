package co.kenrg.mega.frontend.ast.expression;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.ast.type.TypeExpression;
import co.kenrg.mega.frontend.token.Token;

public class Identifier extends Expression {
    public final Token token;
    public final String value;
    public final @Nullable TypeExpression typeAnnotation;

    public Identifier(Token token, String value) {
        this.token = token;
        this.value = value;
        this.typeAnnotation = null;
    }

    public Identifier(Token token, String value, @Nullable TypeExpression typeAnnotation) {
        this.token = token;
        this.value = value;
        this.typeAnnotation = typeAnnotation;
    }

    @Override
    public String repr(boolean debug, int indentLevel) {
        if (this.typeAnnotation == null) {
            return this.value;
        }
        return String.format("%s: %s", this.value, this.typeAnnotation.signature());
    }

    @Override
    public Token getToken() {
        return this.token;
    }
}
