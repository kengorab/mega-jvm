package co.kenrg.mega.frontend.typechecking;

import java.util.List;

import co.kenrg.mega.frontend.ast.iface.Node;
import co.kenrg.mega.frontend.typechecking.errors.TypeCheckerError;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class TypeCheckResult<T extends Node> {
    public final T node;
    public final MegaType type;
    public final List<TypeCheckerError> errors;
    public final TypeEnvironment typeEnvironment;

    public TypeCheckResult(T node, MegaType type, List<TypeCheckerError> errors, TypeEnvironment typeEnvironment) {
        this.node = node;
        this.type = type;
        this.errors = errors;
        this.typeEnvironment = typeEnvironment;
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }
}

