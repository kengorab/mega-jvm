package co.kenrg.mega.frontend.typechecking;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class TypedNode<T extends Node> {
    public final T node;
    public final MegaType type;

    public TypedNode(T node, MegaType type) {
        this.node = node;
        this.type = type;
    }
}
