package co.kenrg.mega.frontend.ast.expression;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.ast.iface.Expression;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

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

    public MegaType getType() {
        return this.ident.getType();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

