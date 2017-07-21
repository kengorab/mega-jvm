package co.kenrg.mega.repl.object.iface;

import co.kenrg.mega.repl.object.EvalError;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class Obj {
    public abstract ObjectType getType();
    public abstract String inspect();

    public boolean isError() {
        return this instanceof EvalError;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
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
