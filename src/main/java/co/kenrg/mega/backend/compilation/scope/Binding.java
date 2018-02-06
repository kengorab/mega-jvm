package co.kenrg.mega.backend.compilation.scope;

import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class Binding {
    public final BindingTypes bindingType;
    public final String name;
    public final boolean isMutable;
    public final MegaType type;
    public final int index;

    Binding(BindingTypes bindingType, String name, boolean isMutable, MegaType type, int index) {
        this.bindingType = bindingType;
        this.name = name;
        this.isMutable = isMutable;
        this.type = type;
        this.index = index;
    }
}
