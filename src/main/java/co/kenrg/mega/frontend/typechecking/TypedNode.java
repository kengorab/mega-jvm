package co.kenrg.mega.frontend.typechecking;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.repl.object.iface.ObjectType;

public class TypedNode<T extends Node> {
    public final T node;
    public final ObjectType type;

    public TypedNode(T node, ObjectType type) {
        this.node = node;
        this.type = type;
    }
}
