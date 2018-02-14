package co.kenrg.mega.backend.compilation.scope;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;

public class Scope {
    public final Scope parent;
    public final FocusedMethod focusedMethod;
    public final Map<String, Binding> bindings;
    public Context context; // Not final; can be set on a compiler's scope from a sub-compiler

    private int nextLocalVarIndex = 0;

    public Scope(FocusedMethod focusedMethod) {
        this.parent = null;
        this.focusedMethod = focusedMethod;
        this.bindings = Maps.newHashMap();
        this.context = new Context();
    }

    private Scope(Scope parent, FocusedMethod focusedMethod, Context context) {
        this.parent = parent;
        this.nextLocalVarIndex = this.parent.nextLocalVarIndex;
        this.focusedMethod = focusedMethod;
        this.bindings = Maps.newHashMap();
        this.context = context;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public Scope createChild() {
        return new Scope(this, this.focusedMethod, this.context);
    }

    public Scope createChild(FocusedMethod focusedMethod) {
        return new Scope(this, focusedMethod, this.context);
    }

    public void addBinding(String name, MegaType type, String ownerModule, BindingTypes bindingType, boolean isMutable, boolean isExported) {
        this.bindings.put(name, new Binding(bindingType, name, isMutable, type, this.nextLocalVarIndex, isExported, ownerModule));
        if (bindingType == BindingTypes.LOCAL) {
            this.nextLocalVarIndex++;
        }
    }

    public void addBinding(String name, MegaType type, String ownerModule, BindingTypes bindingType, boolean isMutable) {
        this.addBinding(name, type, ownerModule, bindingType, isMutable, false);
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

    /**
     * Used in conjunction with <code>MethodVisitor#visitFrame</code>
     *
     * @return the signatures of all current local variables before creating a new frame.
     */
    public Object[] getLocalsSignatures() {
        List<Binding> bindings = Lists.newArrayList(this.bindings.values());
        bindings.sort((b1, b2) -> b2.index - b1.index);

        Object[] signatures = new Object[bindings.size()];
        for (int i = 0; i < bindings.size(); i++) {
            Binding binding = bindings.get(i);
            MegaType type = binding.type;
            if (isPrimitive(type)) {
                if (type == PrimitiveTypes.INTEGER || type == PrimitiveTypes.BOOLEAN) {
                    signatures[i] = Opcodes.INTEGER;
                } else if (type == PrimitiveTypes.FLOAT) {
                    signatures[i] = Opcodes.FLOAT;
                }
            } else {
                signatures[i] = jvmDescriptor(type, false);
            }
        }
        return signatures;
    }
}
