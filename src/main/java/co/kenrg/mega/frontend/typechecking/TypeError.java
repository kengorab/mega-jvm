package co.kenrg.mega.frontend.typechecking;

import co.kenrg.mega.repl.object.iface.ObjectType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TypeError {
    public final ObjectType expected;
    public final ObjectType actual;

    public TypeError(ObjectType expected, ObjectType actual) {
        this.expected = expected;
        this.actual = actual;
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
