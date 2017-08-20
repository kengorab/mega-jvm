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
    private final Map<String, Binding> store = Maps.newHashMap();

    public TypeEnvironment createChildEnvironment() {
        TypeEnvironment child = new TypeEnvironment();
        child.parent = this;
        return child;
    }

    @Nullable
    public MegaType get(String name) {
        if (store.containsKey(name)) {
            return store.get(name).type;
        }

        if (parent != null) {
            return parent.get(name);
        }

        return null;
    }

    public SetBindingStatus add(String name, MegaType type, boolean isImmutable) {
        if (store.containsKey(name)) {
            return SetBindingStatus.E_DUPLICATE;
        }

        store.put(name, new Binding(type, isImmutable));
        return SetBindingStatus.NO_ERROR;
    }

    public SetBindingStatus set(String name, MegaType type) {
        if (store.containsKey(name) && store.get(name).isImmutable) {
            return SetBindingStatus.E_IMMUTABLE;
        } else if (!store.containsKey(name)) {
            if (this.parent != null) {
                return this.parent.set(name, type);
            } else {
                return SetBindingStatus.E_NOBINDING;
            }
        }

        store.put(name, new Binding(type, false));
        return SetBindingStatus.NO_ERROR;
    }
}
