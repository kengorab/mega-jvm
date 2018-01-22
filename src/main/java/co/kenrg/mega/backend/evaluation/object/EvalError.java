package co.kenrg.mega.backend.evaluation.object;

import static co.kenrg.mega.backend.evaluation.object.iface.ObjectType.EVAL_ERROR;

import co.kenrg.mega.backend.evaluation.object.iface.Obj;
import co.kenrg.mega.backend.evaluation.object.iface.ObjectType;

public class EvalError extends Obj {
    public final String message;

    public EvalError(String message) {
        this.message = message;
    }

    public static EvalError typeMismatchError(ObjectType expected, ObjectType actual) {
        return new EvalError(String.format("type mismatch: expected %s, got %s", expected, actual));
    }

    public static EvalError unknownPrefixOperatorError(String operator, Obj right) {
        return new EvalError(String.format("unknown operator: %s%s", operator, right.getType()));
    }

    public static EvalError unknownInfixOperatorError(String operator, Obj left, Obj right) {
        return new EvalError(String.format("unknown operator: %s %s %s", left.getType(), operator, right.getType()));
    }

    public static EvalError unknownIdentifierError(String identifier) {
        return new EvalError(String.format("unknown identifier: %s", identifier));
    }

    public static EvalError uninvokeableTypeError(Obj target) {
        return new EvalError(String.format("cannot invoke %s as a function: incompatible type %s", target.inspect(0), target.getType()));
    }

    public static EvalError functionArityError(int numExpected, int numReceived) {
        return new EvalError(String.format("incorrect number of arguments for function: expected %d, got %d", numExpected, numReceived));
    }

    public static EvalError unsupportedIndexTargetError(Obj target) {
        return new EvalError(String.format("cannot index into type: %s", target.getType()));
    }

    public static EvalError unsupportedIndexOperationError(Obj index) {
        return new EvalError(String.format("cannot use type as index: %s", index.getType()));
    }

    public static EvalError reassigningImmutableBindingError(String name) {
        return new EvalError(String.format("cannot reassign to immutable binding: %s", name));
    }

    public static EvalError duplicateBindingError(String name) {
        return new EvalError(String.format("duplicate binding: %s already defined in this context", name));
    }

    @Override
    public ObjectType getType() {
        return EVAL_ERROR;
    }

    @Override
    public String inspect(int indentLevel) {
        return "Error: " + this.message;
    }
}
