package co.kenrg.mega.frontend.typechecking.types;

import static java.util.stream.Collectors.joining;

import javax.annotation.Nullable;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class MegaType {
    abstract public String displayName();
    abstract public boolean isEquivalentTo(MegaType other);

    public @Nullable List<MegaType> typeArgs() {
        return null;
    }

    public String signature() {
        List<MegaType> typeArgs = typeArgs();
        if (typeArgs == null || typeArgs.isEmpty()) {
            return displayName();
        }

        String typeArgsStr = typeArgs.stream()
            .map(MegaType::signature)
            .collect(joining(", ", "<", ">"));
        return String.format("%s%s", displayName(), typeArgsStr);
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
