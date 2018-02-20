package co.kenrg.mega.frontend.typechecking.types;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.typeForMethod;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import com.google.common.collect.LinkedHashMultimap;
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

    private LinkedHashMultimap<String, MegaType> propertiesCache = null;

    public LinkedHashMultimap<String, MegaType> getProperties() {
        if (this.propertiesCache != null) {
            return this.propertiesCache;
        }

        Class typeClass = this.typeClass();
        if (typeClass == null) {
            return LinkedHashMultimap.create();
        }

        LinkedHashMultimap<String, MegaType> props = LinkedHashMultimap.create();
        for (Method method : typeClass.getMethods()) {
            props.put(method.getName(), typeForMethod(method));
        }

        this.propertiesCache = props;

        return props;
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
