package co.kenrg.mega.repl.evaluator;

import java.util.Map;

import co.kenrg.mega.repl.object.iface.Obj;
import com.google.common.collect.Maps;

public class Environment {
    private Environment parent;
    private final Map<String, Obj> store = Maps.newHashMap();

    public Environment createChildEnvironment() {
        Environment child = new Environment();
        child.parent = this;
        return child;
    }

    public Obj get(String name) {
        if (store.containsKey(name)) {
            return store.get(name);
        }

        if (parent != null) {
            return parent.get(name);
        }

        return null;
    }

    public void set(String name, Obj value) {
        store.put(name, value);
    }
}
