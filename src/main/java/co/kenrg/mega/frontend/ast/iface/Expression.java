package co.kenrg.mega.frontend.ast.iface;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class Expression implements Node {
    private MegaType type;

    @Nullable
    @Override
    public MegaType getType() {
        return this.type;
    }

    @Override
    public void setType(MegaType type) {
        this.type = type;
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
