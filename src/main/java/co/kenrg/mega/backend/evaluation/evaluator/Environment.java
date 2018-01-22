package co.kenrg.mega.backend.evaluation.evaluator;

import java.util.Map;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import com.google.common.collect.Maps;

public class Environment {
    public enum SetBindingStatus {
        NO_ERROR,
        E_IMMUTABLE,
        E_DUPLICATE,
        E_NOBINDING
    }

    private class Binding {
        public final Obj object;
        public final boolean isImmutable;

        public Binding(Obj object, boolean isImmutable) {
            this.object = object;
            this.isImmutable = isImmutable;
        }
    }

    private Environment parent;
    private final Map<String, Binding> store = Maps.newHashMap();

    public Environment createChildEnvironment() {
        Environment child = new Environment();
        child.parent = this;
        return child;
    }

    public Obj get(String name) {
        if (store.containsKey(name)) {
            return store.get(name).object;
        }

        if (parent != null) {
            return parent.get(name);
        }

        return null;
    }

    public SetBindingStatus add(String name, Obj value, boolean isImmutable) {
        if (store.containsKey(name)) {
            return SetBindingStatus.E_DUPLICATE;
        }

        store.put(name, new Binding(value, isImmutable));
        return SetBindingStatus.NO_ERROR;
    }

    public SetBindingStatus set(String name, Obj value) {
        if (store.containsKey(name) && store.get(name).isImmutable) {
            return SetBindingStatus.E_IMMUTABLE;
        } else if (!store.containsKey(name)) {
            if (this.parent != null) {
                return this.parent.set(name, value);
            } else {
                return SetBindingStatus.E_NOBINDING;
            }
        }

        store.put(name, new Binding(value, false));
        return SetBindingStatus.NO_ERROR;
    }
}