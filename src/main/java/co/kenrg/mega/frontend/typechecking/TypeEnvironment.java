package co.kenrg.mega.frontend.typechecking;

import javax.annotation.Nullable;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import com.google.common.collect.Maps;

public class TypeEnvironment {
    public enum SetBindingStatus {
        NO_ERROR,
        E_IMMUTABLE,
        E_DUPLICATE,
        E_NOBINDING
    }

    private class Binding {
        public final MegaType type;
        public final boolean isImmutable;

        public Binding(MegaType type, boolean isImmutable) {
            this.type = type;
            this.isImmutable = isImmutable;
        }
    }

    private TypeEnvironment parent;
    private final Map<String, Binding> bindingTypesStore = Maps.newHashMap();

    public TypeEnvironment createChildEnvironment() {
        TypeEnvironment child = new TypeEnvironment();
        child.parent = this;
        return child;
    }

    @Nullable
    public MegaType getTypeForBinding(String name) {
        if (bindingTypesStore.containsKey(name)) {
            return bindingTypesStore.get(name).type;
        }

        if (parent != null) {
            return parent.getTypeForBinding(name);
        }

        return null;
    }

    public SetBindingStatus addBindingWithType(String name, MegaType type, boolean isImmutable) {
        if (bindingTypesStore.containsKey(name)) {
            return SetBindingStatus.E_DUPLICATE;
        }

        bindingTypesStore.put(name, new Binding(type, isImmutable));
        return SetBindingStatus.NO_ERROR;
    }

    public SetBindingStatus setTypeForBinding(String name, MegaType type) {
        if (bindingTypesStore.containsKey(name) && bindingTypesStore.get(name).isImmutable) {
            return SetBindingStatus.E_IMMUTABLE;
        } else if (!bindingTypesStore.containsKey(name)) {
            if (this.parent != null) {
                return this.parent.setTypeForBinding(name, type);
            } else {
                return SetBindingStatus.E_NOBINDING;
            }
        }

        bindingTypesStore.put(name, new Binding(type, false));
        return SetBindingStatus.NO_ERROR;
    }
}
