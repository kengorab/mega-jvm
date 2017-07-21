package co.kenrg.mega.repl.evaluator;

import java.util.Map;

import co.kenrg.mega.repl.object.iface.Obj;
import com.google.common.collect.Maps;

public class Environment {
    private final Map<String, Obj> store = Maps.newHashMap();

    public Obj get(String name) {
        return store.get(name);
    }

    public void set(String name, Obj value) {
        store.put(name, value);
    }
}
