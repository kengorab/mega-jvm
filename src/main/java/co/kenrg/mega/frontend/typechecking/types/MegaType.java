package co.kenrg.mega.frontend.typechecking.types;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.typeForMethod;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

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

    private List<Pair<String, MegaType>> propertiesCache = null;

    public List<Pair<String, MegaType>> getProperties() {
        if (this.propertiesCache != null) {
            return this.propertiesCache;
        }

        List<Pair<String, MegaType>> props = Lists.newArrayList();
        Class typeClass = this.typeClass();
        if (typeClass == null) {
            return Lists.newArrayList();
        }

        for (Method method : typeClass.getMethods()) {
            props.add(Pair.of(method.getName(), typeForMethod(method)));
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
