package co.kenrg.mega.frontend.typechecking.types;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class MegaType {
    abstract public String displayName();

    abstract public boolean isEquivalentTo(MegaType other);

    public String signature() {
        return displayName();
    }

    public boolean isParametrized() {
        return false;
    }

    @Nullable
    public String className() {
        return null;
    }

    @Nullable
    public Class typeClass() {
        return null;
    }

    @Override
    public String toString() {
        return signature();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}
