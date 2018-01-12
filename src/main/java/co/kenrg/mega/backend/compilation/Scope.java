package co.kenrg.mega.backend.compilation;

import static co.kenrg.mega.backend.compilation.TypesAndSignatures.isPrimitive;
import static co.kenrg.mega.backend.compilation.TypesAndSignatures.jvmDescriptor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.frontend.typechecking.types.PrimitiveTypes;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;

public class Scope {
    public enum BindingTypes {
        STATIC,
        LOCAL,
        METHOD
    }

    public class Binding {
        public final BindingTypes bindingType;
        public final String name;
        public final boolean isMutable;
        public final MegaType type;
        public final int index;

        private Binding(BindingTypes bindingType, String name, boolean isMutable, MegaType type, int index) {
            this.bindingType = bindingType;
            this.name = name;
            this.isMutable = isMutable;
            this.type = type;
            this.index = index;
        }
    }

    public static class Context implements Cloneable {
        public List<Pair<String, Integer>> subcontexts = Lists.newArrayList();

        public Context() {
        }

        // TODO: Remove this and have child scopes append/pop to/from their parent context (it's not like concurrency matters)
        @Override
        public Context clone() {
            try {
                super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            Context c = new Context();
            c.subcontexts = Lists.newArrayList(this.subcontexts);
            return c;
        }

        public void pushContext(String name) {
            this.subcontexts.add(Pair.of(name, 0));
        }

        public void incLambdaCount() {
            int index = this.subcontexts.size() - 1;
            Pair<String, Integer> topOfStack = this.subcontexts.get(index);
            this.subcontexts.add(index, Pair.of(topOfStack.getLeft(), topOfStack.getRight() + 1));
            this.subcontexts.remove(index + 1);
        }

        public void popContext() {
            this.subcontexts.remove(this.subcontexts.size() - 1);
        }
    }

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
        return new Scope(this, focusedMethod, this.context.clone());
    }

    public void addBinding(String name, MegaType type, BindingTypes bindingType, boolean isMutable) {
        this.bindings.put(name, new Binding(bindingType, name, isMutable, type, this.nextLocalVarIndex));
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
