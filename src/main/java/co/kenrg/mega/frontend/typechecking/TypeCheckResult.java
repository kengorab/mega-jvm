package co.kenrg.mega.frontend.typechecking;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;

public class TypeCheckResult<T extends Node> {
    public final TypedNode<T> node;
    public final List<TypeCheckerError> errors;

    public TypeCheckResult(TypedNode<T> node, List<TypeCheckerError> errors) {
        this.node = node;
        this.errors = errors;
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }
}

