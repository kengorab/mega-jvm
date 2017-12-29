package co.kenrg.mega.backend.compilation;

import javax.annotation.Nullable;
import java.util.Map;

import co.kenrg.mega.frontend.ast.iface.Expression;
import com.google.common.collect.Maps;

public class Scope {
    public class Binding {
        public final boolean isStatic;
        public final String name;
        public final boolean isMutable;

        public Binding(boolean isStatic, String name, boolean isMutable) {
            this.isStatic = isStatic;
            this.name = name;
            this.isMutable = isMutable;
        }
    }

    public final Scope parent;
    public final FocusedMethod focusedMethod;
    public final Map<String, Binding> bindings;

    public Scope(FocusedMethod focusedMethod) {
        this.parent = null;
        this.focusedMethod = focusedMethod;
        this.bindings = Maps.newHashMap();
    }

    private Scope(Scope parent, FocusedMethod focusedMethod) {
        this.parent = parent;
        this.focusedMethod = focusedMethod;
        this.bindings = Maps.newHashMap();
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public Scope createChild(FocusedMethod focusedMethod) {
        return new Scope(this, focusedMethod);
    }

    public Scope createChild() {
        return new Scope(this, this.focusedMethod);
    }

    public void addBinding(String name, Expression expr, boolean isStatic, boolean isMutable) {
        this.bindings.put(name, new Binding(isStatic, name, isMutable));
    }

    @Nullable
    public Binding getBinding(String name) {
        if (!this.bindings.containsKey(name)) {
            if (this.parent != null) {
                return this.parent.getBinding(name);
            }
            return null;
        }
        return this.bindings.get(name);
    }
}
