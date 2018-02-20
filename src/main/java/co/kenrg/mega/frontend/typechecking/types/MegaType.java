package co.kenrg.mega.frontend.typechecking.types;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.typeForMethod;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;
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

    private LinkedHashMultimap<String, MegaType> propertiesCache = LinkedHashMultimap.create();

    public LinkedHashMultimap<String, MegaType> getProperties() {
        // Preemptively loading all properties (especially on builtin types) is very costly, and largely unnecessary
        throw new NotImplementedException("Do not call getProperties for a builtin type; use getPropertiesByName");
    }

    public Set<MegaType> getPropertiesByName(String propName) {
        if (this.propertiesCache.containsKey(propName)) {
            return this.propertiesCache.get(propName);
        }

        Class typeClass = this.typeClass();
        if (typeClass == null) {
            return Sets.newHashSet();
        }

        LinkedHashMultimap<String, MegaType> props = LinkedHashMultimap.create();
        for (Method method : typeClass.getMethods()) {
            if (method.getName().equals(propName) && !Modifier.isStatic(method.getModifiers())) {
                props.put(method.getName(), typeForMethod(method));
            }
        }

        this.propertiesCache.putAll(props);

        return Sets.newHashSet(props.values());
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
