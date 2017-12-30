package co.kenrg.mega.backend.compilation;

import javax.annotation.Nullable;
import java.util.Map;

import co.kenrg.mega.frontend.ast.iface.Expression;
import com.google.common.collect.Maps;

public class Scope {
    public enum BindingTypes {
        STATIC,
        LOCAL
    }

    public class Binding {
        public final BindingTypes bindingType;
        public final String name;
        public final boolean isMutable;
        public final Expression expr;
        public final int index;

        private Binding(BindingTypes bindingType, String name, boolean isMutable, Expression expr, int index) {
            this.bindingType = bindingType;
            this.name = name;
            this.isMutable = isMutable;
            this.expr = expr;
            this.index = index;
        }
    }

    public final Scope parent;
    public final FocusedMethod focusedMethod;
    public final Map<String, Binding> bindings;

    private int nextLocalVarIndex = 0;

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

    public void addBinding(String name, Expression expr, BindingTypes bindingType, boolean isMutable) {
        this.bindings.put(name, new Binding(bindingType, name, isMutable, expr, this.nextLocalVarIndex));
        if (bindingType == BindingTypes.LOCAL) {
            this.nextLocalVarIndex++;
        }
    }

    public int nextLocalVariableIndex() {
        return this.nextLocalVarIndex;
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
