package co.kenrg.mega.frontend.ast.type;

import co.kenrg.mega.frontend.token.Position;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class TypeExpression {
    public final Position position;

    public TypeExpression(Position position) {
        this.position = position;
    }

    public abstract String signature();

    @Override
    public String toString() {
        return signature() + String.format("<%d,%d>", position.line, position.col);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
