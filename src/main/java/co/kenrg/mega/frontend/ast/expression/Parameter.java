package co.kenrg.mega.frontend.ast.expression;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.ast.iface.Expression;

public class Parameter {
    public final Identifier ident;
    @Nullable public final Expression defaultValue;

    public Parameter(Identifier ident, @Nullable Expression defaultValue) {
        this.ident = ident;
        this.defaultValue = defaultValue;
    }

    public Parameter(Identifier ident) {
        this(ident, null);
    }

    public boolean hasDefaultValue() {
        return this.defaultValue != null;
    }

    public String repr(boolean debug, int indentLevel) {
        if (this.hasDefaultValue()) {
            assert this.defaultValue != null;
            return this.ident.repr(debug, indentLevel) + " = " + this.defaultValue.repr(debug, indentLevel);
        } else {
            return this.ident.repr(debug, indentLevel);
        }
    }
}
