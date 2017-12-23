package co.kenrg.mega.frontend.ast.iface;

import javax.annotation.Nullable;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class Statement implements Node {

    @Nullable
    @Override
    public MegaType getType() {
        return PrimitiveTypes.UNIT;
    }

    @Override
    public void setType(MegaType type) {
        // This has no effect, since a Statement's type is always Unit.
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
